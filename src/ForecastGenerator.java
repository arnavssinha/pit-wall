import java.util.Random;

/**
 * Draws a race's weather from a track's statistics (brief, Section 5.2).
 * Temperatures come from normal distributions clamped to plausible bounds. Rain happens with the
 * track's rain chance; if it does, a start lap, a length and an intensity are drawn. The forecast
 * shown to the player is less certain than the truth: it gives a percentage (high if rain is coming,
 * low but not zero if not) and a start lap that can be off by a lap or two on changeable tracks.
 */
public final class ForecastGenerator {

    /**
     * Not instantiated; all methods are static.
     */
    private ForecastGenerator() {
    }

    /**
     * Draws a forecast.
     *
     * @param stats the track's weather statistics
     * @param profile the track profile (reference lap time and weather volatility)
     * @param laps race length in laps
     * @param random the random generator to draw from
     * @return the forecast
     */
    public static Forecast generate(WeatherStats stats, TrackProfile profile, int laps, Random random) {
        double air = clamp(stats.getAirMean() + random.nextGaussian() * stats.getAirSd(),
                Tuning.AIR_TEMP_MIN, Tuning.AIR_TEMP_MAX);
        double track = clamp(stats.getTrackMean() + random.nextGaussian() * stats.getTrackSd(),
                Math.max(air, Tuning.TRACK_TEMP_MIN), Tuning.TRACK_TEMP_MAX);
        RainEvent rain = null;
        double shownChance;
        int shownLap = 0;
        if (random.nextDouble() < stats.getRainChance()) {
            double lapSeconds = profile.getMedianRaceLapSeconds();
            int startLap = Tuning.RAIN_FIRST_LAP + random.nextInt(Math.max(1, laps - Tuning.RAIN_FIRST_LAP - 2));
            double start = (startLap - 1 + random.nextDouble()) * lapSeconds;
            double lengthLaps = Tuning.RAIN_MIN_LAPS + random.nextDouble() * (Tuning.RAIN_MAX_LAPS - Tuning.RAIN_MIN_LAPS);
            double peak = Tuning.RAIN_MIN_PEAK + random.nextDouble() * (1.0 - Tuning.RAIN_MIN_PEAK);
            rain = new RainEvent(start, lengthLaps * lapSeconds, peak);
            shownChance = Tuning.FORECAST_RAIN_LOW + random.nextDouble() * (Tuning.FORECAST_RAIN_HIGH - Tuning.FORECAST_RAIN_LOW);
            int error = (int) Math.round(random.nextGaussian() * profile.getWeatherVolatility() * Tuning.FORECAST_LAP_ERROR);
            shownLap = Math.max(1, Math.min(laps, startLap + error));
        } else {
            shownChance = random.nextDouble() * Tuning.FORECAST_FALSE_ALARM;
            if (shownChance > Tuning.FORECAST_SHOW_LAP_ABOVE) {
                shownLap = Tuning.RAIN_FIRST_LAP + random.nextInt(Math.max(1, laps - Tuning.RAIN_FIRST_LAP - 2));
            }
        }
        return new Forecast(track, air, rain, shownChance, shownLap, stats.isApproximate());
    }

    /**
     * Keeps a value between two bounds.
     *
     * @param value the value
     * @param low lower bound
     * @param high upper bound
     * @return the clamped value
     */
    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
