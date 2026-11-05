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

    /** How hard the surface and layout are on tires; 1.0 is neutral. */
    private final double trackWearMultiplier;

    /** How hard it is to overtake here, from 0 (easy) to 1 (almost impossible). */
    private final double overtakingDifficulty;

    /** Distance along the lap of the pit entry, in meters. */
    private final double pitEntryDistance;

    /** Distance along the lap of the pit exit, in meters. */
    private final double pitExitDistance;

    /** Side of the track the pit lane is on: +1 left, -1 right (in the direction of travel). */
    private final int pitSide;

    /** Chance that an incident brings out the safety car in one race, 0 to 1. */
    private final double safetyCarProbability;

    /** How changeable the weather is, 0 to 1; widens the rain forecast's error. */
    private final double weatherVolatility;

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
     * @param trackWearMultiplier tire wear multiplier, 1.0 is neutral
     * @param overtakingDifficulty overtaking difficulty from 0 to 1
     * @param pitEntryDistance pit entry distance along the lap in meters
     * @param pitExitDistance pit exit distance along the lap in meters
     * @param pitSide +1 for a pit lane on the left, -1 on the right
     * @param safetyCarProbability chance of a safety car per race
     * @param weatherVolatility how changeable the weather is, 0 to 1
     * @param difficultyLabel difficulty label
     * @param description one-line description
     * @param dataSource note on where the data came from
     */
    public TrackProfile(String id, String name, String country, double lapLengthMeters,
                        double medianRaceLapSeconds, double trackWearMultiplier, double overtakingDifficulty,
                        double pitEntryDistance, double pitExitDistance, int pitSide,
                        double safetyCarProbability, double weatherVolatility, String difficultyLabel, String description,
                        String dataSource) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.lapLengthMeters = lapLengthMeters;
        this.medianRaceLapSeconds = medianRaceLapSeconds;
        this.trackWearMultiplier = trackWearMultiplier;
        this.overtakingDifficulty = overtakingDifficulty;
        this.pitEntryDistance = pitEntryDistance;
        this.pitExitDistance = pitExitDistance;
        this.pitSide = pitSide;
        this.safetyCarProbability = safetyCarProbability;
        this.weatherVolatility = weatherVolatility;
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
     * Returns the tire wear multiplier.
     *
     * @return wear multiplier, 1.0 is neutral
     */
    public double getTrackWearMultiplier() {
        return trackWearMultiplier;
    }

    /**
     * Returns how hard it is to overtake.
     *
     * @return difficulty from 0 to 1
     */
    public double getOvertakingDifficulty() {
        return overtakingDifficulty;
    }

    /**
     * Returns where the pit entry is.
     *
     * @return distance along the lap in meters
     */
    public double getPitEntryDistance() {
        return pitEntryDistance;
    }

    /**
     * Returns where the pit exit is.
     *
     * @return distance along the lap in meters
     */
    public double getPitExitDistance() {
        return pitExitDistance;
    }

    /**
     * Returns which side the pit lane is on.
     *
     * @return +1 left, -1 right
     */
    public int getPitSide() {
        return pitSide;
    }

    /**
     * Returns the chance of a safety car per race.
     *
     * @return probability from 0 to 1
     */
    public double getSafetyCarProbability() {
        return safetyCarProbability;
    }

    /**
     * Returns how changeable the weather is.
     *
     * @return volatility from 0 to 1
     */
    public double getWeatherVolatility() {
        return weatherVolatility;
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
