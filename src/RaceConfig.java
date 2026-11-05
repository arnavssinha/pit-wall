/**
 * The choices that define one race before it starts: how many cars, how many laps,
 * and the random seed. The same config and seed always produce the same race.
 */
public final class RaceConfig {

    /** Number of cars on the grid. */
    private final int carCount;

    /** Number of laps. */
    private final int laps;

    /** Seed for the race's random number generator. */
    private final long seed;

    /**
     * Creates a config.
     *
     * @param carCount number of cars, from Tuning.MIN_CARS to Tuning.MAX_CARS
     * @param laps number of laps, at least 1
     * @param seed random seed
     * @throws IllegalArgumentException if the car count or lap count is out of range
     */
    public RaceConfig(int carCount, int laps, long seed) {
        if (carCount < Tuning.MIN_CARS || carCount > Tuning.MAX_CARS) {
            throw new IllegalArgumentException("Car count must be " + Tuning.MIN_CARS + " to "
                    + Tuning.MAX_CARS + ", got " + carCount);
        }
        if (laps < 1) {
            throw new IllegalArgumentException("A race needs at least one lap");
        }
        this.carCount = carCount;
        this.laps = laps;
        this.seed = seed;
    }

    /**
     * Returns the number of cars.
     *
     * @return car count
     */
    public int getCarCount() {
        return carCount;
    }

    /**
     * Returns the number of laps.
     *
     * @return lap count
     */
    public int getLaps() {
        return laps;
    }

    /**
     * Returns the random seed.
     *
     * @return the seed
     */
    public long getSeed() {
        return seed;
    }
}
