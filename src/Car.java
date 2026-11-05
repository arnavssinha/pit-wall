import java.util.EnumSet;

/**
 * One car in the race: who drives it, where it is, how fast it goes, and the state of its
 * tires, brakes and battery.
 * Position is stored as total distance driven since the start/finish line (negative while on
 * the grid). Lap number and position on the lap both come from that one number, which keeps
 * lap counting simple and exact.
 * Each step the race asks the car for its target speed (from the speed profile that matches its
 * current grip), moves it, then lets the car update its tires, brakes and battery.
 */
public final class Car {

    /** Row of this car in the race's tables (lap times, timing loops). */
    private final int id;

    /** The driver. */
    private final Driver driver;

    /** Starting position, 1 for pole. */
    private final int gridPosition;

    /**
     * Speed multiplier for this race (1.0 is average). Random within Tuning.PACE_NOISE;
     * not a rating of the real driver.
     */
    private final double paceFactor;

    /** True for the car the player engineers. */
    private final boolean player;

    /** The brakes. */
    private final Brakes brakes = new Brakes();

    /** The energy recovery system. */
    private final Ers ers = new Ers();

    /** The tires currently fitted. */
    private Tire tire;

    /** How hard the driver is pushing. */
    private PushLevel pushLevel = PushLevel.BALANCED;

    /** Whether the driver wants to deploy energy on the straights. */
    private boolean ersWanted;

    /** Total meters driven since the start/finish line; negative on the grid. */
    private double distance;

    /** Current speed in m/s. */
    private double speed;

    /** Braking deceleration during the last step, in m/s squared (0 if not braking). */
    private double deceleration;

    /** Top speed allowed this step, including push level and ERS, in m/s. */
    private double currentTopSpeed;

    /** Grip available this step from tires and brakes, used when braking. */
    private double currentGrip = 1.0;

    /** Sideways offset from the centerline in meters, positive to the left. */
    private double lateralOffset;

    /** Sideways offset the car is moving toward, in meters. */
    private double lateralTarget;

    /**
     * How willing a computer driver is to attack, drawn from a narrow random band each race.
     * Not a rating of the real driver (the brief forbids invented ratings).
     */
    private double aggression = 0.5;

    /** The stance a computer driver plans for its next corner, shown early as an attack warning. */
    private Stance intent = Stance.HOLD;

    /** Which corner (lap times 1000 plus corner number) the intent is for, or -1. */
    private int intentKey = -1;

    /** Which corner (lap times 1000 plus corner number) this car last battled at as attacker, or -1. */
    private int lastBattleKey = -1;

    /** The other car in a pass that is under way, or null. */
    private Car passPartner;

    /** True if this car is the one doing the passing. */
    private boolean passAttacker;

    /** Seconds of time still to be lost from an incident or a defensive line. */
    private double timePenalty;

    /** How fast the car moves sideways right now, in meters per second. */
    private double lateralSpeed = Tuning.LATERAL_SPEED;

    /** The car's tire strategy. */
    private PitPlan plan;

    /** Where the car is in a pit stop. */
    private PitPhase pitPhase = PitPhase.NONE;

    /** Pit entries until the car pits: 0 no stop requested, 1 the next entry, 2 the one after. */
    private int pitRequest;

    /** Tires to fit at the requested stop. */
    private Compound requestedCompound;

    /** Seconds the crew still needs while the car is stopped. */
    private double crewTimer;

    /** Number of pit stops made. */
    private int stopsMade;

    /** Every compound this car has raced on, for the two-compound rule. */
    private final EnumSet<Compound> usedCompounds = EnumSet.noneOf(Compound.class);

    /** True if the car was disqualified (for example, for breaking the two-compound rule). */
    private boolean disqualified;

    /** True if the car has retired from the race (mechanical failure or stopped on track). */
    private boolean retired;

    /** Number of laps completed. */
    private int lapsCompleted;

    /** Race time when the current lap started, in seconds. */
    private double lapStartTime;

    /** Time of the last completed lap in seconds, or 0 before the first lap is complete. */
    private double lastLapTime;

    /** Best lap time so far in seconds, or 0 before the first lap is complete. */
    private double bestLapTime;

    /** True once the car has taken the chequered flag. */
    private boolean finished;

    /** Race time when the car finished, in seconds. */
    private double finishTime;

    /** Finishing position, 1 for the winner, 0 while still racing. */
    private int finishPosition;

    /**
     * Places a car on its grid slot, stationary, on fresh tires.
     *
     * @param id row in the race tables
     * @param driver the driver
     * @param gridPosition starting position, 1 for pole
     * @param paceFactor speed multiplier for this race
     * @param startDistance grid slot as distance from the line (negative, behind it)
     * @param startLateral grid slot sideways offset in meters
     * @param startCompound tires fitted at the start
     * @param player true for the player's car
     */
    public Car(int id, Driver driver, int gridPosition, double paceFactor, double startDistance,
               double startLateral, Compound startCompound, boolean player) {
        this.id = id;
        this.driver = driver;
        this.gridPosition = gridPosition;
        this.paceFactor = paceFactor;
        this.distance = startDistance;
        this.lateralOffset = startLateral;
        this.tire = new Tire(startCompound);
        this.usedCompounds.add(startCompound);
        this.plan = new PitPlan(startCompound);
        this.player = player;
        this.speed = 0;
        this.currentTopSpeed = Tuning.TOP_SPEED;
    }

    /**
     * Works out the speed this car wants at its current position.
     * The car's grip (tires times brake fade) picks the matching speed profile; push level,
     * this race's pace factor and any ERS boost then scale it.
     *
     * @param profiles speed profiles for this track
     * @param conditions current weather
     * @return the target speed in m/s
     */
    public double targetSpeed(ProfileSet profiles, TrackConditions conditions) {
        currentGrip = tire.grip(conditions.wetnessAt(distance)) * brakes.fadeFactor();
        double scale = paceFactor * pushLevel.getPace() * ers.boost();
        currentTopSpeed = Tuning.TOP_SPEED * scale;
        if (retired) {
            return 0;
        }
        double target = profiles.forCarGrip(currentGrip).speedAt(distance) * scale;
        if (timePenalty > 0) {
            target *= Tuning.PENALTY_SPEED_FRACTION;
        }
        return finished ? target * Tuning.COOL_DOWN_PACE : target;
    }

    /**
     * Raises this step's top speed when towed along in another car's slipstream.
     *
     * @param factor speed multiplier from the slipstream, 1 or more
     */
    public void applyTow(double factor) {
        currentTopSpeed *= factor;
    }

    /**
     * Moves the speed toward a target, accelerating or braking within the car's limits,
     * and remembers how hard it braked (which heats the brakes and charges the battery).
     *
     * @param targetSpeed wanted speed in m/s
     * @param gripScale the track's calibrated grip scale, which limits braking
     * @param dt time step in seconds
     */
    public void approachSpeed(double targetSpeed, double gripScale, double dt) {
        double before = speed;
        if (speed < targetSpeed) {
            double gain = CarPhysics.acceleration(speed, currentTopSpeed) * dt;
            speed = Math.min(targetSpeed, speed + gain);
        } else {
            double loss = CarPhysics.braking(speed, gripScale * currentGrip) * dt;
            speed = Math.max(targetSpeed, speed - loss);
        }
        deceleration = Math.max(0, (before - speed) / dt);
    }

    /**
     * Moves the car forward along the track and sideways toward its lateral target, and counts
     * down any time penalty. Driving at a fraction f of normal speed loses (1 / f - 1) seconds
     * for every second driven, which is how the penalty is used up.
     *
     * @param meters distance to move, never negative
     * @param dt time step in seconds
     */
    public void advance(double meters, double dt) {
        distance += meters;
        double step = lateralSpeed * dt;
        double toGo = lateralTarget - lateralOffset;
        if (Math.abs(toGo) <= step) {
            lateralOffset = lateralTarget;
        } else {
            lateralOffset += Math.signum(toGo) * step;
        }
        if (timePenalty > 0) {
            timePenalty -= dt * (1.0 / Tuning.PENALTY_SPEED_FRACTION - 1.0);
        }
    }

    /**
     * Starts a pass: this car and its partner move to opposite sides of the track.
     *
     * @param partner the other car in the pass
     * @param attacking true if this car is the one passing
     * @param lateral sideways offset to move to, in meters
     */
    public void beginPass(Car partner, boolean attacking, double lateral) {
        passPartner = partner;
        passAttacker = attacking;
        setLateralTarget(lateral, Tuning.LATERAL_SPEED);
    }

    /**
     * Sets where the car moves sideways and how fast.
     *
     * @param target sideways offset in meters, positive to the left
     * @param speed sideways speed in meters per second
     */
    public void setLateralTarget(double target, double speed) {
        lateralTarget = target;
        lateralSpeed = speed;
    }

    /**
     * Returns where the car is moving sideways to.
     *
     * @return sideways offset target in meters
     */
    public double getLateralTarget() {
        return lateralTarget;
    }

    /**
     * Ends a pass and heads back to the racing line.
     */
    public void endPass() {
        passPartner = null;
        passAttacker = false;
        setLateralTarget(0, Tuning.LATERAL_SPEED);
    }

    /**
     * Asks to pit at a coming pit entry.
     *
     * @param entries 1 to pit at the next pit entry, 2 to pit at the one after
     * @param compound tires to fit
     */
    public void requestPit(int entries, Compound compound) {
        pitRequest = entries;
        requestedCompound = compound;
    }

    /**
     * Cancels a pit request (only possible before entering the pit lane).
     */
    public void cancelPit() {
        pitRequest = 0;
    }

    /**
     * Counts down a pit request when the car drives past a pit entry without stopping.
     */
    public void skipPitEntry() {
        pitRequest--;
    }

    /**
     * Returns the pit request.
     *
     * @return 0 for none, 1 for the next entry, 2 for the one after
     */
    public int getPitRequest() {
        return pitRequest;
    }

    /**
     * Returns the tires to fit at the requested stop.
     *
     * @return the compound, or null if never requested
     */
    public Compound getRequestedCompound() {
        return requestedCompound;
    }

    /**
     * Changes the tires to fit at the requested stop.
     *
     * @param compound the compound
     */
    public void setRequestedCompound(Compound compound) {
        requestedCompound = compound;
    }

    /**
     * Moves the car to a new pit phase.
     *
     * @param phase the new phase
     */
    public void setPitPhase(PitPhase phase) {
        pitPhase = phase;
        if (phase == PitPhase.IN_LANE) {
            pitRequest = 0;
        }
    }

    /**
     * Returns the pit phase.
     *
     * @return the phase
     */
    public PitPhase getPitPhase() {
        return pitPhase;
    }

    /**
     * Returns whether the car is anywhere in the pit lane or merging out of it.
     *
     * @return true unless the phase is NONE
     */
    public boolean isInPitLane() {
        return pitPhase != PitPhase.NONE;
    }

    /**
     * Starts the crew's work.
     *
     * @param seconds how long the crew needs
     */
    public void startCrew(double seconds) {
        crewTimer = seconds;
        speed = 0;
    }

    /**
     * Counts down the crew's work.
     *
     * @param dt time step in seconds
     * @return true once the crew is finished
     */
    public boolean crewWork(double dt) {
        crewTimer -= dt;
        return crewTimer <= 0;
    }

    /**
     * Records a completed stop.
     */
    public void countStop() {
        stopsMade++;
    }

    /**
     * Returns the number of stops made.
     *
     * @return stops
     */
    public int getStopsMade() {
        return stopsMade;
    }

    /**
     * Sets the tire strategy and fits its starting tires. Only meant before the start.
     *
     * @param newPlan the plan
     */
    public void setPlan(PitPlan newPlan) {
        plan = newPlan;
        tire = new Tire(newPlan.getStartCompound());
        usedCompounds.clear();
        usedCompounds.add(newPlan.getStartCompound());
    }

    /**
     * Returns the tire strategy.
     *
     * @return the plan
     */
    public PitPlan getPlan() {
        return plan;
    }

    /**
     * Returns how many different dry compounds this car has raced on.
     *
     * @return count of dry compounds used
     */
    public int dryCompoundsUsed() {
        int count = 0;
        for (Compound c : usedCompounds) {
            if (c.isDry()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns whether this car ever raced on rain tires.
     *
     * @return true if an Intermediate or Wet was fitted
     */
    public boolean usedRainTires() {
        return usedCompounds.contains(Compound.INTERMEDIATE) || usedCompounds.contains(Compound.WET);
    }

    /**
     * Retires the car: it pulls off the track to one side and stops.
     *
     * @param side +1 to pull off to the left, -1 to the right
     */
    public void retire(int side) {
        retired = true;
        pitRequest = 0;
        setLateralTarget(side * Tuning.OFF_TRACK_OFFSET, Tuning.PIT_LATERAL_SPEED);
    }

    /**
     * Returns whether the car has retired.
     *
     * @return true after a retirement
     */
    public boolean isRetired() {
        return retired;
    }

    /**
     * Returns whether the car is off the racing line for good reason: in the pit lane or retired.
     * Such a car may be alongside a racing car once it is far enough to the side.
     *
     * @return true in the pit lane or when retired
     */
    public boolean isOffTrack() {
        return retired || pitPhase != PitPhase.NONE;
    }

    /**
     * Disqualifies the car.
     */
    public void disqualify() {
        disqualified = true;
    }

    /**
     * Returns whether the car was disqualified.
     *
     * @return true if disqualified
     */
    public boolean isDisqualified() {
        return disqualified;
    }

    /**
     * Adds time to lose, from an incident or a defensive line.
     *
     * @param seconds seconds to lose
     */
    public void addTimePenalty(double seconds) {
        timePenalty += seconds;
    }

    /**
     * Returns the other car in a pass under way.
     *
     * @return the partner, or null if not passing
     */
    public Car getPassPartner() {
        return passPartner;
    }

    /**
     * Returns whether this car is the one doing the passing.
     *
     * @return true for the attacker in a pass
     */
    public boolean isPassAttacker() {
        return passAttacker;
    }

    /**
     * Sets this race's aggression.
     *
     * @param value aggression from 0 to 1
     */
    public void setAggression(double value) {
        aggression = value;
    }

    /**
     * Returns this race's aggression.
     *
     * @return aggression from 0 to 1
     */
    public double getAggression() {
        return aggression;
    }

    /**
     * Sets the stance planned for a coming corner.
     *
     * @param stance the planned stance
     * @param cornerKey lap times 1000 plus corner number
     */
    public void setIntent(Stance stance, int cornerKey) {
        intent = stance;
        intentKey = cornerKey;
    }

    /**
     * Returns the planned stance.
     *
     * @return the stance
     */
    public Stance getIntent() {
        return intent;
    }

    /**
     * Returns which corner the planned stance is for.
     *
     * @return lap times 1000 plus corner number, or -1
     */
    public int getIntentKey() {
        return intentKey;
    }

    /**
     * Records the corner this car last battled at as attacker.
     *
     * @param cornerKey lap times 1000 plus corner number
     */
    public void setLastBattleKey(int cornerKey) {
        lastBattleKey = cornerKey;
    }

    /**
     * Returns the corner this car last battled at as attacker.
     *
     * @return lap times 1000 plus corner number, or -1
     */
    public int getLastBattleKey() {
        return lastBattleKey;
    }

    /**
     * Returns the grip this car had in the last step (tires times brake fade).
     *
     * @return grip multiplier
     */
    public double getCurrentGrip() {
        return currentGrip;
    }

    /**
     * Updates tires, brakes and battery after a move.
     *
     * @param dt time step in seconds
     * @param meters distance moved this step
     * @param track the track
     * @param profiles speed profiles, for the track's mean load and to tell straights from corners
     * @param conditions current weather
     * @param wantDeploy whether to spend battery energy this step if on a straight
     */
    public void updateSystems(double dt, double meters, Track track, ProfileSet profiles,
                              TrackConditions conditions, boolean wantDeploy) {
        double curvature = track.getSample(track.indexAt(distance)).getCurvature();
        double lateral = speed * speed * Math.abs(curvature);
        double load = CarPhysics.load(lateral, deceleration);
        tire.update(dt, meters, load, profiles.getMeanLoad(), track.getLapLength(), pushLevel,
                track.getProfile().getTrackWearMultiplier(), conditions.tireAmbient(),
                conditions.wetnessAt(distance));
        brakes.update(dt, speed, deceleration, pushLevel.getHeat(), conditions.getAirTemp());
        ers.update(dt, deceleration, wantDeploy, profiles.isStraight(distance));
    }

    /**
     * Records a completed lap.
     *
     * @param raceTime race time at the line in seconds
     */
    public void completeLap(double raceTime) {
        lastLapTime = raceTime - lapStartTime;
        if (bestLapTime == 0 || lastLapTime < bestLapTime) {
            bestLapTime = lastLapTime;
        }
        lapsCompleted++;
        lapStartTime = raceTime;
    }

    /**
     * Records the chequered flag.
     *
     * @param raceTime race time at the line in seconds
     * @param position finishing position
     */
    public void finish(double raceTime, int position) {
        finished = true;
        finishTime = raceTime;
        finishPosition = position;
    }

    /**
     * Forces the speed down, used when the car is boxed in behind another car.
     *
     * @param limit highest allowed speed in m/s
     */
    public void capSpeed(double limit) {
        speed = Math.min(speed, Math.max(0, limit));
    }

    /**
     * Fits a fresh set of tires.
     *
     * @param compound the new compound
     */
    public void fitTires(Compound compound) {
        tire = new Tire(compound);
        usedCompounds.add(compound);
    }

    /**
     * Sets how hard the driver pushes.
     *
     * @param level the push level
     */
    public void setPushLevel(PushLevel level) {
        pushLevel = level;
    }

    /**
     * Sets whether the driver wants to deploy energy on straights.
     *
     * @param wanted true to deploy
     */
    public void setErsWanted(boolean wanted) {
        ersWanted = wanted;
    }

    /**
     * Returns whether the driver wants to deploy energy.
     *
     * @return true if deploy is switched on
     */
    public boolean isErsWanted() {
        return ersWanted;
    }

    /**
     * Returns whether this is the player's car.
     *
     * @return true for the player's car
     */
    public boolean isPlayer() {
        return player;
    }

    /**
     * Returns the fitted tires.
     *
     * @return the tire set
     */
    public Tire getTire() {
        return tire;
    }

    /**
     * Returns the brakes.
     *
     * @return the brakes
     */
    public Brakes getBrakes() {
        return brakes;
    }

    /**
     * Returns the battery.
     *
     * @return the ERS
     */
    public Ers getErs() {
        return ers;
    }

    /**
     * Returns the push level.
     *
     * @return the push level
     */
    public PushLevel getPushLevel() {
        return pushLevel;
    }

    /**
     * Returns this race's top speed before push level and ERS.
     *
     * @return top speed in m/s
     */
    public double getTopSpeed() {
        return Tuning.TOP_SPEED * paceFactor;
    }

    /**
     * Returns the row index used in the race tables.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the driver.
     *
     * @return the driver
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * Returns the starting position.
     *
     * @return grid position, 1 for pole
     */
    public int getGridPosition() {
        return gridPosition;
    }

    /**
     * Returns this race's speed multiplier.
     *
     * @return the pace factor
     */
    public double getPaceFactor() {
        return paceFactor;
    }

    /**
     * Returns total distance driven.
     *
     * @return meters since the start/finish line, negative on the grid
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Returns the current speed.
     *
     * @return speed in m/s
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Returns the braking deceleration during the last step.
     *
     * @return m/s squared, 0 if not braking
     */
    public double getDeceleration() {
        return deceleration;
    }

    /**
     * Returns the sideways offset from the centerline.
     *
     * @return offset in meters, positive to the left
     */
    public double getLateralOffset() {
        return lateralOffset;
    }

    /**
     * Returns the number of laps completed.
     *
     * @return completed laps
     */
    public int getLapsCompleted() {
        return lapsCompleted;
    }

    /**
     * Returns the last completed lap time.
     *
     * @return seconds, or 0 if no lap is complete
     */
    public double getLastLapTime() {
        return lastLapTime;
    }

    /**
     * Returns the best lap time.
     *
     * @return seconds, or 0 if no lap is complete
     */
    public double getBestLapTime() {
        return bestLapTime;
    }

    /**
     * Returns whether the car has finished.
     *
     * @return true after the chequered flag
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Returns the finishing time.
     *
     * @return race time at the flag in seconds, or 0 if still racing
     */
    public double getFinishTime() {
        return finishTime;
    }

    /**
     * Returns the finishing position.
     *
     * @return 1 for the winner, 0 while still racing
     */
    public int getFinishPosition() {
        return finishPosition;
    }
}
