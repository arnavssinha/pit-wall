/**
 * One strategy the Strategy Lab tries: either one of the three presets with its suggested tires,
 * or a one-stop on Medium then Hard with the stop at a given lap (for the pit-window sweep).
 */
public final class StrategyVariant {

    /** Name shown in the results. */
    private final String name;

    /** The preset, or null for a pit-window variant. */
    private final StrategyPreset preset;

    /** Stop lap for a pit-window variant, or 0 for a preset. */
    private final int windowLap;

    /**
     * Creates a variant.
     *
     * @param name name shown in the results
     * @param preset the preset, or null for a pit-window variant
     * @param windowLap stop lap for a pit-window variant, or 0
     */
    private StrategyVariant(String name, StrategyPreset preset, int windowLap) {
        this.name = name;
        this.preset = preset;
        this.windowLap = windowLap;
    }

    /**
     * Creates the variant for a preset.
     *
     * @param preset the preset
     * @return the variant
     */
    public static StrategyVariant ofPreset(StrategyPreset preset) {
        return new StrategyVariant(preset.getDisplayName(), preset, 0);
    }

    /**
     * Creates a one-stop (Medium, then Hard) variant stopping at the end of a given lap.
     *
     * @param lap the stop lap
     * @return the variant
     */
    public static StrategyVariant pitWindow(int lap) {
        return new StrategyVariant("Stop lap " + lap, null, lap);
    }

    /**
     * Builds a fresh pit plan for one race.
     *
     * @return the plan
     */
    public PitPlan makePlan() {
        if (preset != null) {
            return preset.makePlan();
        }
        PitPlan plan = new PitPlan(Compound.MEDIUM);
        plan.addStop(new PitStop(windowLap, Compound.HARD));
        return plan;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this variant belongs to the pit-window sweep.
     *
     * @return true for a pit-window variant
     */
    public boolean isWindow() {
        return preset == null;
    }

    /**
     * Returns the stop lap of a pit-window variant.
     *
     * @return the lap, or 0 for a preset
     */
    public int getWindowLap() {
        return windowLap;
    }
}
