import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Strategy Lab (brief, Section 3.13): runs many complete races with no graphics and compares
 * strategies.
 * Each scenario is one random seed (which sets the grid, the computer drivers' plans and pace, and
 * the incidents) plus weather drawn from the track's statistics. Every strategy is raced on the same
 * scenarios, so differences come from the strategy and not from luck. The computer's default stance
 * policy stands in for the player in battles. Races run in parallel on all processor cores; each race
 * has its own objects and random generator, so they never interfere.
 */
public final class StrategyLab {

    /** Where the pit-window sweep stops, as fractions of the race. */
    private static final double[] WINDOW_FRACTIONS = {0.2, 0.4, 0.6, 0.8};

    /** Longest a single lab race may run, in race seconds (a guard; real races take about 15 minutes). */
    private static final double MAX_RACE_SECONDS = 3600;

    /** The track. */
    private final Track track;

    /** Every driver. */
    private final List<Driver> roster;

    /** The track's weather statistics, or null for neutral dry races. */
    private final WeatherStats weather;

    /** Number of cars per race. */
    private final int carCount;

    /** Race length in laps. */
    private final int laps;

    /** Races finished so far in the current run (read by the screen for its progress bar). */
    private final AtomicInteger finished = new AtomicInteger();

    /** Races planned in the current run. */
    private volatile int planned;

    /**
     * Creates a lab for one track.
     *
     * @param track the track
     * @param roster every driver
     * @param weather the track's weather statistics, or null
     * @param carCount number of cars per race
     * @param laps race length in laps
     */
    public StrategyLab(Track track, List<Driver> roster, WeatherStats weather, int carCount, int laps) {
        this.track = track;
        this.roster = roster;
        this.weather = weather;
        this.carCount = carCount;
        this.laps = laps;
    }

    /**
     * Returns the strategies the lab compares for a race length: the presets that fit, then a
     * one-stop pit-window sweep (when the race is long enough to stop on different laps).
     *
     * @param laps race length in laps
     * @return the variants
     */
    public static List<StrategyVariant> variants(int laps) {
        List<StrategyVariant> list = new ArrayList<StrategyVariant>();
        for (StrategyPreset preset : StrategyPreset.forRace(laps)) {
            list.add(StrategyVariant.ofPreset(preset));
        }
        List<Integer> used = new ArrayList<Integer>();
        for (double f : WINDOW_FRACTIONS) {
            int lap = Math.max(1, Math.min(laps - 1, (int) Math.round(f * laps)));
            if (laps >= Tuning.COMPOUND_RULE_MIN_LAPS && !used.contains(lap)) {
                used.add(lap);
                list.add(StrategyVariant.pitWindow(lap));
            }
        }
        return list;
    }

    /**
     * Runs about the requested number of races (rounded down to whole scenarios, each raced once per
     * strategy) and returns the summary. Blocks until done; call it from a background thread.
     *
     * @param totalRaces roughly how many races to run in all
     * @param baseSeed seed of the first scenario; the others follow from it
     * @return the summary
     */
    public LabResult run(int totalRaces, long baseSeed) {
        List<StrategyVariant> variants = variants(laps);
        int scenarios = Math.max(1, totalRaces / variants.size());
        planned = scenarios * variants.size();
        finished.set(0);
        long start = System.nanoTime();
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<LabRun>> futures = new ArrayList<Future<LabRun>>();
        for (int s = 0; s < scenarios; s++) {
            long seed = baseSeed + s * 7919L;
            for (StrategyVariant v : variants) {
                futures.add(pool.submit(() -> raceOnce(v, seed)));
            }
        }
        List<LabRun> runs = new ArrayList<LabRun>();
        try {
            for (Future<LabRun> f : futures) {
                runs.add(f.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Strategy Lab was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("A Strategy Lab race failed: " + e.getCause(), e.getCause());
        } finally {
            pool.shutdown();
        }
        return new LabResult(runs, variants, (System.nanoTime() - start) / 1e9);
    }

    /**
     * Runs one race of one scenario with one strategy for the player's car.
     *
     * @param variant the strategy
     * @param seed the scenario seed
     * @return the player's outcome
     */
    private LabRun raceOnce(StrategyVariant variant, long seed) {
        Race race = new Race(new RaceConfig(carCount, laps, seed), track, roster);
        Forecast forecast = null;
        if (weather != null) {
            forecast = ForecastGenerator.generate(weather, track.getProfile(), laps, new Random(seed * 31 + 7));
            race.applyForecast(forecast);
        }
        race.setPlayerPlan(variant.makePlan(laps), PushLevel.BALANCED);
        race.runToEnd(MAX_RACE_SECONDS);
        Car player = race.getPlayerCar();
        String status = player.isDisqualified() ? "DSQ" : player.isRetired() ? "DNF" : "Finished";
        int finish = status.equals("Finished") ? race.getClassification().indexOf(player) + 1 : carCount;
        boolean rained = forecast != null && forecast.getRain() != null;
        double trackTemp = race.getConditions().getTrackTemp();
        finished.incrementAndGet();
        return new LabRun(variant, seed, player.getGridPosition(), finish, status, rained, trackTemp);
    }

    /**
     * Returns how many races of the current run have finished.
     *
     * @return finished races
     */
    public int getFinished() {
        return finished.get();
    }

    /**
     * Returns how many races the current run will do.
     *
     * @return planned races
     */
    public int getPlanned() {
        return planned;
    }
}
