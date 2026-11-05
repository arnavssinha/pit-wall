import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The summary of a Strategy Lab run: every individual race, plus per-strategy numbers worked out
 * from them (win and podium chance, average finish, places gained, and how the strategy did in the
 * rain or dry and on a hot or cool track).
 */
public final class LabResult {

    /** Every race run, in the order they finished. */
    private final List<LabRun> runs;

    /** The strategies tried. */
    private final List<StrategyVariant> variants;

    /** Track temperature that splits "hot" from "cool" scenarios (the median). */
    private final double hotThreshold;

    /** Wall-clock seconds the run took. */
    private final double seconds;

    /**
     * Creates a result.
     *
     * @param runs every race run
     * @param variants the strategies tried
     * @param seconds wall-clock seconds taken
     */
    public LabResult(List<LabRun> runs, List<StrategyVariant> variants, double seconds) {
        this.runs = new ArrayList<LabRun>(runs);
        this.variants = variants;
        this.seconds = seconds;
        List<Double> temps = new ArrayList<Double>();
        for (LabRun r : runs) {
            temps.add(r.getTrackTemp());
        }
        Collections.sort(temps);
        this.hotThreshold = temps.isEmpty() ? 0 : temps.get(temps.size() / 2);
    }

    /**
     * Returns the runs of one strategy, optionally only those matching a weather condition.
     *
     * @param variant the strategy
     * @param filter 0 for all runs, 1 rainy only, 2 dry only, 3 hot only, 4 cool only
     * @return the matching runs
     */
    public List<LabRun> runsOf(StrategyVariant variant, int filter) {
        List<LabRun> out = new ArrayList<LabRun>();
        for (LabRun r : runs) {
            if (r.getVariant() != variant) {
                continue;
            }
            boolean hot = r.getTrackTemp() >= hotThreshold;
            boolean keep = filter == 0 || (filter == 1 && r.isRained()) || (filter == 2 && !r.isRained())
                    || (filter == 3 && hot) || (filter == 4 && !hot);
            if (keep) {
                out.add(r);
            }
        }
        return out;
    }

    /**
     * Returns the average finishing position of some runs.
     *
     * @param list the runs
     * @return the average, or NaN if the list is empty
     */
    public static double averageFinish(List<LabRun> list) {
        double sum = 0;
        for (LabRun r : list) {
            sum += r.getFinish();
        }
        return list.isEmpty() ? Double.NaN : sum / list.size();
    }

    /**
     * Returns the average places gained of some runs.
     *
     * @param list the runs
     * @return the average, or NaN if the list is empty
     */
    public static double averageGained(List<LabRun> list) {
        double sum = 0;
        for (LabRun r : list) {
            sum += r.placesGained();
        }
        return list.isEmpty() ? Double.NaN : sum / list.size();
    }

    /**
     * Returns the share of runs that finished in the top so-many places.
     *
     * @param list the runs
     * @param top 1 for wins, 3 for podiums
     * @return a fraction from 0 to 1, or NaN if the list is empty
     */
    public static double shareInTop(List<LabRun> list, int top) {
        int count = 0;
        for (LabRun r : list) {
            if (r.getFinish() <= top) {
                count++;
            }
        }
        return list.isEmpty() ? Double.NaN : count / (double) list.size();
    }

    /**
     * Returns the preset strategy with the most places gained on average.
     *
     * @return the best preset variant
     */
    public StrategyVariant bestPreset() {
        StrategyVariant best = null;
        for (StrategyVariant v : variants) {
            if (!v.isWindow() && (best == null || averageGained(runsOf(v, 0)) > averageGained(runsOf(best, 0)))) {
                best = v;
            }
        }
        return best;
    }

    /**
     * Returns the pit-window variant with the most places gained on average.
     *
     * @return the best stop lap variant, or null if there was no sweep
     */
    public StrategyVariant bestWindow() {
        StrategyVariant best = null;
        for (StrategyVariant v : variants) {
            if (v.isWindow() && (best == null || averageGained(runsOf(v, 0)) > averageGained(runsOf(best, 0)))) {
                best = v;
            }
        }
        return best;
    }

    /**
     * Returns every race run.
     *
     * @return the runs
     */
    public List<LabRun> getRuns() {
        return Collections.unmodifiableList(runs);
    }

    /**
     * Returns the strategies tried.
     *
     * @return the variants
     */
    public List<StrategyVariant> getVariants() {
        return variants;
    }

    /**
     * Returns the median track temperature used to split hot from cool.
     *
     * @return Celsius
     */
    public double getHotThreshold() {
        return hotThreshold;
    }

    /**
     * Returns how long the run took.
     *
     * @return wall-clock seconds
     */
    public double getSeconds() {
        return seconds;
    }
}
