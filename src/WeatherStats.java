/**
 * Weather statistics for one track: average and spread of air and track temperature, and how often
 * it rains. Forecasts are drawn from these numbers.
 */
public final class WeatherStats {

    /** Track id. */
    private final String trackId;

    /** Mean air temperature in Celsius. */
    private final double airMean;

    /** Standard deviation of air temperature. */
    private final double airSd;

    /** Mean track temperature in Celsius. */
    private final double trackMean;

    /** Standard deviation of track temperature. */
    private final double trackSd;

    /** Fraction of sessions with rain, from 0 to 1. */
    private final double rainChance;

    /** Number of real sessions the numbers came from (0 for approximate values). */
    private final int sessionsUsed;

    /** True if these are approximate fallback values, not measured data. */
    private final boolean approximate;

    /**
     * Creates the statistics.
     *
     * @param trackId track id
     * @param airMean mean air temperature
     * @param airSd air temperature standard deviation
     * @param trackMean mean track temperature
     * @param trackSd track temperature standard deviation
     * @param rainChance fraction of sessions with rain
     * @param sessionsUsed number of sessions measured
     * @param approximate true for fallback values
     */
    public WeatherStats(String trackId, double airMean, double airSd, double trackMean, double trackSd,
                        double rainChance, int sessionsUsed, boolean approximate) {
        this.trackId = trackId;
        this.airMean = airMean;
        this.airSd = airSd;
        this.trackMean = trackMean;
        this.trackSd = trackSd;
        this.rainChance = rainChance;
        this.sessionsUsed = sessionsUsed;
        this.approximate = approximate;
    }

    /**
     * Returns the track id.
     *
     * @return the id
     */
    public String getTrackId() {
        return trackId;
    }

    /**
     * Returns the mean air temperature.
     *
     * @return Celsius
     */
    public double getAirMean() {
        return airMean;
    }

    /**
     * Returns the air temperature spread.
     *
     * @return standard deviation in Celsius
     */
    public double getAirSd() {
        return airSd;
    }

    /**
     * Returns the mean track temperature.
     *
     * @return Celsius
     */
    public double getTrackMean() {
        return trackMean;
    }

    /**
     * Returns the track temperature spread.
     *
     * @return standard deviation in Celsius
     */
    public double getTrackSd() {
        return trackSd;
    }

    /**
     * Returns how often it rains.
     *
     * @return fraction from 0 to 1
     */
    public double getRainChance() {
        return rainChance;
    }

    /**
     * Returns the number of sessions measured.
     *
     * @return sessions, 0 for approximate values
     */
    public int getSessionsUsed() {
        return sessionsUsed;
    }

    /**
     * Returns whether these are approximate fallback values.
     *
     * @return true if not measured
     */
    public boolean isApproximate() {
        return approximate;
    }
}
