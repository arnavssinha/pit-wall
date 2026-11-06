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

    /** Extra sideways room added when pushing a car back beside another, in meters, so rounding never leaves them touching. */
    private static final double NUDGE_MARGIN = 0.05;

    /** The race settings. */
    private final RaceConfig config;

    /** The circuit. */
    private final Track track;

    /** Target speeds around the lap for every grip level. */
    private final ProfileSet profiles;

    /** The DRS zones. */
    private final DrsZones drsZones;

    /** True once the "DRS enabled" message has been sent while DRS is allowed. */
    private boolean drsAnnounced;

    /** Current weather on track. */
    private TrackConditions conditions;

    /** The rain that will fall this race, or null for a dry race. */
    private RainEvent rain;

    /** The forecast shown to the player, or null if none was applied. */
    private Forecast forecast;

    /** Scheduled events (rain, incidents, retirements), earliest first. */
    private final EventQueue events = new EventQueue();

    /** The safety car. */
    private final SafetyCar safetyCar = new SafetyCar();

    /** The player's race engineer, who sends radio messages. */
    private final RadioEngineer engineer;

    /** The one random number generator for this race. */
    private final Random random;

    /** All cars, in grid order. Index equals Car.getId(). */
    private final ArrayList<Car> cars = new ArrayList<Car>();

    /**
     * Cars in the order they sit on the road (by position on the lap, not by race position),
     * re-sorted after every step. The car ahead of any car on the road is simply the next one in this
     * list, which is much faster than comparing every pair of cars.
     */
    private final ArrayList<Car> roadOrder = new ArrayList<Car>();

    /** Index of each car (by id) in roadOrder. */
    private int[] roadIndex;

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
        this.drsZones = new DrsZones(track, profiles);
        this.conditions = new TrackConditions(track, Tuning.NEUTRAL_TRACK_TEMP, Tuning.NEUTRAL_AIR_TEMP, random);
        this.engineer = new RadioEngineer(this);
        this.corners = Corner.findAll(track, profiles.forCarGrip(Compound.MEDIUM.getDryGrip()));
        this.battles = new BattleManager(this, random);

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
            PitPlan plan = isPlayer ? defaultPlan(config.getLaps()) : PitStrategyPlanner.choosePlan(random, config.getLaps());
            Car car = new Car(i, entry.getDriver(), i + 1, entry.getPace(), gridDistance, gridLateral,
                    plan.getStartCompound(), isPlayer);
            car.setPlan(plan);
            car.setTireLifeScale(RaceLength.tireLifeScale(config.getLaps()));
            double spread = Tuning.AI_AGGRESSION_MAX - Tuning.AI_AGGRESSION_MIN;
            car.setAggression(Tuning.AI_AGGRESSION_MIN + random.nextDouble() * spread);
            car.setLaunchDelay(Tuning.LAUNCH_DELAY_MIN + random.nextDouble() * Tuning.LAUNCH_DELAY_SPREAD);
            car.setErsStyle(random.nextDouble());
            cars.add(car);
            standings.add(car);
            if (isPlayer) {
                player = car;
            }
        }
        playerCar = player;
        this.pitLane = new PitLane(this, random);
        roadOrder.addAll(cars);
        roadIndex = new int[cars.size()];
        refreshRoadOrder();
        scheduleIncidents();

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
        handleEvents();
        if (Math.floor(time) != Math.floor(time - dt)) {
            double biasRange = leader().getLapsCompleted() == 0 ? Tuning.LINE_BIAS_START_RANGE : Tuning.LINE_BIAS_RANGE;
            for (Car car : cars) {
                car.updateForm(random.nextGaussian() * Tuning.FORM_STEP);
                car.updateLineBias(random.nextGaussian() * Tuning.LINE_BIAS_STEP, biasRange);
            }
        }
        conditions.update(dt, rain == null ? 0 : rain.intensityAt(time));
        if (conditions.averageWetness() > Tuning.SLICK_WET_CLIFF) {
            wetRace = true;
        }
        battles.beforeMove();
        pitLane.beforeMove();
        double[] moves = new double[n];
        for (int i = 0; i < n; i++) {
            moves[i] = planMove(cars.get(i), dt);
        }
        double[] lateralBefore = new double[n];
        for (int i = 0; i < n; i++) {
            lateralBefore[i] = cars.get(i).getLateralOffset();
        }
        for (int i = 0; i < n; i++) {
            Car car = cars.get(i);
            double before = car.getDistance();
            car.advance(moves[i], dt);
            car.steer(personalLine(car), moves[i], dt);
        }
        keepSideBySideRoom(lateralBefore);
        for (int i = 0; i < n; i++) {
            Car car = cars.get(i);
            double before = car.getDistance() - moves[i];
            pitLane.afterMove(car, before, before + moves[i], dt);
            updateDrs(car, before, moves[i]);
            car.updateSystems(dt, moves[i], track, profiles, conditions, wantsDeploy(car));
            recordCrossings(car, before, moves[i], dt);
        }
        time += dt;
        Collections.sort(standings, Race::compareStandings);
        refreshRoadOrder();
        battles.afterMove();
        engineer.update();
    }

    /**
     * Sorts the cars by where they are on the lap and remembers each car's place in that order.
     */
    private void refreshRoadOrder() {
        Collections.sort(roadOrder, (a, b) -> Double.compare(track.wrap(a.getDistance()), track.wrap(b.getDistance())));
        for (int i = 0; i < roadOrder.size(); i++) {
            roadIndex[roadOrder.get(i).getId()] = i;
        }
    }

    /**
     * Stops cars from sliding sideways into a car they are running alongside. Only the nearest cars
     * on the road can overlap, so each car checks a few neighbours either side in the road order.
     * For every pair that overlaps lengthwise and was side by side before this step, a car that moved too
     * close is pushed back so the gap stays at least the clearance they need (or, if they were already a
     * little closer than that, at least as wide as it was), on the side it was on. The car that moved
     * toward the other is the one moved back (each in proportion to how far it moved), and a car already
     * at the track edge cannot go further, so the other car takes the rest.
     * A car parked in its box and another team's car are left alone: the parked car is out of the way.
     *
     * @param lateralBefore each car's sideways position before this step, by car id
     */
    private void keepSideBySideRoom(double[] lateralBefore) {
        for (int pass = 0; pass < Tuning.SIDE_BY_SIDE_PASSES; pass++) {
            if (!keepRoomOnce(lateralBefore)) {
                return;
            }
        }
    }

    /**
     * One pass of the side-by-side check (see keepSideBySideRoom). Several passes are needed when three
     * cars are squeezed together, because pushing one car away from a second can push it into a third.
     *
     * @param lateralBefore each car's sideways position before this step, by car id
     * @return true if any car was pushed
     */
    private boolean keepRoomOnce(double[] lateralBefore) {
        boolean pushedAny = false;
        int n = roadOrder.size();
        int reach = Math.min(Tuning.SIDE_BY_SIDE_NEIGHBOURS, n - 1);
        for (Car car : cars) {
            int index = roadIndex[car.getId()];
            for (int k = -reach; k <= reach; k++) {
                Car other = roadOrder.get(Math.floorMod(index + k, n));
                if (other == car || car.isRetired() || other.isRetired() || parkedApart(car, other)) {
                    continue;
                }
                boolean overlapping = trackGap(car, other) < Tuning.MIN_SEPARATION
                        || trackGap(other, car) < Tuning.MIN_SEPARATION;
                double wasApart = lateralBefore[car.getId()] - lateralBefore[other.getId()];
                if (!overlapping || Math.abs(wasApart) < Tuning.CAR_WIDTH / 2) {
                    continue;
                }
                double needed = Math.min(clearanceBetween(car, other), Math.abs(wasApart));
                double apart = car.getLateralOffset() - other.getLateralOffset();
                if (Math.abs(apart) < needed || Math.signum(apart) != Math.signum(wasApart)) {
                    double sign = Math.signum(wasApart);
                    double carMoved = Math.max(0, (lateralBefore[car.getId()] - car.getLateralOffset()) * sign);
                    double otherMoved = Math.max(0, (other.getLateralOffset() - lateralBefore[other.getId()]) * sign);
                    if (carMoved + otherMoved < 1e-9) {
                        carMoved = 1;
                        otherMoved = 1;
                    }
                    double deficit = needed + NUDGE_MARGIN - apart * sign;
                    double carShare = deficit * carMoved / (carMoved + otherMoved);
                    double carWanted = car.getLateralOffset() + sign * carShare;
                    double carKept = keepOnTrack(car, carWanted);
                    double otherWanted = other.getLateralOffset() - sign * (deficit - carShare) - (carWanted - carKept);
                    double otherKept = keepOnTrack(other, otherWanted);
                    car.nudgeLateral(carKept - (otherWanted - otherKept));
                    other.nudgeLateral(otherKept);
                    pushedAny = true;
                }
            }
        }
        return pushedAny;
    }

    /**
     * Keeps a pushed car inside the track edges (a car in the pit lane or parked off the track is left
     * where it is pushed).
     *
     * @param car the car
     * @param lateral where it is being pushed to
     * @return the sideways position it may have
     */
    private static double keepOnTrack(Car car, double lateral) {
        if (car.isOffTrack()) {
            return lateral;
        }
        double limit = Tuning.TRACK_WIDTH / 2 - Tuning.CAR_WIDTH / 2;
        return Math.max(-limit, Math.min(limit, lateral));
    }

    /**
     * Rolls this race's incidents at the start and puts them in the event queue: possibly a car
     * stopping on track (which brings out the safety car), and rare mechanical retirements.
     */
    private void scheduleIncidents() {
        double lap = track.getProfile().getMedianRaceLapSeconds();
        double first = Tuning.INCIDENT_FIRST_FRACTION * config.getLaps() * lap;
        double span = (Tuning.INCIDENT_LAST_FRACTION - Tuning.INCIDENT_FIRST_FRACTION) * config.getLaps() * lap;
        if (random.nextDouble() < track.getProfile().getSafetyCarProbability()) {
            Car victim = cars.get(random.nextInt(cars.size()));
            if (victim != playerCar) {
                events.schedule(new RaceEvent(first + random.nextDouble() * span, RaceEvent.Type.SAFETY_CAR, victim));
            }
        }
        for (Car car : cars) {
            if (random.nextDouble() < Tuning.RETIREMENT_CHANCE) {
                events.schedule(new RaceEvent(first + random.nextDouble() * span, RaceEvent.Type.RETIREMENT, car));
            }
        }
    }

    /**
     * Handles every scheduled event that is due.
     */
    private void handleEvents() {
        for (RaceEvent event : events.takeDue(time)) {
            Car car = event.getCar();
            switch (event.getType()) {
                case RAIN_START:
                    addMessage("Rain is starting!", true);
                    break;
                case RAIN_STOP:
                    addMessage("Rain has stopped. The track will start to dry.", true);
                    break;
                case SAFETY_CAR:
                    if (!retire(car, "has stopped on track")) {
                        retryLater(event);
                    } else {
                        events.schedule(new RaceEvent(time + Tuning.SC_DEPLOY_DELAY + Tuning.SC_DURATION_SECONDS,
                                RaceEvent.Type.SAFETY_CAR_END, null));
                        safetyCar.deploy();
                        battles.cancelAll();
                        addMessage("SAFETY CAR deployed. A pit stop is cheap now.", true);
                    }
                    break;
                case SAFETY_CAR_END:
                    safetyCar.end();
                    addMessage("Safety car is in. Green flag, racing resumes!", true);
                    break;
                default:
                    if (!retire(car, "retires with a mechanical failure")) {
                        retryLater(event);
                    }
                    break;
            }
        }
    }

    /**
     * Tries an incident again a little later, when its car was busy (in the pit lane or mid-pass).
     * Cars that have finished or already retired are left alone.
     *
     * @param event the incident
     */
    private void retryLater(RaceEvent event) {
        Car car = event.getCar();
        if (!car.isFinished() && !car.isRetired()) {
            events.schedule(new RaceEvent(time + Tuning.INCIDENT_RETRY_SECONDS, event.getType(), car));
        }
    }

    /**
     * Retires a car if it is still racing on track (cars in the pit lane or already finished are left alone).
     *
     * @param car the car
     * @param reason the end of the radio message, such as "has stopped on track"
     * @return true if the car was retired
     */
    private boolean retire(Car car, String reason) {
        if (car.isFinished() || car.isRetired() || car.isInPitLane()) {
            return false;
        }
        car.setLinePlan(null);
        car.setYieldTo(null, time);
        car.retire(-track.getProfile().getPitSide());
        addMessage((car.isPlayer() ? "Your car" : car.getDriver().getCode()) + " " + reason + ".", true);
        return true;
    }

    /**
     * Applies a forecast before the start: sets the temperatures and schedules any rain.
     *
     * @param forecast the forecast
     * @throws IllegalStateException if the race has already started
     */
    public void applyForecast(Forecast forecast) {
        if (time > 0) {
            throw new IllegalStateException("The weather can only be set before the start");
        }
        this.forecast = forecast;
        conditions = new TrackConditions(track, forecast.getTrackTemp(), forecast.getAirTemp(), random);
        setRain(forecast.getRain());
    }

    /**
     * Sets the rain for this race and schedules its start and stop messages.
     *
     * @param spell the rain, or null for a dry race
     */
    public void setRain(RainEvent spell) {
        rain = spell;
        if (spell != null) {
            events.schedule(new RaceEvent(spell.getStart(), RaceEvent.Type.RAIN_START, null));
            events.schedule(new RaceEvent(spell.getEnd(), RaceEvent.Type.RAIN_STOP, null));
        }
    }

    /**
     * Schedules a car to stop on track at a given time, bringing out the safety car (for tests and scenarios).
     *
     * @param when race time in seconds
     * @param car the car that stops
     */
    public void scheduleSafetyCar(double when, Car car) {
        events.schedule(new RaceEvent(when, RaceEvent.Type.SAFETY_CAR, car));
    }

    /**
     * Schedules a mechanical retirement (for tests and scenarios).
     *
     * @param when race time in seconds
     * @param car the car that retires
     */
    public void scheduleRetirement(double when, Car car) {
        events.schedule(new RaceEvent(when, RaceEvent.Type.RETIREMENT, car));
    }

    /**
     * Returns the forecast shown to the player.
     *
     * @return the forecast, or null if none was applied
     */
    public Forecast getForecast() {
        return forecast;
    }

    /**
     * Returns the rain for this race.
     *
     * @return the rain spell, or null for a dry race
     */
    public RainEvent getRain() {
        return rain;
    }

    /**
     * Returns the pit lane.
     *
     * @return the pit lane
     */
    public PitLane getPitLane() {
        return pitLane;
    }

    /**
     * Returns the safety car.
     *
     * @return the safety car
     */
    public SafetyCar getSafetyCar() {
        return safetyCar;
    }

    /**
     * Decides how far one car moves this step.
     * The car aims for its own target speed (profile, push, ERS, DRS, fight line), loses a little grip
     * in another car's dirty air through corners, gets a tow on straights when it is right in another
     * car's wake, gives way to a car it has lost a corner to, eases off to follow a close car ahead, and
     * is finally clamped so it can never get nearer than the minimum separation to the car blocking it.
     * Nothing here hands a car extra speed for an overtake: passes come only from these effects.
     *
     * @param car the car
     * @param dt time step in seconds
     * @return meters to move, never negative
     */
    private double planMove(Car car, double dt) {
        double target = Math.min(car.targetSpeed(profiles, conditions), pitLane.speedLimit(car));
        if (time < car.getLaunchDelay()) {
            target = 0;
        }
        Car ahead = blockingCarAhead(car);
        double gap = ahead == null ? Double.MAX_VALUE : trackGap(car, ahead);
        if (!car.isInPitLane()) {
            target *= safetyCar.paceFactor(car == leader(), gap);
        }
        Car front = carAheadOnTrack(car);
        double frontGap = front == null ? Double.MAX_VALUE : trackGap(car, front);
        boolean straight = onStraight(car);
        if (front != null && !car.isInPitLane() && !front.isInPitLane()) {
            double inWake = Math.max(0, 1 - Math.abs(car.getLateralOffset() - front.getLateralOffset()) / Tuning.LATERAL_CLEARANCE);
            double seconds = frontGap / Math.max(1, car.getSpeed());
            if (!straight && seconds < Tuning.DIRTY_AIR_SECONDS) {
                target *= 1 - Tuning.DIRTY_AIR_LOSS * inWake * (1 - seconds / Tuning.DIRTY_AIR_SECONDS);
            }
            double tow = 1 + (slipstreamFactor(frontGap, car.getSpeed(), straight) - 1) * inWake;
            if (tow > 1.0) {
                target *= tow;
                car.applyTow(tow);
            }
        }
        Car winner = car.getYieldTo();
        if (winner != null) {
            double floor = target * Tuning.YIELD_FLOOR_FRACTION;
            target = Math.min(target, Math.max(floor, winner.getSpeed() - Tuning.PASS_YIELD_SPEED));
        }
        LinePlan plan = car.getLinePlan();
        boolean attackingFront = plan != null && plan.isAttacker() && plan.getChoice().leavesLine();
        boolean racingFront = front != null && !front.isInPitLane() && !car.isInPitLane() && !front.isRetired()
                && front != winner && car.getYieldTo() == null && !front.isFinished();
        if (racingFront && !attackingFront) {
            double keep = plan != null && plan.getChoice() == LineChoice.STAY_BEHIND
                    ? Tuning.STAY_BEHIND_GAP_SECONDS : Tuning.FOLLOW_TIME_GAP;
            if (frontGap < Tuning.MIN_SEPARATION + car.getSpeed() * keep) {
                double drop = frontGap < Tuning.MIN_SEPARATION ? Tuning.OVERLAP_DROP_SPEED : 0;
                target = Math.min(target, front.getSpeed() - drop);
            }
        }
        if (car.getPitPhase() == PitPhase.MERGING && hasCarAlongside(car, true)) {
            target *= 1 - Tuning.ABORT_SLOWDOWN;
        }
        target = Math.max(0, target);
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
     * Returns the line this car normally drives here: the racing line plus the driver's own small
     * offset, kept inside the track.
     *
     * @param car the car
     * @return sideways position in meters from the centerline
     */
    public double personalLine(Car car) {
        double limit = Tuning.TRACK_WIDTH / 2 - Tuning.RACING_LINE_EDGE_MARGIN;
        return Math.max(-limit, Math.min(limit, track.lineAt(car.getDistance()) + car.getLineBias()));
    }

    /**
     * Opens and closes a car's DRS. Crossing a detection point less than Tuning.DRS_GAP_SECONDS behind
     * the car ahead earns DRS for that zone; the flap is open while the car is in the zone and shuts
     * at its end. DRS is off on the first lap, under the safety car and on a wet track.
     *
     * @param car the car
     * @param before distance before this step
     * @param meters distance driven this step
     */
    private void updateDrs(Car car, double before, double meters) {
        boolean allowed = isDrsAllowed();
        if (allowed != drsAnnounced) {
            drsAnnounced = allowed;
            addMessage(allowed ? "DRS enabled." : "DRS disabled.", false);
        }
        if (!allowed || car.isInPitLane() || car.isRetired() || car.isFinished()) {
            car.setDrs(-1, false);
            return;
        }
        int zone = car.getDrsZone();
        int crossed = drsZones.detectionCrossed(before, meters);
        if (crossed >= 0) {
            Car ahead = carAheadOnTrack(car);
            boolean close = ahead != null
                    && trackGap(car, ahead) / Math.max(1, car.getSpeed()) < Tuning.DRS_GAP_SECONDS;
            zone = close ? crossed : -1;
        }
        boolean inZone = zone >= 0 && drsZones.zoneAt(car.getDistance()) == zone;
        if (car.isDrsOpen() && !inZone) {
            zone = -1;
        }
        car.setDrs(zone, inZone);
    }

    /**
     * Returns whether DRS may be used right now.
     *
     * @return true from the second lap on, on a dry track, with no safety car
     */
    public boolean isDrsAllowed() {
        Car first = standings.get(0);
        return first.getLapsCompleted() >= Tuning.DRS_FIRST_LAP - 1 && !safetyCar.isActive()
                && conditions.averageWetness() < Tuning.DRS_MAX_WETNESS && drsZones.size() > 0;
    }

    /**
     * Returns the DRS zones.
     *
     * @return the zones
     */
    public DrsZones getDrsZones() {
        return drsZones;
    }

    /**
     * Returns whether a car on the track is level with this one (overlapping lengthwise). A car
     * rejoining from the pits uses this to lift and slot in behind instead of running alongside
     * forever, and only counts as back in the race once nobody overlaps it.
     *
     * @param car the car
     * @param aheadOnly true to count only cars level or ahead (the ones this car should drop behind)
     * @return true if a car on track or rejoining (not in the pit lane itself, not retired) overlaps it
     */
    public boolean hasCarAlongside(Car car, boolean aheadOnly) {
        int n = roadOrder.size();
        int index = roadIndex[car.getId()];
        int reach = Math.min(Tuning.SIDE_BY_SIDE_NEIGHBOURS, n - 1);
        for (int k = -reach; k <= reach; k++) {
            Car other = roadOrder.get(Math.floorMod(index + k, n));
            boolean inLane = other.isInPitLane() && other.getPitPhase() != PitPhase.MERGING;
            if (other == car || inLane || other.isRetired()) {
                continue;
            }
            boolean levelOrAhead = trackGap(car, other) < Tuning.MIN_SEPARATION;
            if (levelOrAhead || (!aheadOnly && trackGap(other, car) < Tuning.MIN_SEPARATION)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Speed bonus from the slipstream: a car close behind another on a straight punches a smaller
     * hole in the air, so it can go a little faster. The bonus is largest right behind the car
     * ahead and fades (slowly at first, then quickly) to nothing at Tuning.SLIPSTREAM_GAP_SECONDS.
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
        double t = gapSeconds / Tuning.SLIPSTREAM_GAP_SECONDS;
        return 1.0 + Tuning.SLIPSTREAM_MAX_BONUS * (1.0 - t * t);
    }

    /**
     * Returns whether a car is on a straight, judged from the reference speed profile.
     *
     * @param car the car
     * @return true on straights
     */
    private boolean onStraight(Car car) {
        return profiles.isStraight(car.getDistance());
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
        if (safetyCar.isActive()) {
            return false;
        }
        return car.isPlayer() ? car.isErsWanted() : AiDriver.wantsDeploy(car, track.wrap(car.getDistance()));
    }

    /**
     * Finds the car that limits how far a car may move: the nearest car ahead on the road that is not
     * far enough to the side to be alongside safely. Any two cars may run side by side when there is
     * room (that is how cars draw alongside and pass, even two or three at a time); the corner rules
     * in BattleManager then decide who gets the corner. A car parked in its pit box never blocks
     * another team's car at all.
     *
     * @param car the car
     * @return the blocking car, or null
     */
    private Car blockingCarAhead(Car car) {
        int n = roadOrder.size();
        int start = roadIndex[car.getId()];
        for (int k = 1; k < n; k++) {
            Car other = roadOrder.get((start + k) % n);
            if (parkedApart(car, other)) {
                continue;
            }
            if (!sideBySide(car, other)) {
                return other;
            }
        }
        return null;
    }

    /**
     * Returns whether one of two cars is parked in its pit box while the other belongs to a different
     * team. A parked car sits beside the fast lane, out of everyone's way, so it never blocks another
     * team's car; a teammate arriving for a double stack still queues behind it.
     *
     * @param a one car
     * @param b the other car
     * @return true if they can never block each other
     */
    private static boolean parkedApart(Car a, Car b) {
        boolean parked = a.getPitPhase() == PitPhase.STOPPED || b.getPitPhase() == PitPhase.STOPPED;
        return parked && a.isInPitLane() && b.isInPitLane() && a.getDriver().getTeam() != b.getDriver().getTeam();
    }

    /**
     * Returns whether two cars are far enough apart sideways to be alongside without touching.
     *
     * @param a one car
     * @param b the other car
     * @return true if their sideways gap is at least Tuning.LATERAL_CLEARANCE (Tuning.PIT_LATERAL_CLEARANCE
     *         for two cars in the pit lane, which drive slowly and park in their boxes)
     */
    public static boolean sideBySide(Car a, Car b) {
        return Math.abs(a.getLateralOffset() - b.getLateralOffset()) >= clearanceBetween(a, b);
    }

    /**
     * Returns the sideways gap two cars need to run alongside each other.
     *
     * @param a one car
     * @param b the other car
     * @return Tuning.PIT_LATERAL_CLEARANCE if both are in the pit lane, otherwise Tuning.LATERAL_CLEARANCE
     */
    private static double clearanceBetween(Car a, Car b) {
        return a.isInPitLane() && b.isInPitLane() ? Tuning.PIT_LATERAL_CLEARANCE : Tuning.LATERAL_CLEARANCE;
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
        int n = roadOrder.size();
        return n < 2 ? null : roadOrder.get((roadIndex[car.getId()] + 1) % n);
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
        if (line >= 1 && line * lap <= after && !car.isFinished() && !car.isRetired()) {
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
     * Returns the player's starting plan before the setup screen changes it: one-stop late where the
     * race length allows it, otherwise the first strategy that fits.
     *
     * @param laps race length in laps
     * @return a fresh plan
     */
    public static PitPlan defaultPlan(int laps) {
        List<StrategyPreset> allowed = StrategyPreset.forRace(laps);
        StrategyPreset preset = allowed.contains(StrategyPreset.ONE_STOP_LATE) && RaceLength.compoundRuleApplies(laps)
                ? StrategyPreset.ONE_STOP_LATE : allowed.get(0);
        return preset.makePlan(laps);
    }

    /**
     * Disqualifies a finished car that raced a dry race on only one dry compound.
     *
     * @param car a car that has just finished
     */
    private void checkCompoundRule(Car car) {
        if (RaceLength.compoundRuleApplies(config.getLaps()) && !wetRace && car.dryCompoundsUsed() < 2) {
            car.disqualify();
            String who = car.isPlayer() ? "You are" : car.getDriver().getCode() + " is";
            addMessage(who + " disqualified: only one dry compound used", car.isPlayer());
        }
    }

    /**
     * Returns the final classification: finishers in order, then retired cars (DNF), then
     * disqualified cars.
     *
     * @return cars in classified order
     */
    public List<Car> getClassification() {
        List<Car> result = new ArrayList<Car>();
        for (Car car : standings) {
            if (!car.isDisqualified() && !car.isRetired()) {
                result.add(car);
            }
        }
        for (Car car : standings) {
            if (car.isRetired() && !car.isDisqualified()) {
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
     * Returns the race leader among cars still running.
     *
     * @return the first car in the standings
     */
    private Car leader() {
        return standings.get(0);
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
     * Orders two cars for the standings: retired cars go to the back; finished cars come first by
     * finishing position; everyone else is ordered by distance driven (furthest first).
     *
     * @param a first car
     * @param b second car
     * @return negative if a is ahead of b, positive if behind
     */
    private static int compareStandings(Car a, Car b) {
        if (a.isRetired() != b.isRetired()) {
            return a.isRetired() ? 1 : -1;
        }
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
     * Returns when a car passed a point, read from the timing loops and blended between the two loops
     * either side of it.
     *
     * @param car the car
     * @param distance total distance in meters (lap number times lap length plus position on the lap)
     * @return race time in seconds, or NaN if the car has not reached that point
     */
    public double timeAtDistance(Car car, double distance) {
        if (distance > car.getDistance()) {
            return Double.NaN;
        }
        double loop = Tuning.TIMING_LOOP_SPACING;
        double d = distance - loopOrigin;
        int k = (int) Math.floor(d / loop);
        if (k < 0 || k + 1 >= loopTimes[0].length) {
            return Double.NaN;
        }
        double before = loopTimes[car.getId()][k];
        double after = (k + 1) * loop <= car.getDistance() - loopOrigin ? loopTimes[car.getId()][k + 1] : time;
        return before + (after - before) * (d - k * loop) / loop;
    }

    /**
     * Runs the race to its end without drawing (used when the player skips ahead to the results).
     *
     * @param maxSeconds stop after this much more race time even if not over
     */
    public void runToEnd(double maxSeconds) {
        double stop = time + maxSeconds;
        while (!isOver() && time < stop) {
            step();
        }
    }

    /**
     * Returns whether every car has finished.
     *
     * @return true when the race is over
     */
    public boolean isOver() {
        int retired = 0;
        for (Car car : cars) {
            if (car.isRetired()) {
                retired++;
            }
        }
        return finishedCount + retired == cars.size();
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
