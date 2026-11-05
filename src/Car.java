/**
 * One car in the race: who drives it, where it is, and how fast it is going.
 * Position is stored as total distance driven since the start/finish line (negative while on
 * the grid). Lap number and position on the lap both come from that one number, which keeps
 * lap counting simple and exact.
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

    /** Total meters driven since the start/finish line; negative on the grid. */
    private double distance;

    /** Current speed in m/s. */
    private double speed;

    /** Sideways offset from the centerline in meters, positive to the left. */
    private double lateralOffset;

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
     * Places a car on its grid slot, stationary.
     *
     * @param id row in the race tables
     * @param driver the driver
     * @param gridPosition starting position, 1 for pole
     * @param paceFactor speed multiplier for this race
     * @param startDistance grid slot as distance from the line (negative, behind it)
     * @param startLateral grid slot sideways offset in meters
     */
    public Car(int id, Driver driver, int gridPosition, double paceFactor, double startDistance, double startLateral) {
        this.id = id;
        this.driver = driver;
        this.gridPosition = gridPosition;
        this.paceFactor = paceFactor;
        this.distance = startDistance;
        this.lateralOffset = startLateral;
        this.speed = 0;
    }

    /**
     * Moves the speed toward a target, accelerating or braking within the car's limits.
     *
     * @param targetSpeed wanted speed in m/s
     * @param gripScale grip multiplier, which limits braking
     * @param dt time step in seconds
     */
    public void approachSpeed(double targetSpeed, double gripScale, double dt) {
        if (speed < targetSpeed) {
            double gain = CarPhysics.acceleration(speed, getTopSpeed()) * dt;
            speed = Math.min(targetSpeed, speed + gain);
        } else {
            double loss = CarPhysics.braking(speed, gripScale) * dt;
            speed = Math.max(targetSpeed, speed - loss);
        }
    }

    /**
     * Moves the car forward along the track and eases it back toward the racing line.
     *
     * @param meters distance to move, never negative
     * @param dt time step in seconds
     */
    public void advance(double meters, double dt) {
        distance += meters;
        double step = Tuning.LATERAL_RETURN_SPEED * dt;
        if (Math.abs(lateralOffset) <= step) {
            lateralOffset = 0;
        } else {
            lateralOffset -= Math.signum(lateralOffset) * step;
        }
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
     * Returns this car's top speed for the race.
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
