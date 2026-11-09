/** The tire compounds: three for the dry and two for rain. */
public enum Compound {

    /** Fastest dry tire, wears quickly. */
    SOFT("Soft", 'S', 1.00, 0.16, false),

    /** Middle dry tire. */
    MEDIUM("Medium", 'M', 0.98, 0.10, false),

    /** Slowest dry tire, lasts longest. */
    HARD("Hard", 'H', 0.96, 0.07, false),

    /** For a damp or drying track. */
    INTER("Intermediate", 'I', 0.93, 0.08, true),

    /** For heavy rain. */
    WET("Wet", 'W', 0.89, 0.07, true);

    /** Name shown on screen. */
    private final String name;

    /** One-letter label. */
    private final char letter;

    /** Grip on a dry track (1.0 = best). */
    private final double grip;

    /** Wear per lap (1.0 = completely worn). */
    private final double wearPerLap;

    /** True for rain tires. */
    private final boolean rain;

    /** Creates a compound. */
    Compound(String name, char letter, double grip, double wearPerLap, boolean rain) {
        this.name = name;
        this.letter = letter;
        this.grip = grip;
        this.wearPerLap = wearPerLap;
        this.rain = rain;
    }

    /**
     * Returns the grip of this tire on a track of a given wetness, before wear. Dry tires lose grip quickly
     * as the track gets wet; rain tires lose grip when it is dry.
     */
    public double grip(double wetness) {
        if (this == INTER) {
            return grip - 0.25 * Math.abs(wetness - 0.5);
        }
        if (this == WET) {
            return grip - 0.25 * (1 - wetness);
        }
        return grip - 0.45 * wetness;
    }

    /** Returns the display name. */
    public String getName() {
        return name;
    }

    /** Returns the one-letter label. */
    public char getLetter() {
        return letter;
    }

    /** Returns the wear per lap. */
    public double getWearPerLap() {
        return wearPerLap;
    }

    /** Returns whether this is a rain tire. */
    public boolean isRain() {
        return rain;
    }
}
