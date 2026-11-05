/**
 * The five tire compounds and what makes each one different.
 * Three are slicks for a dry track (Soft, Medium, Hard) and two are for rain (Intermediate, Wet).
 * Every number is a balance choice for a 10-lap sprint, documented in the constructor below.
 */
public enum Compound {

    /** Fastest dry tire, shortest life, likes lower temperatures. */
    SOFT("Soft", "S", 1.000, 80, 107, 5.5, 0.0),

    /** The middle dry tire. */
    MEDIUM("Medium", "M", 0.985, 90, 112, 7.5, 0.0),

    /** Slowest dry tire, longest life, needs more heat to work. */
    HARD("Hard", "H", 0.970, 100, 122, 11.0, 0.0),

    /** Grooved tire for a damp or drying track. */
    INTERMEDIATE("Intermediate", "I", 0.930, 60, 85, 6.0, 0.6),

    /** Deep-grooved tire for heavy rain. */
    WET("Wet", "W", 0.900, 45, 70, 5.0, 1.0);

    /** Name shown to the player. */
    private final String displayName;

    /** One-letter label for the timing tower. */
    private final String letter;

    /** Grip on a dry track, fresh and at the right temperature, relative to a fresh Soft. */
    private final double dryGrip;

    /** Bottom of the ideal temperature window in degrees Celsius. */
    private final double windowLow;

    /** Top of the ideal temperature window in degrees Celsius. */
    private final double windowHigh;

    /**
     * Laps until fully worn at Balanced push on a track with wear multiplier 1.0, with the tire in
     * its temperature window. Sprint scaling: real tires last 15 to 30 laps, but this race is 10 laps,
     * so lives are shortened so that stopping is worth thinking about (brief, Section 3.7).
     */
    private final double lifeLaps;

    /** Best track wetness for this tire, from 0 (dry) to 1 (soaked). */
    private final double idealWetness;

    /**
     * Creates a compound.
     *
     * @param displayName name shown to the player
     * @param letter one-letter label
     * @param dryGrip relative dry grip
     * @param windowLow bottom of the temperature window in Celsius
     * @param windowHigh top of the temperature window in Celsius
     * @param lifeLaps laps of life at Balanced push on a neutral track
     * @param idealWetness best track wetness, 0 to 1
     */
    Compound(String displayName, String letter, double dryGrip, double windowLow, double windowHigh,
             double lifeLaps, double idealWetness) {
        this.displayName = displayName;
        this.letter = letter;
        this.dryGrip = dryGrip;
        this.windowLow = windowLow;
        this.windowHigh = windowHigh;
        this.lifeLaps = lifeLaps;
        this.idealWetness = idealWetness;
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
     * Returns the one-letter label.
     *
     * @return S, M, H, I or W
     */
    public String getLetter() {
        return letter;
    }

    /**
     * Returns the relative dry grip of a fresh tire in its window.
     *
     * @return grip relative to a fresh Soft
     */
    public double getDryGrip() {
        return dryGrip;
    }

    /**
     * Returns the bottom of the temperature window.
     *
     * @return degrees Celsius
     */
    public double getWindowLow() {
        return windowLow;
    }

    /**
     * Returns the top of the temperature window.
     *
     * @return degrees Celsius
     */
    public double getWindowHigh() {
        return windowHigh;
    }

    /**
     * Returns the life in laps on a neutral track at Balanced push.
     *
     * @return laps
     */
    public double getLifeLaps() {
        return lifeLaps;
    }

    /**
     * Returns the best track wetness for this tire.
     *
     * @return wetness from 0 to 1
     */
    public double getIdealWetness() {
        return idealWetness;
    }

    /**
     * Returns whether this is a slick for dry running.
     *
     * @return true for Soft, Medium and Hard
     */
    public boolean isDry() {
        return idealWetness == 0.0;
    }
}
