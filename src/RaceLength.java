/**
 * How long a race is, chosen on the setup screen. Real-time durations are at the default 4x speed.
 * Tire life scales with the race length so short races still have tire management, and the
 * two-compound rule only applies to races of Tuning.COMPOUND_RULE_MIN_LAPS laps or more.
 */
public enum RaceLength {

    /** One flat-out lap from a standing start. */
    SHOOTOUT("1-lap shootout", 1, "about 25 seconds"),

    /** A three-lap dash. */
    SPRINT("Sprint, 3 laps", 3, "about 1 minute"),

    /** A full race with pit strategy. */
    RACE("Race, 12 laps", 12, "about 5 minutes");

    /** Name shown to the player. */
    private final String displayName;

    /** Number of laps. */
    private final int laps;

    /** Rough real-time length at the default speed. */
    private final String duration;

    /**
     * Creates a race length.
     *
     * @param displayName name shown to the player
     * @param laps number of laps
     * @param duration rough real-time length
     */
    RaceLength(String displayName, int laps, String duration) {
        this.displayName = displayName;
        this.laps = laps;
        this.duration = duration;
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
     * Returns the number of laps.
     *
     * @return laps
     */
    public int getLaps() {
        return laps;
    }

    /**
     * Returns the rough real-time length.
     *
     * @return text such as "about 1 minute"
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Returns how much longer or shorter tires last in a race of a given length than in the
     * reference 10-lap race: tires last proportionally, but never less than a minimum share, so
     * a single lap does not destroy them.
     *
     * @param laps race length in laps
     * @return multiplier on tire life
     */
    public static double tireLifeScale(int laps) {
        return Math.max(Tuning.MIN_TIRE_LIFE_SCALE, laps / (double) Tuning.RACE_LAPS);
    }

    /**
     * Returns whether the two-compound rule applies to a race of a given length.
     *
     * @param laps race length in laps
     * @return true for longer races
     */
    public static boolean compoundRuleApplies(int laps) {
        return Tuning.TWO_COMPOUND_RULE && laps >= Tuning.COMPOUND_RULE_MIN_LAPS;
    }
}
