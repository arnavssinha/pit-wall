/**
 * How hard a driver is pushing: the main lever the player pulls every lap.
 * Pushing is a little faster but wears tires much faster and runs hotter, so pushing all race
 * loses (brief, Section 3.5). The wear cost grows with the square of the intensity.
 */
public enum PushLevel {

    /** Save tires: about 2.5 percent slower, tires wear about 45 percent slower and run much cooler. */
    CONSERVE("Conserve", 0.975, 0.74, 0.85),

    /** The normal race pace everything else is tuned around. */
    BALANCED("Balanced", 1.000, 1.00, 1.00),

    /** Attack the lap: about 3 percent faster, tires wear about twice as fast and run much hotter. */
    PUSH("Push", 1.030, 1.42, 1.09);

    /** Name shown to the player. */
    private final String displayName;

    /** Multiplier on target speed. Balance choice. */
    private final double pace;

    /** Driving intensity; tire wear scales with its square (brief, Section 3.7). Balance choice. */
    private final double intensity;

    /** Multiplier on how much heat the tires and brakes build up. Balance choice. */
    private final double heat;

    /**
     * Creates a push level.
     *
     * @param displayName name shown to the player
     * @param pace speed multiplier
     * @param intensity driving intensity, squared for wear
     * @param heat heat multiplier
     */
    PushLevel(String displayName, double pace, double intensity, double heat) {
        this.displayName = displayName;
        this.pace = pace;
        this.intensity = intensity;
        this.heat = heat;
    }

    /**
     * Returns the name shown to the player.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the speed multiplier.
     *
     * @return pace multiplier
     */
    public double getPace() {
        return pace;
    }

    /**
     * Returns the wear multiplier, the intensity squared.
     *
     * @return wear multiplier
     */
    public double getWearFactor() {
        return intensity * intensity;
    }

    /**
     * Returns the heat multiplier.
     *
     * @return heat multiplier
     */
    public double getHeat() {
        return heat;
    }
}
