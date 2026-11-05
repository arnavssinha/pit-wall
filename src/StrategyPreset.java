/**
 * The three rough strategies from the brief (Section 3.3), each with a suggested compound
 * sequence for a 10-lap sprint. The player may change the compounds on the setup screen.
 * Suggested sequences fit the sprint-scaled tire lives (Soft 5.5, Medium 7.5, Hard 11 laps).
 */
public enum StrategyPreset {

    /** Stop at the end of lap 3: a short sprint on Softs, then Hards to the end. */
    ONE_STOP_EARLY("One-stop early", new int[] {3}, new Compound[] {Compound.SOFT, Compound.HARD}),

    /** Stop at the end of lap 6: a long first stint on Mediums, then Softs to the end. */
    ONE_STOP_LATE("One-stop late", new int[] {6}, new Compound[] {Compound.MEDIUM, Compound.SOFT}),

    /** Stop at the end of laps 3 and 7: three short, fast stints. */
    TWO_STOP("Two-stop", new int[] {3, 7}, new Compound[] {Compound.SOFT, Compound.MEDIUM, Compound.SOFT});

    /** Name shown to the player. */
    private final String displayName;

    /** Stop laps. */
    private final int[] stopLaps;

    /** Compound for each stint: the start tire, then one per stop. */
    private final Compound[] stints;

    /**
     * Creates a preset.
     *
     * @param displayName name shown to the player
     * @param stopLaps laps at the end of which to stop
     * @param stints compounds for each stint
     */
    StrategyPreset(String displayName, int[] stopLaps, Compound[] stints) {
        this.displayName = displayName;
        this.stopLaps = stopLaps;
        this.stints = stints;
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
     * Returns the suggested compound for one stint.
     *
     * @param stint stint number, 0 for the start
     * @return the compound
     */
    public Compound stintCompound(int stint) {
        return stints[stint];
    }

    /**
     * Returns the number of stints (stops plus one).
     *
     * @return stint count
     */
    public int stintCount() {
        return stints.length;
    }

    /**
     * Builds a fresh pit plan with the suggested compounds.
     *
     * @return the plan
     */
    public PitPlan makePlan() {
        PitPlan plan = new PitPlan(stints[0]);
        for (int i = 0; i < stopLaps.length; i++) {
            plan.addStop(new PitStop(stopLaps[i], stints[i + 1]));
        }
        return plan;
    }
}
