import java.util.HashMap;

/**
 * Speed profiles for one track at many grip levels, built on demand and kept for reuse.
 * Every car's grip changes all the time (tire temperature, wear, brake fade), so each car needs
 * the profile for its current grip. Grip is rounded to steps of Tuning.PROFILE_GRIP_STEP and each
 * level's profile is built only once and then looked up in a HashMap.
 */
public final class ProfileSet {

    /** The track. */
    private final Track track;

    /** Track calibration: multiplies every car's grip so a clean lap matches the real lap time. */
    private final double gripScale;

    /** Profiles already built, keyed by grip divided by the step and rounded. */
    private final HashMap<Integer, SpeedProfile> profiles = new HashMap<Integer, SpeedProfile>();

    /** Average load per meter over a clean reference lap, used to keep heat and wear fair across tracks. */
    private final double meanLoad;

    /**
     * Creates the set for a track.
     *
     * @param track the track
     * @param gripScale the track's calibrated grip scale
     */
    public ProfileSet(Track track, double gripScale) {
        this.track = track;
        this.gripScale = gripScale;
        this.meanLoad = LapCalibrator.meanLoadPerMeter(track, gripScale);
    }

    /**
     * Returns the profile for a car whose tires and brakes give the stated grip multiplier.
     *
     * @param carGrip grip from tires and brakes (about 1 for a fresh Soft)
     * @return the matching profile
     */
    public SpeedProfile forCarGrip(double carGrip) {
        int key = (int) Math.round(carGrip * gripScale / Tuning.PROFILE_GRIP_STEP);
        SpeedProfile profile = profiles.get(key);
        if (profile == null) {
            profile = new SpeedProfile(track, key * Tuning.PROFILE_GRIP_STEP, Tuning.TOP_SPEED);
            profiles.put(key, profile);
        }
        return profile;
    }

    /**
     * Returns the average load per meter over a clean reference lap.
     *
     * @return mean load, 1 or more
     */
    public double getMeanLoad() {
        return meanLoad;
    }

    /**
     * Returns the track's calibrated grip scale.
     *
     * @return grip scale
     */
    public double getGripScale() {
        return gripScale;
    }
}
