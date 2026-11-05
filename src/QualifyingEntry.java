/**
 * One driver's result in the simulated qualifying session that sets the grid.
 * The qualifying lap is the track's reference lap divided by the car's pace for this race,
 * plus a little random noise for how well the lap came together. The grid is these entries
 * sorted from fastest to slowest.
 */
public final class QualifyingEntry {

    /** The driver. */
    private final Driver driver;

    /** The car's pace multiplier for this race. */
    private final double pace;

    /** The qualifying lap time in seconds. */
    private final double lapTime;

    /**
     * Creates an entry.
     *
     * @param driver the driver
     * @param pace pace multiplier for this race
     * @param lapTime qualifying lap time in seconds
     */
    public QualifyingEntry(Driver driver, double pace, double lapTime) {
        this.driver = driver;
        this.pace = pace;
        this.lapTime = lapTime;
    }

    /**
     * Returns the driver.
     *
     * @return the driver
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * Returns the pace multiplier.
     *
     * @return pace
     */
    public double getPace() {
        return pace;
    }

    /**
     * Returns the qualifying lap time.
     *
     * @return seconds
     */
    public double getLapTime() {
        return lapTime;
    }
}
