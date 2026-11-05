import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One race: the cars, the clock, and the rules that move everything forward one step at a time.
 * The race owns a single seeded random number generator, so the same seed always gives the
 * same race. It knows nothing about graphics; the screen reads its state and draws it.
 * Each step every car aims for the speed profile's target, slows if it is too close to the car
 * in front, and then moves. A car is never allowed to move closer than Tuning.MIN_SEPARATION to
 * the car ahead, so cars cannot drive through each other.
 */
public final class Race {

    /** The race settings. */
    private final RaceConfig config;

    /** The circuit. */
    private final Track track;

    /** Target speeds around the lap for every grip level. */
    private final ProfileSet profiles;

    /** Current weather on track. */
    private final TrackConditions conditions;

    /** The one random number generator for this race. */
    private final Random random;

    /** All cars, in grid order. Index equals Car.getId(). */
    private final ArrayList<Car> cars = new ArrayList<Car>();

    /** Cars in race order, re-sorted after every step. */
    private final ArrayList<Car> standings = new ArrayList<Car>();

    /** Corners with braking zones, where battles happen. */
    private final List<Corner> corners;

    /** Runs the battles. */
    private final BattleManager battles;

    /** Runs the pit stops. */
    private final PitLane pitLane;

    /** True once the track has been wet enough for slicks to struggle; the two-compound rule is then lifted. */
    private boolean wetRace;

    /** Messages for the radio feed, oldest first. */
    private final ArrayList<RaceMessage> messages = new ArrayList<RaceMessage>();

    /** Simulated qualifying results, fastest first; this order is the grid. */
    private final List<QualifyingEntry> qualifying;

    /** The car the player follows and engineers. */
    private final Car playerCar;

    /** Lap time table: row = car id, column = lap number minus 1, value in seconds (0 if not yet driven). */
    private final double[][] lapTimes;

    /** Tire wear table: row = car id, column = lap number minus 1, value = wear when that lap ended. */
    private final double[][] wearHistory;

    /**
     * Timing loop table: row = car id, column = loop number, value = race time the car crossed it
     * (0 if the car started past it). Loops sit every Tuning.TIMING_LOOP_SPACING meters from loopOrigin. The gap between two cars is the
     * difference in time at the latest loop both have crossed, just like real timing.
     */
    private final double[][] loopTimes;

    /**
     * Distance of timing loop 0, in meters. It sits behind the last grid slot so every car,
     * even one starting behind the line, is timed from the first second of the race.
     */
    private final double loopOrigin;

    /** Race time in seconds. */
    private double time;

    /** Number of cars that have taken the chequered flag. */
    private int finishedCount;

    /**
     * Sets up the race: finds the corners, runs qualifying, and places the cars on the grid.
     * Computer drivers get a random strategy; the player starts on the one-stop-late plan until
     * setPlayerPlan is called from the setup screen.
     *
     * @param config race settings
     * @param track the circuit
     * @param roster every available driver; must hold at least config.getCarCount() drivers
     * @throws IllegalArgumentException if the roster is too small
     */
    public Race(RaceConfig config, Track track, List<Driver> roster) {
        if (roster.size() < config.getCarCount()) {
            throw new IllegalArgumentException("Roster has " + roster.size() + " drivers but "
                    + config.getCarCount() + " cars were requested");
        }
        this.config = config;
        this.track = track;
        this.random = new Random(config.getSeed());
        this.profiles = new ProfileSet(track, LapCalibrator.gripScaleFor(track));
        this.conditions = TrackConditions.neutral();
        this.corners = Corner.findAll(track, profiles.forCarGrip(Compound.MEDIUM.getDryGrip()));
        this.battles = new BattleManager(this, random);
        this.pitLane = new PitLane(this, random);

        ArrayList<Driver> field = new ArrayList<Driver>(roster);
        Collections.shuffle(field, random);
        Driver playerDriver = field.get(random.nextInt(config.getCarCount()));
        qualifying = runQualifying(field.subList(0, config.getCarCount()), track);
        Car player = null;
        for (int i = 0; i < qualifying.size(); i++) {
            QualifyingEntry entry = qualifying.get(i);
            double gridDistance = -(Tuning.GRID_FIRST_SLOT_OFFSET + i * Tuning.GRID_SLOT_SPACING);
            double gridLateral = (i % 2 == 0 ? 1 : -1) * Tuning.GRID_LATERAL_OFFSET;
            boolean isPlayer = entry.getDriver() == playerDriver;
            PitPlan plan = isPlayer ? StrategyPreset.ONE_STOP_LATE.makePlan() : PitStrategyPlanner.choosePlan(random);
            Car car = new Car(i, entry.getDriver(), i + 1, entry.getPace(), gridDistance, gridLateral,
                    plan.getStartCompound(), isPlayer);
            car.setPlan(plan);
            double spread = Tuning.AI_AGGRESSION_MAX - Tuning.AI_AGGRESSION_MIN;
            car.setAggression(Tuning.AI_AGGRESSION_MIN + random.nextDouble() * spread);
            cars.add(car);
            standings.add(car);
            if (isPlayer) {
                player = car;
            }
        }
        playerCar = player;

        lapTimes = new double[cars.size()][config.getLaps()];
        wearHistory = new double[cars.size()][config.getLaps()];
        loopOrigin = -(Tuning.GRID_FIRST_SLOT_OFFSET + Tuning.MAX_CARS * Tuning.GRID_SLOT_SPACING);
        double timedDistance = (config.getLaps() + 1) * track.getLapLength() - loopOrigin;
        loopTimes = new double[cars.size()][(int) Math.ceil(timedDistance / Tuning.TIMING_LOOP_SPACING) + 1];
    }

    /**
     * Simulated qualifying: draws each car's pace for this race, gives it a qualifying lap
     * (reference lap divided by pace, plus random noise for how the lap went), then sorts the
     * entries fastest first. The sorted list is the starting grid.
     *
     * @param drivers the drivers taking part
     * @param track the track, for its reference lap time
     * @return entries sorted fastest first
     */
    private List<QualifyingEntry> runQualifying(List<Driver> drivers, Track track) {
        List<QualifyingEntry> entries = new ArrayList<QualifyingEntry>();
        double referenceLap = track.getProfile().getMedianRaceLapSeconds();
        for (Driver driver : drivers) {
            double pace = 1.0 + (random.nextDouble() * 2 - 1) * Tuning.PACE_NOISE;
            double lap = referenceLap / pace + random.nextGaussian() * Tuning.QUALIFYING_NOISE_SECONDS;
            entries.add(new QualifyingEntry(driver, pace, lap));
        }
        Collections.sort(entries, (a, b) -> Double.compare(a.getLapTime(), b.getLapTime()));
        return entries;
    }

    /**
     * Advances the race by one fixed time step (Tuning.SIM_STEP_SECONDS).
     * All cars decide their move from the positions at the start of the step, then all move,
     * so the order in which cars are processed never changes the result.
     */
    public void step() {
        double dt = Tuning.SIM_STEP_SECONDS;
        int n = cars.size();
        for (Car car : cars) {
            if (!car.isPlayer()) {
                car.setPushLevel(AiDriver.choosePush(car));
            }
        }
        battles.beforeMove();
        pitLane.beforeMove();
        if (conditions.wetnessAt(0) > Tuning.SLICK_WET_CLIFF) {
            wetRace = true;
        }
        double[] moves = new double[n];
        for (int i = 0; i < n; i++) {
            moves[i] = planMove(cars.get(i), dt);
        }
        for (int i = 0; i < n; i++) {
            Car car = cars.get(i);
            double before = car.getDistance();
            car.advance(moves[i], dt);
            pitLane.afterMove(car, before, before + moves[i], dt);
            car.updateSystems(dt, moves[i], track, profiles, conditions, wantsDeploy(car));
            recordCrossings(car, before, moves[i], dt);
        }
        time += dt;
        Collections.sort(standings, Race::compareStandings);
        battles.afterMove();
    }

    /**
     * Decides how far one car moves this step.
     * The car aims for the profile speed, gets a tow in the slipstream, speeds up (attacker) or
     * eases off (defender) during a pass, eases off to follow a close car ahead, and is finally
     * clamped so it can never get nearer than the minimum separation to the car blocking it.
     *
     * @param car the car
     * @param dt time step in seconds
     * @return meters to move, never negative
     */
    private double planMove(Car car, double dt) {
        double target = Math.min(car.targetSpeed(profiles, conditions), pitLane.speedLimit(car));
        Car partner = car.getPassPartner();
        if (partner != null && car.isPassAttacker()) {
            target = Math.max(target * (1 + Tuning.PASS_SPEED_BONUS), partner.getSpeed() + Tuning.PASS_MIN_CLOSING);
        } else if (partner != null) {
            target *= 1 - Tuning.PASS_DEFENDER_SLOW;
        }
        Car ahead = blockingCarAhead(car);
        double gap = ahead == null ? Double.MAX_VALUE : trackGap(car, ahead);
        double tow = slipstreamFactor(gap, car.getSpeed(), onStraight(car));
        if (tow > 1.0) {
            target *= tow;
            car.applyTow(tow);
        }
        if (ahead != null) {
            double comfortable = Tuning.MIN_SEPARATION + car.getSpeed() * Tuning.FOLLOW_TIME_GAP;
            if (gap < comfortable) {
                target = Math.min(target, ahead.getSpeed());
            }
        }
        car.approachSpeed(target, profiles.getGripScale(), dt);
        double move = car.getSpeed() * dt;
        double room = gap - Tuning.MIN_SEPARATION;
        if (move > room) {
            move = Math.max(0, room);
            car.capSpeed(ahead.getSpeed());
        }
        return move;
    }

    /**
     * Speed bonus from the slipstream: a car close behind another on a straight punches a smaller
     * hole in the air, so it can go a little faster. The bonus is largest right behind the car
     * ahead and fades to nothing at Tuning.SLIPSTREAM_GAP_SECONDS.
     *
     * @param gapMeters distance to the car ahead in meters
     * @param speed own speed in m/s
     * @param onStraight whether the car is on a straight
     * @return speed multiplier, 1 when there is no slipstream
     */
    public static double slipstreamFactor(double gapMeters, double speed, boolean onStraight) {
        if (!onStraight || speed <= 0) {
            return 1.0;
        }
        double gapSeconds = gapMeters / speed;
        if (gapSeconds >= Tuning.SLIPSTREAM_GAP_SECONDS) {
            return 1.0;
        }
        return 1.0 + Tuning.SLIPSTREAM_MAX_BONUS * (1.0 - gapSeconds / Tuning.SLIPSTREAM_GAP_SECONDS);
    }

    /**
     * Returns whether a car is on a straight, judged from the reference speed profile.
     *
     * @param car the car
     * @return true on straights
     */
    private boolean onStraight(Car car) {
        SpeedProfile reference = profiles.forCarGrip(Compound.MEDIUM.getDryGrip());
        return reference.cornerLimitAt(track.indexAt(car.getDistance())) >= Tuning.STRAIGHT_FRACTION * Tuning.TOP_SPEED;
    }

    /**
     * Returns the simulated qualifying results.
     *
     * @return entries, fastest first
     */
    public List<QualifyingEntry> getQualifying() {
        return Collections.unmodifiableList(qualifying);
    }

    /**
     * Decides whether a car spends battery energy this step. The player's car follows the player's
     * deploy switch; computer cars deploy while the battery is comfortably charged.
     *
     * @param car the car
     * @return true to deploy on straights
     */
    private boolean wantsDeploy(Car car) {
        return car.isPlayer() ? car.isErsWanted() : AiDriver.wantsDeploy(car);
    }

    /**
     * Finds the car that limits how far a car may move: the nearest car ahead on the road, except a
     * car that is far enough to the side to be alongside safely, which is allowed only for a passing
     * partner, or for one car in the pit lane next to one on track (two pit-lane cars queue).
     *
     * @param car the car
     * @return the blocking car, or null
     */
    private Car blockingCarAhead(Car car) {
        Car best = null;
        double bestGap = Double.MAX_VALUE;
        for (Car other : cars) {
            boolean mayBeAlongside = other == car.getPassPartner() || car.isInPitLane() != other.isInPitLane();
            if (other == car || (mayBeAlongside && sideBySide(car, other))) {
                continue;
            }
            double gap = trackGap(car, other);
            if (gap < bestGap) {
                bestGap = gap;
                best = other;
            }
        }
        return best;
    }

    /**
     * Returns whether two cars are far enough apart sideways to be alongside without touching.
     *
     * @param a one car
     * @param b the other car
     * @return true if their sideways gap is at least Tuning.LATERAL_CLEARANCE
     */
    public static boolean sideBySide(Car a, Car b) {
        return Math.abs(a.getLateralOffset() - b.getLateralOffset()) >= Tuning.LATERAL_CLEARANCE;
    }

    /**
     * Adds a message for the radio feed.
     *
     * @param text the message
     * @param aboutPlayer true if it concerns the player's car
     */
    public void addMessage(String text, boolean aboutPlayer) {
        messages.add(new RaceMessage(time, text, aboutPlayer));
    }

    /**
     * Returns all messages so far, oldest first.
     *
     * @return the messages, read-only
     */
    public List<RaceMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns the corners with braking zones.
     *
     * @return corners in lap order, read-only
     */
    public List<Corner> getCorners() {
        return Collections.unmodifiableList(corners);
    }

    /**
     * Returns the battle manager.
     *
     * @return the battles
     */
    public BattleManager getBattles() {
        return battles;
    }

    /**
     * Finds the nearest car in front on the road (not in the standings: a lapped car can be ahead on the road).
     *
     * @param car the car looking forward
     * @return the car ahead, or null if this is the only car
     */
    public Car carAheadOnTrack(Car car) {
        Car best = null;
        double bestGap = Double.MAX_VALUE;
        for (Car other : cars) {
            if (other != car) {
                double gap = trackGap(car, other);
                if (gap < bestGap) {
                    bestGap = gap;
                    best = other;
                }
            }
        }
        return best;
    }

    /**
     * Distance along the road from one car forward to another, wrapping around the lap.
     *
     * @param from the following car
     * @param to the car in front
     * @return meters, from 0 up to the lap length
     */
    public double trackGap(Car from, Car to) {
        return track.wrap(to.getDistance() - from.getDistance());
    }

    /**
     * Records any timing loops and finish lines the car crossed during its move,
     * with the crossing time interpolated inside the step.
     *
     * @param car the car
     * @param before distance before the move
     * @param move meters moved
     * @param dt time step in seconds
     */
    private void recordCrossings(Car car, double before, double move, double dt) {
        if (move <= 0) {
            return;
        }
        double after = before + move;
        double loop = Tuning.TIMING_LOOP_SPACING;
        for (long k = (long) Math.floor((before - loopOrigin) / loop) + 1; loopOrigin + k * loop <= after; k++) {
            if (k >= 0 && k < loopTimes[car.getId()].length) {
                loopTimes[car.getId()][(int) k] = time + dt * (loopOrigin + k * loop - before) / move;
            }
        }
        double lap = track.getLapLength();
        long line = (long) Math.floor(before / lap) + 1;
        if (line >= 1 && line * lap <= after && !car.isFinished()) {
            double crossTime = time + dt * (line * lap - before) / move;
            car.completeLap(crossTime);
            int lapIndex = car.getLapsCompleted() - 1;
            if (lapIndex < config.getLaps()) {
                lapTimes[car.getId()][lapIndex] = car.getLastLapTime();
                wearHistory[car.getId()][lapIndex] = car.getTire().getWear();
            }
            if (car.getLapsCompleted() >= config.getLaps() || finishedCount > 0) {
                finishedCount++;
                car.finish(crossTime, finishedCount);
                checkCompoundRule(car);
            }
        }
    }

    /**
     * Disqualifies a finished car that raced a dry race on only one dry compound.
     *
     * @param car a car that has just finished
     */
    private void checkCompoundRule(Car car) {
        if (Tuning.TWO_COMPOUND_RULE && !wetRace && car.dryCompoundsUsed() < 2) {
            car.disqualify();
            String who = car.isPlayer() ? "You are" : car.getDriver().getCode() + " is";
            addMessage(who + " disqualified: only one dry compound used", car.isPlayer());
        }
    }

    /**
     * Returns the final classification: finishing order, with disqualified cars moved to the end.
     *
     * @return cars in classified order
     */
    public List<Car> getClassification() {
        List<Car> result = new ArrayList<Car>();
        for (Car car : standings) {
            if (!car.isDisqualified()) {
                result.add(car);
            }
        }
        for (Car car : standings) {
            if (car.isDisqualified()) {
                result.add(car);
            }
        }
        return result;
    }

    /**
     * Gives the player a tire strategy before the start (from the setup screen).
     *
     * @param plan the player's plan
     * @param push the player's baseline push level
     * @throws IllegalStateException if the race has already started
     */
    public void setPlayerPlan(PitPlan plan, PushLevel push) {
        if (time > 0) {
            throw new IllegalStateException("The plan can only be set before the start");
        }
        playerCar.setPlan(plan);
        playerCar.setPushLevel(push);
    }

    /**
     * Returns whether the race counts as wet (the two-compound rule no longer applies).
     *
     * @return true once the track has been properly wet
     */
    public boolean isWetRace() {
        return wetRace;
    }

    /**
     * Orders two cars for the standings: finished cars by finishing position first,
     * then everyone else by distance driven (furthest first).
     *
     * @param a first car
     * @param b second car
     * @return negative if a is ahead of b, positive if behind
     */
    private static int compareStandings(Car a, Car b) {
        if (a.isFinished() && b.isFinished()) {
            return Integer.compare(a.getFinishPosition(), b.getFinishPosition());
        }
        if (a.isFinished() != b.isFinished()) {
            return a.isFinished() ? -1 : 1;
        }
        return Double.compare(b.getDistance(), a.getDistance());
    }

    /**
     * Returns the time gap from the race leader to a car.
     *
     * @param car the car
     * @return seconds behind the leader (0 for the leader)
     */
    public double gapToLeader(Car car) {
        return timeBehind(car, standings.get(0));
    }

    /**
     * Returns the time gap from the car one place ahead in the standings.
     *
     * @param car the car
     * @return seconds behind the car ahead (0 for the leader)
     */
    public double intervalToCarAhead(Car car) {
        int position = standings.indexOf(car);
        return position <= 0 ? 0 : timeBehind(car, standings.get(position - 1));
    }

    /**
     * Returns how many whole laps a car is behind the leader.
     *
     * @param car the car
     * @return laps down, 0 if on the lead lap
     */
    public int lapsBehindLeader(Car car) {
        Car leader = standings.get(0);
        int behind = leader.getLapsCompleted() - car.getLapsCompleted();
        if (car.isFinished() || behind <= 0) {
            return Math.max(0, behind);
        }
        return (int) Math.floor((leader.getDistance() - car.getDistance()) / track.getLapLength());
    }

    /**
     * Time gap between two cars: how long ago the car ahead was at the exact spot where the
     * following car is now. The timing loop table tells when the car ahead crossed the loops
     * either side of that spot, and the time in between is blended in proportion to distance.
     * Because it uses the following car's exact position, gaps change smoothly and always
     * agree with the running order.
     *
     * @param car the following car
     * @param ahead the car in front in the standings
     * @return seconds, never negative
     */
    private double timeBehind(Car car, Car ahead) {
        if (car == ahead) {
            return 0;
        }
        if (car.isFinished() && ahead.isFinished()) {
            return car.getFinishTime() - ahead.getFinishTime();
        }
        double loop = Tuning.TIMING_LOOP_SPACING;
        double d = car.getDistance() - loopOrigin;
        double aheadD = ahead.getDistance() - loopOrigin;
        int k = (int) Math.floor(d / loop);
        if (k < 0 || k + 1 >= loopTimes[0].length) {
            return 0;
        }
        double before = loopTimes[ahead.getId()][k];
        double after;
        double afterDistance;
        if (aheadD >= (k + 1) * loop) {
            after = loopTimes[ahead.getId()][k + 1];
            afterDistance = (k + 1) * loop;
        } else {
            after = time;
            afterDistance = aheadD;
        }
        double span = afterDistance - k * loop;
        double aheadWasHere = span <= 0 ? before : before + (after - before) * (d - k * loop) / span;
        return Math.max(0, time - aheadWasHere);
    }

    /**
     * Returns whether every car has finished.
     *
     * @return true when the race is over
     */
    public boolean isOver() {
        return finishedCount == cars.size();
    }

    /**
     * Returns the lap the leader is on, for the lap counter.
     *
     * @return current lap from 1 to the race length
     */
    public int getLeaderLap() {
        return Math.min(config.getLaps(), Math.max(1, standings.get(0).getLapsCompleted() + 1));
    }

    /**
     * Returns the race time.
     *
     * @return seconds since the start
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns all cars in grid order.
     *
     * @return the cars, read-only
     */
    public List<Car> getCars() {
        return Collections.unmodifiableList(cars);
    }

    /**
     * Returns the cars in race order.
     *
     * @return the standings, read-only
     */
    public List<Car> getStandings() {
        return Collections.unmodifiableList(standings);
    }

    /**
     * Returns the player's car.
     *
     * @return the player car
     */
    public Car getPlayerCar() {
        return playerCar;
    }

    /**
     * Returns the circuit.
     *
     * @return the track
     */
    public Track getTrack() {
        return track;
    }

    /**
     * Returns the race settings.
     *
     * @return the config
     */
    public RaceConfig getConfig() {
        return config;
    }

    /**
     * Returns the speed profiles in use.
     *
     * @return the profile set
     */
    public ProfileSet getProfiles() {
        return profiles;
    }

    /**
     * Returns the current weather on track.
     *
     * @return the conditions
     */
    public TrackConditions getConditions() {
        return conditions;
    }

    /**
     * Returns a car's tire wear at the end of a lap.
     *
     * @param car the car
     * @param lap lap number, starting at 1
     * @return wear, or 0 if the lap is not complete
     */
    public double getWearAtLap(Car car, int lap) {
        return wearHistory[car.getId()][lap - 1];
    }

    /**
     * Returns one car's time for one lap.
     *
     * @param car the car
     * @param lap lap number, starting at 1
     * @return seconds, or 0 if the lap is not complete
     */
    public double getLapTime(Car car, int lap) {
        return lapTimes[car.getId()][lap - 1];
    }
}
