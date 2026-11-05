/**
 * The weather the cars are driving in right now: track and air temperature and how wet the track is.
 * Until the weather system arrives (M6) a race uses neutral dry conditions from Tuning.
 */
public final class TrackConditions {

    /** Track surface temperature in Celsius. */
    private double trackTemp;

    /** Air temperature in Celsius. */
    private double airTemp;

    /** Track wetness from 0 (dry) to 1 (soaked). */
    private double wetness;

    /**
     * Creates conditions.
     *
     * @param trackTemp track temperature in Celsius
     * @param airTemp air temperature in Celsius
     * @param wetness wetness from 0 to 1
     */
    public TrackConditions(double trackTemp, double airTemp, double wetness) {
        this.trackTemp = trackTemp;
        this.airTemp = airTemp;
        this.wetness = wetness;
    }

    /**
     * Creates the neutral dry conditions used until weather is simulated.
     *
     * @return neutral conditions
     */
    public static TrackConditions neutral() {
        return new TrackConditions(Tuning.NEUTRAL_TRACK_TEMP, Tuning.NEUTRAL_AIR_TEMP, 0.0);
    }

    /**
     * Returns the temperature tires settle toward when not working: mostly track, partly air.
     *
     * @return ambient temperature for tires in Celsius
     */
    public double tireAmbient() {
        return Tuning.TRACK_TEMP_SHARE * trackTemp + (1 - Tuning.TRACK_TEMP_SHARE) * airTemp;
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
     * Returns the wetness at a point on the track. Uniform for now; per-section wetness comes with weather.
     *
     * @param distance distance along the lap in meters
     * @return wetness from 0 to 1
     */
    public double wetnessAt(double distance) {
        return wetness;
    }

    /**
     * Sets the wetness everywhere.
     *
     * @param value wetness from 0 to 1
     */
    public void setWetness(double value) {
        wetness = Math.max(0, Math.min(1, value));
    }
}
