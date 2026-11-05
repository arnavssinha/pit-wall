import java.util.Random;

/**
 * The weather on track right now: temperatures, how hard it is raining, and how wet each part of
 * the track is.
 * Wetness lives in a 2D grid of square cells laid over the circuit (double[][]). Rain wets every
 * cell; when it stops, each cell dries at its own rate (shade, drainage), stored in a second grid,
 * so some corners dry before others. Wetness lags the rain: it builds and fades gradually.
 */
public final class TrackConditions {

    /** The track, for turning a lap distance into a grid cell. */
    private final Track track;

    /** Track surface temperature in Celsius. */
    private final double trackTemp;

    /** Air temperature in Celsius. */
    private final double airTemp;

    /** Wetness of each cell, from 0 (dry) to 1 (soaked); indexed [column][row]. */
    private final double[][] wetness;

    /** How fast each cell dries relative to average (1.0); indexed [column][row]. */
    private final double[][] dryingRate;

    /** Grid column of each track sample, worked out once so lookups are fast. */
    private final int[] sampleColumn;

    /** Grid row of each track sample. */
    private final int[] sampleRow;

    /** Current rain intensity from 0 to 1. */
    private double rainIntensity;

    /** Average wetness along the lap, recomputed once per update. */
    private double average;

    /**
     * Creates dry conditions with the given temperatures.
     *
     * @param track the track
     * @param trackTemp track temperature in Celsius
     * @param airTemp air temperature in Celsius
     * @param random random generator for the cells' drying rates
     */
    public TrackConditions(Track track, double trackTemp, double airTemp, Random random) {
        this.track = track;
        this.trackTemp = trackTemp;
        this.airTemp = airTemp;
        int columns = (int) Math.ceil((track.getMaxX() - track.getMinX()) / Tuning.WETNESS_CELL_METERS) + 1;
        int rows = (int) Math.ceil((track.getMaxY() - track.getMinY()) / Tuning.WETNESS_CELL_METERS) + 1;
        wetness = new double[columns][rows];
        dryingRate = new double[columns][rows];
        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                dryingRate[c][r] = 1.0 + (random.nextDouble() * 2 - 1) * Tuning.DRYING_VARIATION;
            }
        }
        int n = track.getSampleCount();
        sampleColumn = new int[n];
        sampleRow = new int[n];
        for (int i = 0; i < n; i++) {
            TrackSample s = track.getSample(i);
            sampleColumn[i] = Math.min(columns - 1, (int) ((s.getX() - track.getMinX()) / Tuning.WETNESS_CELL_METERS));
            sampleRow[i] = Math.min(rows - 1, (int) ((s.getY() - track.getMinY()) / Tuning.WETNESS_CELL_METERS));
        }
    }

    /**
     * Creates the neutral dry conditions from Tuning (used when no forecast is applied).
     *
     * @param track the track
     * @return neutral conditions
     */
    public static TrackConditions neutral(Track track) {
        return new TrackConditions(track, Tuning.NEUTRAL_TRACK_TEMP, Tuning.NEUTRAL_AIR_TEMP, new Random(0));
    }

    /**
     * Advances the weather by one step: rain wets every cell; with no rain each cell dries at its own
     * rate, faster on a hotter track.
     *
     * @param dt step length in seconds
     * @param intensity rain intensity from 0 to 1
     */
    public void update(double dt, double intensity) {
        rainIntensity = intensity;
        double wetting = intensity * dt / Tuning.WETTING_SECONDS;
        double drying = intensity > Tuning.DRIZZLE ? 0 : dt / Tuning.DRYING_SECONDS * (trackTemp / Tuning.NEUTRAL_TRACK_TEMP);
        for (int c = 0; c < wetness.length; c++) {
            for (int r = 0; r < wetness[c].length; r++) {
                double w = wetness[c][r] + wetting - drying * dryingRate[c][r];
                wetness[c][r] = Math.max(0, Math.min(1, w));
            }
        }
        average = computeAverage();
    }

    /**
     * Returns the wetness at a point on the track, from the grid cell its nearest sample lies in.
     *
     * @param distance distance along the lap in meters
     * @return wetness from 0 to 1
     */
    public double wetnessAt(double distance) {
        int i = track.indexAt(distance);
        return wetness[sampleColumn[i]][sampleRow[i]];
    }

    /**
     * Returns the average wetness along the whole lap (as of the last update).
     *
     * @return wetness from 0 to 1
     */
    public double averageWetness() {
        return average;
    }

    /**
     * Averages the wetness over evenly spread points around the lap.
     *
     * @return wetness from 0 to 1
     */
    private double computeAverage() {
        double total = 0;
        int n = track.getSampleCount();
        for (int i = 0; i < n; i += Tuning.WETNESS_AVERAGE_STRIDE) {
            total += wetnessAt(track.getSample(i).getDistance());
        }
        return total / Math.ceil(n / (double) Tuning.WETNESS_AVERAGE_STRIDE);
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
     * Returns how hard it is raining.
     *
     * @return intensity from 0 to 1
     */
    public double getRainIntensity() {
        return rainIntensity;
    }

    /**
     * Sets every cell to the same wetness (used by tests and scenarios).
     *
     * @param value wetness from 0 to 1
     */
    public void setWetness(double value) {
        for (double[] column : wetness) {
            for (int r = 0; r < column.length; r++) {
                column[r] = Math.max(0, Math.min(1, value));
            }
        }
        average = computeAverage();
    }
}
