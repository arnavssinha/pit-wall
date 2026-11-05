import java.util.Random;

/**
 * Runs pit stops for one race.
 * The pit lane runs beside the main straight from the pit entry to the pit exit, offset to one side
 * of the track. A car that has asked to stop turns in at the entry, drives at the pit speed limit to
 * its box at the start/finish line, waits while the crew fits new tires, drives to the exit, and
 * merges back onto the racing line only when no car is alongside.
 * The time a stop costs is not a fixed number: it comes from the lane's length and speed limit plus
 * the crew's (random) time, so undercuts and overcuts happen naturally.
 */
public final class PitLane {

    /** The race. */
    private final Race race;

    /** The race's random generator, for crew times. */
    private final Random random;

    /** Distance along the lap of the pit entry, in meters. */
    private final double entry;

    /** Distance along the lap of the pit exit, in meters. */
    private final double exit;

    /** Distance along the lap of the pit boxes, in meters (the start/finish line). */
    private final double box;

    /** Sideways position of the pit lane in meters (sign gives the side of the track). */
    private final double laneOffset;

    /**
     * Sets up the pit lane from the track profile.
     *
     * @param race the race
     * @param random the race's random generator
     */
    public PitLane(Race race, Random random) {
        this.race = race;
        this.random = random;
        TrackProfile profile = race.getTrack().getProfile();
        this.entry = profile.getPitEntryDistance();
        this.exit = profile.getPitExitDistance();
        this.box = 0.0;
        this.laneOffset = profile.getPitSide() * Tuning.PIT_LANE_OFFSET;
    }

    /**
     * Runs before cars move: lets strategies call stops and decides whether merging cars may move
     * back onto the racing line yet.
     */
    public void beforeMove() {
        for (Car car : race.getCars()) {
            if (car.isFinished() || car.isRetired()) {
                continue;
            }
            PitStrategyPlanner.callStops(car, race);
            if (car.getPitPhase() == PitPhase.MERGING) {
                double target = canMerge(car) ? 0 : car.getLateralOffset();
                car.setLateralTarget(target, Tuning.PIT_LATERAL_SPEED);
            }
        }
    }

    /**
     * Returns the highest speed a car may drive at because of the pit lane.
     *
     * @param car the car
     * @return speed limit in m/s (no limit outside the lane)
     */
    public double speedLimit(Car car) {
        switch (car.getPitPhase()) {
            case IN_LANE:
            case LEAVING:
                return Tuning.PIT_SPEED_LIMIT;
            case STOPPED:
                return 0;
            default:
                return Double.MAX_VALUE;
        }
    }

    /**
     * Runs after a car moves: moves it through the pit phases when it crosses the entry, the box or
     * the exit, and runs the crew's work while it is stopped.
     *
     * @param car the car
     * @param before distance before the move
     * @param after distance after the move
     * @param dt time step in seconds
     */
    public void afterMove(Car car, double before, double after, double dt) {
        switch (car.getPitPhase()) {
            case NONE:
                if (car.getPitRequest() > 0 && !car.isFinished() && crossed(before, after, entry)) {
                    if (car.getPitRequest() == 1) {
                        car.setPitPhase(PitPhase.IN_LANE);
                        car.setLateralTarget(laneOffset, Tuning.PIT_LATERAL_SPEED);
                    } else {
                        car.skipPitEntry();
                    }
                }
                break;
            case IN_LANE:
                if (crossed(before, after, box)) {
                    car.setPitPhase(PitPhase.STOPPED);
                    car.startCrew(Math.max(Tuning.CREW_TIME_MIN,
                            Tuning.CREW_TIME_MEAN + random.nextGaussian() * Tuning.CREW_TIME_SPREAD));
                }
                break;
            case STOPPED:
                if (car.crewWork(dt)) {
                    finishStop(car);
                }
                break;
            case LEAVING:
                if (crossed(before, after, exit)) {
                    car.setPitPhase(PitPhase.MERGING);
                }
                break;
            default:
                if (Math.abs(car.getLateralOffset()) < 0.01) {
                    car.setPitPhase(PitPhase.NONE);
                }
                break;
        }
    }

    /**
     * Fits the new tires, ticks off the planned stop if this was it, and sends the car on its way.
     *
     * @param car the car
     */
    private void finishStop(Car car) {
        Compound fitted = car.getRequestedCompound() != null ? car.getRequestedCompound() : car.getTire().getCompound();
        car.fitTires(fitted);
        car.countStop();
        PitStop planned = car.getPlan().nextStop();
        if (planned != null && planned.getLap() <= car.getLapsCompleted() + 1) {
            car.getPlan().removeNextStop();
        }
        car.setPitPhase(PitPhase.LEAVING);
        if (car.isPlayer()) {
            race.addMessage("Stop done: " + fitted.getDisplayName() + "s fitted. Go, go, go!", true);
        }
    }

    /**
     * Returns whether a merging car can move back to the racing line. A car on track must be at least
     * the minimum gap away, plus the distance it could close during a whole merge (a fast car coming
     * from behind closes quickly on a car just leaving the pits).
     *
     * @param car the merging car
     * @return true if the way is clear
     */
    private boolean canMerge(Car car) {
        double mergeSeconds = Math.abs(car.getLateralOffset()) / Tuning.PIT_LATERAL_SPEED + Tuning.MERGE_MARGIN_SECONDS;
        for (Car other : race.getCars()) {
            if (other == car || other.isInPitLane() || other.isRetired()) {
                continue;
            }
            double behindNeeded = Tuning.MIN_SEPARATION + Math.max(0, other.getSpeed() - car.getSpeed()) * mergeSeconds;
            double aheadNeeded = Tuning.MIN_SEPARATION + Math.max(0, car.getSpeed() - other.getSpeed()) * mergeSeconds;
            if (race.trackGap(other, car) < behindNeeded || race.trackGap(car, other) < aheadNeeded) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether a move from one distance to another passed a point on the lap.
     *
     * @param before total distance before the move
     * @param after total distance after the move
     * @param point distance along the lap of the point
     * @return true if the point was crossed
     */
    private boolean crossed(double before, double after, double point) {
        double lap = race.getTrack().getLapLength();
        return Math.floor((before - point) / lap) < Math.floor((after - point) / lap);
    }
}
