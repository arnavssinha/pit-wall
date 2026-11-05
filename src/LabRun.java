/**
 * The outcome of one Strategy Lab race for the car standing in for the player.
 */
public final class LabRun {

    /** The strategy used. */
    private final StrategyVariant variant;

    /** The scenario's random seed (grid, pace, incidents). */
    private final long seed;

    /** Starting position. */
    private final int grid;

    /** Classified position; a car that retired or was disqualified counts as last. */
    private final int finish;

    /** "Finished", "DNF" or "DSQ". */
    private final String status;

    /** Whether it rained in this scenario. */
    private final boolean rained;

    /** Track temperature in Celsius. */
    private final double trackTemp;

    /**
     * Records a run.
     *
     * @param variant the strategy
     * @param seed scenario seed
     * @param grid starting position
     * @param finish classified position
     * @param status Finished, DNF or DSQ
     * @param rained whether it rained
     * @param trackTemp track temperature
     */
    public LabRun(StrategyVariant variant, long seed, int grid, int finish, String status, boolean rained, double trackTemp) {
        this.variant = variant;
        this.seed = seed;
        this.grid = grid;
        this.finish = finish;
        this.status = status;
        this.rained = rained;
        this.trackTemp = trackTemp;
    }

    /**
     * Returns the strategy.
     *
     * @return the variant
     */
    public StrategyVariant getVariant() {
        return variant;
    }

    /**
     * Returns the scenario seed.
     *
     * @return the seed
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Returns the starting position.
     *
     * @return grid position
     */
    public int getGrid() {
        return grid;
    }

    /**
     * Returns the classified position.
     *
     * @return finish position
     */
    public int getFinish() {
        return finish;
    }

    /**
     * Returns places gained from the start (negative if places were lost).
     *
     * @return grid minus finish
     */
    public int placesGained() {
        return grid - finish;
    }

    /**
     * Returns the status.
     *
     * @return Finished, DNF or DSQ
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns whether it rained.
     *
     * @return true if it rained
     */
    public boolean isRained() {
        return rained;
    }

    /**
     * Returns the track temperature.
     *
     * @return Celsius
     */
    public double getTrackTemp() {
        return trackTemp;
    }
}
