import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * The race report shown after the flag, worked out from the race's timing tables (brief, Section 3.12):
 * the classification, the player's tire wear lap by lap, gaps to the leader lap by lap for the player and
 * the cars that finished either side, the best sector times, and how much time the player gained or lost
 * at each corner compared with the rest of the field.
 */
public final class RaceReport {

    /** Number of sectors per lap. */
    public static final int SECTORS = 3;

    /** Meters before a corner's braking point where its timing zone starts. */
    private static final double ZONE_BEFORE = 50;

    /** Meters after a corner's apex where its timing zone ends. */
    private static final double ZONE_AFTER = 100;

    /** A lap slower than the car's median lap by this factor is left out of corner analysis (pit or safety car). */
    private static final double SLOW_LAP_FACTOR = 1.1;

    /** The race. */
    private final Race race;

    /** Classified order. */
    private final List<Car> classification;

    /** Race time at the end of each lap: row = car id, column = lap minus 1 (NaN if not completed). */
    private final double[][] lapEnds;

    /** The player's sector times by lap, in lap order (a TreeMap keeps the keys sorted). */
    private final TreeMap<Integer, double[]> playerSectors = new TreeMap<Integer, double[]>();

    /** Fastest time in each sector by any car. */
    private final double[] bestSector = new double[SECTORS];

    /** Who set each fastest sector. */
    private final Car[] bestSectorCar = new Car[SECTORS];

    /** The player's fastest time in each sector. */
    private final double[] playerBestSector = new double[SECTORS];

    /** Seconds per lap the player lost (positive) or gained (negative) at each corner against the field median. */
    private final double[] cornerDelta;

    /**
     * Builds the report from a finished race.
     *
     * @param race the race (should be over)
     */
    public RaceReport(Race race) {
        this.race = race;
        this.classification = race.getClassification();
        int laps = race.getConfig().getLaps();
        lapEnds = new double[race.getCars().size()][laps];
        for (Car car : race.getCars()) {
            double t = 0;
            for (int lap = 1; lap <= laps; lap++) {
                double lapTime = lap <= car.getLapsCompleted() ? race.getLapTime(car, lap) : 0;
                t += lapTime;
                lapEnds[car.getId()][lap - 1] = lapTime > 0 ? t : Double.NaN;
            }
        }
        Arrays.fill(bestSector, Double.MAX_VALUE);
        Arrays.fill(playerBestSector, Double.MAX_VALUE);
        findSectors();
        cornerDelta = new double[race.getCorners().size()];
        findCornerDeltas();
    }

    /**
     * Times every sector of every completed lap, keeps the overall and player bests, and stores the
     * player's sectors by lap.
     */
    private void findSectors() {
        double lap = race.getTrack().getLapLength();
        for (Car car : race.getCars()) {
            for (int l = 1; l <= car.getLapsCompleted(); l++) {
                double[] times = new double[SECTORS];
                for (int s = 0; s < SECTORS; s++) {
                    double start = (l - 1) * lap + s * lap / SECTORS;
                    double end = (l - 1) * lap + (s + 1) * lap / SECTORS;
                    times[s] = race.timeAtDistance(car, end) - race.timeAtDistance(car, start);
                    if (times[s] < bestSector[s]) {
                        bestSector[s] = times[s];
                        bestSectorCar[s] = car;
                    }
                    if (car.isPlayer()) {
                        playerBestSector[s] = Math.min(playerBestSector[s], times[s]);
                    }
                }
                if (car.isPlayer()) {
                    playerSectors.put(l, times);
                }
            }
        }
    }

    /**
     * For each corner, compares the player's average time through it with the median of every other
     * car's average, using only clean laps (lap 1, pit laps and safety car laps are left out).
     */
    private void findCornerDeltas() {
        List<Corner> corners = race.getCorners();
        Car player = race.getPlayerCar();
        double[] playerAvg = cornerAverages(player);
        for (int c = 0; c < corners.size(); c++) {
            List<Double> others = new ArrayList<Double>();
            for (Car car : race.getCars()) {
                if (car != player) {
                    double v = cornerAverages(car)[c];
                    if (!Double.isNaN(v)) {
                        others.add(v);
                    }
                }
            }
            cornerDelta[c] = Double.isNaN(playerAvg[c]) || others.isEmpty() ? 0 : playerAvg[c] - median(others);
        }
    }

    /**
     * Average time a car took through each corner zone over its clean laps.
     *
     * @param car the car
     * @return one average per corner, NaN if the car had no clean laps
     */
    private double[] cornerAverages(Car car) {
        List<Corner> corners = race.getCorners();
        double lap = race.getTrack().getLapLength();
        double[] sum = new double[corners.size()];
        int[] count = new int[corners.size()];
        List<Double> lapTimes = new ArrayList<Double>();
        for (int l = 1; l <= car.getLapsCompleted(); l++) {
            lapTimes.add(race.getLapTime(car, l));
        }
        double limit = lapTimes.isEmpty() ? 0 : median(lapTimes) * SLOW_LAP_FACTOR;
        for (int l = 2; l <= car.getLapsCompleted(); l++) {
            if (race.getLapTime(car, l) > limit) {
                continue;
            }
            for (int c = 0; c < corners.size(); c++) {
                Corner corner = corners.get(c);
                double start = (l - 1) * lap + corner.getBrakingPoint() - ZONE_BEFORE;
                double end = start + ZONE_BEFORE + race.getTrack().wrap(corner.getApex() - corner.getBrakingPoint()) + ZONE_AFTER;
                double t = race.timeAtDistance(car, end) - race.timeAtDistance(car, start);
                if (!Double.isNaN(t)) {
                    sum[c] += t;
                    count[c]++;
                }
            }
        }
        double[] avg = new double[corners.size()];
        for (int c = 0; c < avg.length; c++) {
            avg[c] = count[c] == 0 ? Double.NaN : sum[c] / count[c];
        }
        return avg;
    }

    /**
     * Returns the median of some values.
     *
     * @param values the values (not empty)
     * @return the middle value (the average of the two middle values for an even count)
     */
    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<Double>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }

    /**
     * Returns a car's gap to the leader at the end of each lap.
     *
     * @param car the car
     * @return one gap per lap in seconds (NaN where the car did not complete the lap)
     */
    public double[] gapToLeaderByLap(Car car) {
        int laps = race.getConfig().getLaps();
        double[] gaps = new double[laps];
        for (int l = 0; l < laps; l++) {
            double first = Double.MAX_VALUE;
            for (double[] row : lapEnds) {
                if (!Double.isNaN(row[l])) {
                    first = Math.min(first, row[l]);
                }
            }
            gaps[l] = lapEnds[car.getId()][l] - first;
        }
        return gaps;
    }

    /**
     * Returns the player's tire wear at the end of each lap (it drops after a stop).
     *
     * @return wear per lap
     */
    public double[] playerWearByLap() {
        Car player = race.getPlayerCar();
        double[] wear = new double[race.getConfig().getLaps()];
        for (int l = 1; l <= wear.length; l++) {
            wear[l - 1] = l <= player.getLapsCompleted() ? race.getWearAtLap(player, l) : Double.NaN;
        }
        return wear;
    }

    /**
     * Returns the cars classified just ahead of and just behind the player (either may be null).
     *
     * @return {car ahead, car behind}
     */
    public Car[] playerNeighbours() {
        int i = classification.indexOf(race.getPlayerCar());
        Car ahead = i > 0 ? classification.get(i - 1) : null;
        Car behind = i + 1 < classification.size() ? classification.get(i + 1) : null;
        return new Car[] {ahead, behind};
    }

    /**
     * Returns the player's classified position.
     *
     * @return position, 1 for the winner
     */
    public int playerPosition() {
        return classification.indexOf(race.getPlayerCar()) + 1;
    }

    /**
     * Returns the classification.
     *
     * @return cars in classified order
     */
    public List<Car> getClassification() {
        return classification;
    }

    /**
     * Returns the fastest time in a sector.
     *
     * @param sector sector number from 0
     * @return seconds
     */
    public double getBestSector(int sector) {
        return bestSector[sector];
    }

    /**
     * Returns who set the fastest time in a sector.
     *
     * @param sector sector number from 0
     * @return the car
     */
    public Car getBestSectorCar(int sector) {
        return bestSectorCar[sector];
    }

    /**
     * Returns the player's fastest time in a sector.
     *
     * @param sector sector number from 0
     * @return seconds
     */
    public double getPlayerBestSector(int sector) {
        return playerBestSector[sector];
    }

    /**
     * Returns the player's sector times by lap, in lap order.
     *
     * @return lap number to three sector times
     */
    public TreeMap<Integer, double[]> getPlayerSectors() {
        return playerSectors;
    }

    /**
     * Returns the time lost (positive) or gained (negative) per lap at each corner.
     *
     * @return seconds per lap, one per corner
     */
    public double[] getCornerDelta() {
        return cornerDelta.clone();
    }

    /**
     * Returns the race.
     *
     * @return the race
     */
    public Race getRace() {
        return race;
    }
}
