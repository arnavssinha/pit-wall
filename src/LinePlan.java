/**
 * The exact path and speed effects of one car's line choice through one corner. The car drives this
 * path, and the screen draws the same path as a preview, so what the player sees is what happens.
 * Along the lap the plan has four parts: follow the racing line (in the tow); move across to the chosen
 * lane, finishing just before the braking point; hold the lane to the apex; then blend back to the
 * racing line on the exit. Distances here are absolute (counted from the start of the race), like
 * Car.getDistance.
 */
public final class LinePlan {

    /** The chosen line. */
    private final LineChoice choice;

    /** The corner the plan is for. */
    private final Corner corner;

    /** True if the car brakes later than normal into the corner. */
    private final boolean lateBrake;

    /** True if this car is the attacker (the one behind). */
    private final boolean attacker;

    /** Where the move across starts. */
    private final double moveStart;

    /** Where the car is fully in its lane. */
    private final double moveEnd;

    /** The braking point of the corner. */
    private final double brakeAt;

    /** The apex of the corner. */
    private final double apexAt;

    /** Where the plan ends, back on the racing line. */
    private final double endAt;

    /** Sideways position of the lane, in meters from the centerline (positive left). */
    private final double lane;

    /** True once the car has locked up its brakes in this corner. */
    private boolean lockedUp;

    /**
     * Makes a plan.
     *
     * @param choice the chosen line
     * @param corner the corner
     * @param lateBrake true to brake later than normal
     * @param attacker true for the car behind, false for the car ahead
     * @param distance the car's distance now (absolute)
     * @param early true to move across straight away (an attacker with a speed edge pulling out of the
     *              tow to drag past on the straight) instead of just before the braking point
     * @param track the track
     */
    public LinePlan(LineChoice choice, Corner corner, boolean lateBrake, boolean attacker, double distance,
                    boolean early, Track track) {
        this.choice = choice;
        this.corner = corner;
        this.lateBrake = lateBrake;
        this.attacker = attacker;
        this.apexAt = distance + track.wrap(corner.getApex() - distance);
        this.brakeAt = apexAt - track.wrap(corner.getApex() - corner.getBrakingPoint());
        double lead = attacker ? Tuning.LANE_SETTLE_BEFORE_BRAKING : Tuning.COVER_SETTLE_BEFORE_BRAKING;
        double latestEnd = Math.max(distance + 1, brakeAt - lead);
        this.moveEnd = early ? Math.min(latestEnd, distance + Tuning.LANE_CHANGE_METERS) : latestEnd;
        this.moveStart = Math.max(distance, moveEnd - Tuning.LANE_CHANGE_METERS);
        this.endAt = apexAt + Tuning.CORNER_EXIT_METERS;
        int inside = corner.getInsideSide();
        if (choice == LineChoice.INSIDE) {
            lane = inside * Tuning.LANE_OFFSET;
        } else if (choice == LineChoice.OUTSIDE) {
            lane = -inside * Tuning.LANE_OFFSET;
        } else {
            lane = 0;
        }
    }

    /**
     * Returns where the car should be sideways at a distance.
     *
     * @param distance absolute distance along the race
     * @param line the car's normal line here (racing line plus the driver's own variation)
     * @return sideways target in meters from the centerline
     */
    public double lateralAt(double distance, double line) {
        double laneHere = lockedUp ? -corner.getInsideSide() * Tuning.RUN_WIDE_OFFSET : lane;
        if (!choice.leavesLine() && !lockedUp) {
            return line;
        }
        if (distance < moveStart || distance >= endAt) {
            return line;
        }
        if (distance < moveEnd) {
            return blend(line, laneHere, (distance - moveStart) / (moveEnd - moveStart));
        }
        if (distance < apexAt) {
            return laneHere;
        }
        return blend(laneHere, line, (distance - apexAt) / (endAt - apexAt));
    }

    /**
     * Returns how much later than normal the car brakes at a distance: the car aims for the speed of
     * a point this many meters further back, so it stays fast for longer. It tapers to nothing at the apex.
     *
     * @param distance absolute distance along the race
     * @return shift in meters, 0 when not late braking
     */
    public double brakeShift(double distance) {
        if (!lateBrake || lockedUp || distance < brakeAt - Tuning.LATE_BRAKE_METERS || distance >= apexAt) {
            return 0;
        }
        double toApex = (apexAt - distance) / Math.max(1, apexAt - brakeAt + Tuning.LATE_BRAKE_METERS);
        return Tuning.LATE_BRAKE_METERS * Math.min(1, toApex * 2);
    }

    /**
     * Returns the speed multiplier for this line at a distance: the outside is the long way in, the
     * inside is a tight, slow exit, and a lock-up costs a lot of speed until the exit.
     *
     * @param distance absolute distance along the race
     * @return multiplier, 1 when the line costs nothing
     */
    public double speedFactor(double distance) {
        if (lockedUp && distance >= brakeAt && distance < endAt) {
            return 1 - Tuning.LOCKUP_SPEED_LOSS;
        }
        if (choice == LineChoice.OUTSIDE && distance >= brakeAt && distance < apexAt) {
            return 1 - Tuning.OUTSIDE_ENTRY_LOSS;
        }
        if (choice == LineChoice.INSIDE && distance >= apexAt && distance < endAt) {
            return 1 - Tuning.INSIDE_EXIT_LOSS;
        }
        return 1;
    }

    /**
     * Records a lock-up: the car runs wide and loses speed until the exit.
     */
    public void lockUp() {
        lockedUp = true;
    }

    /**
     * Returns whether the car locked up in this corner.
     *
     * @return true after a lock-up
     */
    public boolean isLockedUp() {
        return lockedUp;
    }

    /**
     * Returns whether the plan is over (the car is back on the racing line after the exit).
     *
     * @param distance absolute distance along the race
     * @return true once past the end
     */
    public boolean isDone(double distance) {
        return distance >= endAt;
    }

    /**
     * Returns the chosen line.
     *
     * @return the choice
     */
    public LineChoice getChoice() {
        return choice;
    }

    /**
     * Returns the corner.
     *
     * @return the corner
     */
    public Corner getCorner() {
        return corner;
    }

    /**
     * Returns whether the car brakes late.
     *
     * @return true for late braking
     */
    public boolean isLateBrake() {
        return lateBrake;
    }

    /**
     * Returns whether this is the attacking car's plan.
     *
     * @return true for the attacker
     */
    public boolean isAttacker() {
        return attacker;
    }

    /**
     * Returns the braking point.
     *
     * @return absolute distance
     */
    public double getBrakeAt() {
        return brakeAt;
    }

    /**
     * Returns the apex.
     *
     * @return absolute distance
     */
    public double getApexAt() {
        return apexAt;
    }

    /**
     * Returns where the plan ends.
     *
     * @return absolute distance
     */
    public double getEndAt() {
        return endAt;
    }

    /**
     * Smooth blend between two values (eases in and out, like a car turning in and straightening).
     *
     * @param from start value
     * @param to end value
     * @param t progress from 0 to 1
     * @return blended value
     */
    private static double blend(double from, double to, double t) {
        double c = Math.max(0, Math.min(1, t));
        double s = c * c * (3 - 2 * c);
        return from + (to - from) * s;
    }
}
