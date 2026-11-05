/**
 * The fastest safe speed at every sample of a track, including braking ahead of corners.
 * It is built in two passes:
 * first, each sample gets its corner speed limit from its curvature;
 * second, walking backwards around the lap, each sample is lowered so a car can still brake
 * in time for the slower samples ahead. A car that follows this profile brakes before corners
 * and never arrives too fast.
 */
public final class SpeedProfile {

    /** The track this profile belongs to. */
    private final Track track;

    /** Grip multiplier the profile was built with. */
    private final double gripScale;

    /** Corner speed limit at each sample in m/s, ignoring braking. */
    private final double[] cornerLimit;

    /** Final target speed at each sample in m/s, including braking for what comes next. */
    private final double[] target;

    /**
     * Builds the profile for a track.
     *
     * @param track the track
     * @param gripScale grip multiplier from calibration
     * @param topSpeed straight-line top speed in m/s
     */
    public SpeedProfile(Track track, double gripScale, double topSpeed) {
        this.track = track;
        this.gripScale = gripScale;
        int n = track.getSampleCount();
        cornerLimit = new double[n];
        target = new double[n];
        for (int i = 0; i < n; i++) {
            cornerLimit[i] = CarPhysics.cornerSpeedLimit(track.getSample(i).getCurvature(), gripScale, topSpeed);
            target[i] = cornerLimit[i];
        }
        applyBraking(track.getSpacing());
    }

    /**
     * Walks backwards around the lap twice (so the wrap from the last sample to the first is
     * covered) and caps each sample at the speed from which the car can still slow to the next one.
     * From v squared = u squared + 2 a d: the fastest entry speed is sqrt(next squared + 2 a d).
     *
     * @param spacing sample spacing in meters
     */
    private void applyBraking(double spacing) {
        int n = target.length;
        for (int k = 2 * n - 1; k >= 0; k--) {
            int i = k % n;
            double next = target[(i + 1) % n];
            double decel = CarPhysics.braking(next, gripScale) * Tuning.BRAKE_PLANNING_MARGIN;
            double reachable = Math.sqrt(next * next + 2 * decel * spacing);
            if (reachable < target[i]) {
                target[i] = reachable;
            }
        }
    }

    /**
     * Returns the target speed at a point on the lap, blended between samples.
     *
     * @param distance distance along the lap in meters
     * @return target speed in m/s
     */
    public double speedAt(double distance) {
        double d = track.wrap(distance);
        int i = track.indexAt(d);
        double t = (d - track.getSample(i).getDistance()) / track.getSpacing();
        double a = target[i];
        double b = target[(i + 1) % target.length];
        return a + (b - a) * t;
    }

    /**
     * Returns the corner speed limit at one sample (no braking applied).
     *
     * @param index sample index
     * @return speed limit in m/s
     */
    public double cornerLimitAt(int index) {
        return cornerLimit[Math.floorMod(index, cornerLimit.length)];
    }

    /**
     * Returns the grip multiplier this profile was built with.
     *
     * @return grip scale
     */
    public double getGripScale() {
        return gripScale;
    }
}
