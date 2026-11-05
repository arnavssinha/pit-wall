/**
 * The descriptive facts about one circuit, read from data/tracks/(id).properties.
 * Geometry lives in Track; this class only holds names and numbers.
 */
public final class TrackProfile {

    /** Short id used in file names, such as "monza". */
    private final String id;

    /** Display name, such as "Monza". */
    private final String name;

    /** Country the circuit is in. */
    private final String country;

    /** Official lap length in meters; the centerline is rescaled to match it. */
    private final double lapLengthMeters;

    /** Typical green-flag race lap in seconds; the speed model is calibrated to it. */
    private final double medianRaceLapSeconds;

    /** Difficulty label shown to the player. */
    private final String difficultyLabel;

    /** One-line description of what makes the track different. */
    private final String description;

    /** Where the geometry and lap time came from, shown so approximate data is never hidden. */
    private final String dataSource;

    /**
     * Creates a profile.
     *
     * @param id short id
     * @param name display name
     * @param country country
     * @param lapLengthMeters official lap length in meters
     * @param medianRaceLapSeconds typical race lap in seconds
     * @param difficultyLabel difficulty label
     * @param description one-line description
     * @param dataSource note on where the data came from
     */
    public TrackProfile(String id, String name, String country, double lapLengthMeters,
                        double medianRaceLapSeconds, String difficultyLabel, String description,
                        String dataSource) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.lapLengthMeters = lapLengthMeters;
        this.medianRaceLapSeconds = medianRaceLapSeconds;
        this.difficultyLabel = difficultyLabel;
        this.description = description;
        this.dataSource = dataSource;
    }

    /**
     * Returns the short id.
     *
     * @return the id, such as "monza"
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the country.
     *
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * Returns the official lap length.
     *
     * @return lap length in meters
     */
    public double getLapLengthMeters() {
        return lapLengthMeters;
    }

    /**
     * Returns the typical race lap time used as the calibration target.
     *
     * @return lap time in seconds
     */
    public double getMedianRaceLapSeconds() {
        return medianRaceLapSeconds;
    }

    /**
     * Returns the difficulty label.
     *
     * @return the label
     */
    public String getDifficultyLabel() {
        return difficultyLabel;
    }

    /**
     * Returns the one-line description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the note on where the data came from.
     *
     * @return the data source note
     */
    public String getDataSource() {
        return dataSource;
    }
}
