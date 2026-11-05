import java.util.ArrayList;
import java.util.List;

/**
 * A corner with a braking zone, where battles happen.
 * Corners are found from the track itself: a run of bending samples is a corner, its apex is the
 * sharpest sample, and its braking point is where the speed profile starts to fall before it.
 * Only corners where cars brake noticeably count, because overtakes happen under braking.
 */
public final class Corner {

    /** Corner number in lap order, starting at 1. */
    private final int number;

    /** Distance along the lap where braking starts, in meters. */
    private final double brakingPoint;

    /** Distance along the lap of the apex, in meters. */
    private final double apex;

    /** Signed curvature at the apex: positive for left-hand corners. */
    private final double apexCurvature;

    /**
     * How hard it is to pass here, from 0 (easy) to 1 (hard). Big braking zones are easy;
     * corners taken with little braking are hard.
     */
    private final double difficulty;

    /**
     * Creates a corner.
     *
     * @param number corner number
     * @param brakingPoint distance of the braking point in meters
     * @param apex distance of the apex in meters
     * @param apexCurvature signed curvature at the apex
     * @param difficulty passing difficulty from 0 to 1
     */
    public Corner(int number, double brakingPoint, double apex, double apexCurvature, double difficulty) {
        this.number = number;
        this.brakingPoint = brakingPoint;
        this.apex = apex;
        this.apexCurvature = apexCurvature;
        this.difficulty = difficulty;
    }

    /**
     * Finds every corner with a real braking zone.
     *
     * @param track the track
     * @param profile the reference speed profile
     * @return corners in lap order
     */
    public static List<Corner> findAll(Track track, SpeedProfile profile) {
        List<Corner> corners = new ArrayList<Corner>();
        int n = track.getSampleCount();
        int first = 0;
        while (first < n && isBend(track, first)) {
            first++;
        }
        int end = first + n;
        int i = first;
        while (i < end) {
            if (!isBend(track, i)) {
                i++;
                continue;
            }
            int apexIndex = i;
            while (i < end && isBend(track, i)) {
                if (Math.abs(track.getSample(i).getCurvature()) > Math.abs(track.getSample(apexIndex).getCurvature())) {
                    apexIndex = i;
                }
                i++;
            }
            int brake = apexIndex;
            while (speedAtSample(track, profile, brake - 1) > speedAtSample(track, profile, brake) + 1e-6) {
                brake--;
            }
            double drop = speedAtSample(track, profile, brake) - speedAtSample(track, profile, apexIndex);
            if (drop >= Tuning.CORNER_MIN_SPEED_DROP) {
                double difficulty = Math.max(0, Math.min(1, 1.0 - drop / Tuning.CORNER_EASY_SPEED_DROP));
                TrackSample apex = track.getSample(apexIndex);
                corners.add(new Corner(corners.size() + 1, track.getSample(brake).getDistance(),
                        apex.getDistance(), apex.getCurvature(), difficulty));
            }
        }
        return corners;
    }

    /**
     * Returns the profile speed at one sample.
     *
     * @param track the track
     * @param profile the speed profile
     * @param index sample index (wrapped)
     * @return speed in m/s
     */
    private static double speedAtSample(Track track, SpeedProfile profile, int index) {
        return profile.speedAt(track.getSample(index).getDistance());
    }

    /**
     * Returns whether a sample is part of a bend.
     *
     * @param track the track
     * @param index sample index (wrapped)
     * @return true if the curvature is above the corner threshold
     */
    private static boolean isBend(Track track, int index) {
        return Math.abs(track.getSample(index).getCurvature()) >= Tuning.CORNER_CURVATURE;
    }

    /**
     * Returns the corner number.
     *
     * @return number, starting at 1
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns where braking starts.
     *
     * @return distance along the lap in meters
     */
    public double getBrakingPoint() {
        return brakingPoint;
    }

    /**
     * Returns the apex position.
     *
     * @return distance along the lap in meters
     */
    public double getApex() {
        return apex;
    }

    /**
     * Returns which way the corner turns, as a sideways direction: +1 for left, -1 for right.
     * The inside of the corner is on this side.
     *
     * @return +1 or -1
     */
    public int getInsideSide() {
        return apexCurvature > 0 ? 1 : -1;
    }

    /**
     * Returns the passing difficulty.
     *
     * @return 0 (easy) to 1 (hard)
     */
    public double getDifficulty() {
        return difficulty;
    }
}
