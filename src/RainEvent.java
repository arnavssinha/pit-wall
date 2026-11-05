/**
 * A spell of rain: when it starts, how long it lasts and how hard it rains at its peak.
 * Rain builds up over a short ramp, stays at its peak, then eases off over another ramp.
 */
public final class RainEvent {

    /** Race time when rain starts, in seconds. */
    private final double start;

    /** How long it rains, in seconds. */
    private final double duration;

    /** Peak intensity from 0 to 1 (1 is a downpour). */
    private final double peak;

    /**
     * Creates a rain spell.
     *
     * @param start race time when it starts, in seconds
     * @param duration how long it lasts, in seconds
     * @param peak peak intensity from 0 to 1
     */
    public RainEvent(double start, double duration, double peak) {
        this.start = start;
        this.duration = duration;
        this.peak = peak;
    }

    /**
     * Returns how hard it is raining at a moment.
     *
     * @param time race time in seconds
     * @return intensity from 0 to 1
     */
    public double intensityAt(double time) {
        double t = time - start;
        if (t <= 0 || t >= duration) {
            return 0;
        }
        double ramp = Math.min(Tuning.RAIN_RAMP_SECONDS, duration / 2);
        double fraction = Math.min(1.0, Math.min(t / ramp, (duration - t) / ramp));
        return peak * fraction;
    }

    /**
     * Returns when the rain starts.
     *
     * @return race time in seconds
     */
    public double getStart() {
        return start;
    }

    /**
     * Returns when the rain stops.
     *
     * @return race time in seconds
     */
    public double getEnd() {
        return start + duration;
    }

    /**
     * Returns the peak intensity.
     *
     * @return intensity from 0 to 1
     */
    public double getPeak() {
        return peak;
    }
}
