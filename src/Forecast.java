/**
 * The weather for one race: what will really happen (temperatures and an optional spell of rain)
 * and what the forecast shown to the player says, which is deliberately less certain: the chance
 * of rain is a percentage and the expected start lap can be a lap or two out.
 */
public final class Forecast {

    /** Track temperature in Celsius. */
    private final double trackTemp;

    /** Air temperature in Celsius. */
    private final double airTemp;

    /** The rain that will really fall, or null for a dry race. */
    private final RainEvent rain;

    /** The chance of rain the forecast shows, from 0 to 1. */
    private final double shownChance;

    /** The lap the forecast says rain could arrive, or 0 if it shows no likely rain. */
    private final int shownLap;

    /** True if the forecast is based on approximate statistics. */
    private final boolean approximate;

    /**
     * Creates a forecast.
     *
     * @param trackTemp track temperature
     * @param airTemp air temperature
     * @param rain the real rain, or null
     * @param shownChance the chance shown to the player
     * @param shownLap the lap shown to the player, or 0
     * @param approximate whether approximate statistics were used
     */
    public Forecast(double trackTemp, double airTemp, RainEvent rain, double shownChance, int shownLap, boolean approximate) {
        this.trackTemp = trackTemp;
        this.airTemp = airTemp;
        this.rain = rain;
        this.shownChance = shownChance;
        this.shownLap = shownLap;
        this.approximate = approximate;
    }

    /**
     * Returns the track temperature.
     *
     * @return Celsius
     */
    public double getTrackTemp() {
        return trackTemp;
    }

    /**
     * Returns the air temperature.
     *
     * @return Celsius
     */
    public double getAirTemp() {
        return airTemp;
    }

    /**
     * Returns the rain that will really fall.
     *
     * @return the rain spell, or null for a dry race
     */
    public RainEvent getRain() {
        return rain;
    }

    /**
     * Returns the chance of rain shown to the player.
     *
     * @return fraction from 0 to 1
     */
    public double getShownChance() {
        return shownChance;
    }

    /**
     * Returns the lap the forecast says rain could arrive.
     *
     * @return the lap, or 0 if no rain is expected
     */
    public int getShownLap() {
        return shownLap;
    }

    /**
     * Returns whether approximate statistics were used.
     *
     * @return true if approximate
     */
    public boolean isApproximate() {
        return approximate;
    }
}
