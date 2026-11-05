import java.util.ArrayList;
import java.util.List;

/**
 * The rough strategies from the brief (Section 3.3), each with a suggested compound sequence.
 * Stops are placed as fractions of the race (for example 30 percent of the way through), so the same
 * strategy works for any race length. The player may change the compounds on the setup screen.
 */
public enum StrategyPreset {

    /** No stop at all: only for short races, where the two-compound rule does not apply. */
    NO_STOP("No stop", new double[] {}, new Compound[] {Compound.SOFT}),

    /** Stop 30 percent of the way through: a short sprint on Softs, then Hards to the end. */
    ONE_STOP_EARLY("One-stop early", new double[] {0.3}, new Compound[] {Compound.SOFT, Compound.HARD}),

    /** Stop 60 percent of the way through: a long first stint on Mediums, then Softs to the end. */
    ONE_STOP_LATE("One-stop late", new double[] {0.6}, new Compound[] {Compound.MEDIUM, Compound.SOFT}),

    /** Stop at 30 and 70 percent: three short, fast stints. */
    TWO_STOP("Two-stop", new double[] {0.3, 0.7}, new Compound[] {Compound.SOFT, Compound.MEDIUM, Compound.SOFT});

    /** Name shown to the player. */
    private final String displayName;

    /** Where each stop falls, as a fraction of the race. */
    private final double[] stopFractions;

    /** Compound for each stint: the start tire, then one per stop. */
    private final Compound[] stints;

    /**
     * Creates a preset.
     *
     * @param displayName name shown to the player
     * @param stopFractions where each stop falls, as a fraction of the race
     * @param stints compounds for each stint
     */
    StrategyPreset(String displayName, double[] stopFractions, Compound[] stints) {
        this.displayName = displayName;
        this.stopFractions = stopFractions;
        this.stints = stints;
    }

    /**
     * Returns the presets that make sense for a race length: No stop only when the two-compound rule
     * does not apply, stopping strategies only when there are laps to stop on, and Two-stop only when
     * its stops fall on different laps.
     *
     * @param laps race length in laps
     * @return the usable presets, in order
     */
    public static List<StrategyPreset> forRace(int laps) {
        List<StrategyPreset> list = new ArrayList<StrategyPreset>();
        for (StrategyPreset p : values()) {
            boolean usable;
            if (p == NO_STOP) {
                usable = !RaceLength.compoundRuleApplies(laps);
            } else {
                usable = laps >= 2 && p.stopLaps(laps).length == p.stopFractions.length;
            }
            if (usable) {
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Returns the stop laps for a race length: each fraction of the race rounded to a lap, at least
     * lap 1 and at most the second-to-last lap, with duplicates dropped.
     *
     * @param laps race length in laps
     * @return stop laps in order
     */
    public int[] stopLaps(int laps) {
        List<Integer> out = new ArrayList<Integer>();
        for (double f : stopFractions) {
            int lap = Math.max(1, Math.min(laps - 1, (int) Math.round(f * laps)));
            if (lap >= 1 && !out.contains(lap)) {
                out.add(lap);
            }
        }
        int[] result = new int[out.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = out.get(i);
        }
        return result;
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
     * Builds a fresh pit plan with the suggested compounds for a race length.
     *
     * @param laps race length in laps
     * @return the plan
     */
    public PitPlan makePlan(int laps) {
        PitPlan plan = new PitPlan(stints[0]);
        int[] stops = stopLaps(laps);
        for (int i = 0; i < stops.length; i++) {
            plan.addStop(new PitStop(stops[i], stints[i + 1]));
        }
        return plan;
    }
}
