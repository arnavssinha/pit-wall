import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Runs pit stops for one race.
 * The pit lane runs beside the main straight from the pit entry to the pit exit, offset to one side
 * of the track. A car that has asked to stop turns in at the entry, drives at the pit speed limit to
 * its team's box (the boxes sit side by side along the lane, around the start/finish line), pulls
 * sideways out of the fast lane into the box so cars behind can drive past, waits while the crew
 * fits new tires (a teammate arriving meanwhile queues behind, as in a real double stack), drives to
 * the exit, runs along the track edge, and moves across to the racing line only when no car is close.
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

    /** Race time each car leaving the pits started looking for a gap to rejoin the racing line. */
    private final HashMap<Car, Double> mergingSince = new HashMap<Car, Double>();

    /** Cars that have found their gap and are moving across to the racing line. */
    private final HashSet<Car> rejoining = new HashSet<Car>();

    /** The teams in pit-lane order, one box each. */
    private final ArrayList<Team> teams = new ArrayList<Team>();

    /** Each team's box position, in meters along the lap. */
    private final HashMap<Team, Double> boxByTeam = new HashMap<Team, Double>();


    /** Sideways position of the pit lane in meters (sign gives the side of the track). */
    private final double laneOffset;

    /** Sideways position of a car parked in its box, just outside the fast lane, in meters. */
    private final double boxOffset;

    /**
     * Sideways position at the track edge, on the pit side, where a car leaving the pits runs until
     * there is room to move across to the racing line, in meters.
     */
    private final double exitEdgeOffset;

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
        this.laneOffset = profile.getPitSide() * Tuning.PIT_LANE_OFFSET;
        this.boxOffset = laneOffset + profile.getPitSide() * Tuning.PIT_BOX_SHIFT;
        this.exitEdgeOffset = profile.getPitSide() * (Tuning.TRACK_WIDTH / 2 - Tuning.RACING_LINE_EDGE_MARGIN);
        for (Car car : race.getCars()) {
            Team team = car.getDriver().getTeam();
            if (!teams.contains(team)) {
                teams.add(team);
            }
        }
        double first = Tuning.PIT_BOXES_CENTER - Tuning.PIT_BOX_SPACING * (teams.size() - 1) / 2.0;
        for (int i = 0; i < teams.size(); i++) {
            boxByTeam.put(teams.get(i), race.getTrack().wrap(first + i * Tuning.PIT_BOX_SPACING));
        }
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
            if (car.getPitPhase() == PitPhase.MERGING && !rejoining.contains(car)) {
                Double since = mergingSince.get(car);
                if (since == null) {
                    since = race.getTime();
                    mergingSince.put(car, since);
                }
                boolean waitedLong = race.getTime() - since > Tuning.MERGE_PATIENCE_SECONDS;
                if (canMerge(car, waitedLong)) {
                    mergingSince.remove(car);
                    rejoining.add(car);
                    car.returnToLine(Tuning.PIT_LATERAL_SPEED);
                } else {
                    car.setLateralTarget(exitEdgeOffset, Tuning.PIT_LATERAL_SPEED);
                }
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
                double box = boxOf(car);
                if (race.getTrack().wrap(box - after) < Tuning.PIT_BOX_TURN_IN) {
                    car.setLateralTarget(boxOffset, Tuning.PIT_LATERAL_SPEED);
                }
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
                if (car.isOnLine() && !race.hasCarAlongside(car, false)) {
                    rejoining.remove(car);
                    car.setPitPhase(PitPhase.NONE);
                }
                break;
        }
    }

    /**
     * Returns where a car's team box is.
     *
     * @param car the car
     * @return the box position in meters along the lap
     */
    public double boxOf(Car car) {
        return boxByTeam.get(car.getDriver().getTeam());
    }

    /**
     * Returns the teams in the order of their boxes along the pit lane.
     *
     * @return the teams, read-only
     */
    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    /**
     * Returns a team's box position.
     *
     * @param team the team
     * @return the box position in meters along the lap
     */
    public double boxOf(Team team) {
        return boxByTeam.get(team);
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
        car.setLateralTarget(laneOffset, Tuning.PIT_LATERAL_SPEED);
        if (car.isPlayer()) {
            race.addMessage("Stop done: " + fitted.getDisplayName() + "s fitted. Go, go, go!", true);
        }
    }

    /**
     * Returns whether a merging car can move back to the racing line. A car on track must be at least
     * the minimum gap away, plus the distance it could close during a whole merge (a fast car coming
     * from behind closes quickly on a car just leaving the pits). A car that has already waited
     * Tuning.MERGE_PATIENCE_SECONDS only needs nobody alongside, as a driver would squeeze in.
     *
     * @param car the merging car
     * @param waitedLong true once the car has waited a while for a gap
     * @return true if the way is clear
     */
    private boolean canMerge(Car car, boolean waitedLong) {
        double across = Math.abs(car.getLateralOffset() - race.getTrack().lineAt(car.getDistance()));
        double sideways = Math.min(Tuning.PIT_LATERAL_SPEED, Tuning.MAX_LATERAL_RATE * Math.max(1, car.getSpeed()));
        double mergeSeconds = across / sideways + Tuning.MERGE_MARGIN_SECONDS;
        for (Car other : race.getCars()) {
            if (other == car || other.isInPitLane() || other.isRetired()) {
                continue;
            }
            double closingWeight = waitedLong ? 0 : mergeSeconds;
            double clear = Tuning.MIN_SEPARATION - Tuning.MERGE_GAP_TOLERANCE;
            double behindNeeded = clear + Math.max(0, other.getSpeed() - car.getSpeed()) * closingWeight;
            double aheadNeeded = clear + Math.max(0, car.getSpeed() - other.getSpeed()) * closingWeight;
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
