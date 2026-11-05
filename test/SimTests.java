import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Headless tests for the simulation, run by test.sh without any test framework.
 * Each test method throws an AssertionError on failure; main counts the results
 * and exits with a non-zero code if anything failed.
 */
public final class SimTests {

    /** Longest a full headless 10-lap race may take, in milliseconds (a target set for M1). */
    private static final long FULL_RACE_BUDGET_MS = 5000;

    /** Names of tests that failed during this run. */
    private final List<String> failures = new ArrayList<String>();

    /** Number of tests that ran. */
    private int testsRun;

    /**
     * Creates an empty test run.
     */
    public SimTests() {
        testsRun = 0;
    }

    /**
     * Runs every test and prints a summary.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        SimTests tests = new SimTests();
        tests.runAll();
        System.exit(tests.report());
    }

    /**
     * Runs each test in turn.
     */
    private void runAll() {
        check("race length is the 10-lap sprint", () -> testRaceLength());
        check("roster loads 22 drivers in 11 teams", () -> testRoster());
        check("monza loads and its loop closes", () -> testTrackGeometry());
        check("speed profile respects corner limits", () -> testSpeedProfile());
        check("clean lap within 3% of target (calibration)", () -> testCalibration());
        check("car count outside 10-22 is rejected", () -> testConfigLimits());
        check("same seed gives the same race", () -> testDeterminism());
        check("full race: no overlap, NaN, laps, order, gaps", () -> testFullRaceInvariants());
    }

    /**
     * Runs one test and records whether it passed.
     *
     * @param name readable test name
     * @param test the test body
     */
    private void check(String name, Runnable test) {
        testsRun++;
        long start = System.nanoTime();
        try {
            test.run();
            System.out.printf("PASS  %-50s %6.0f ms%n", name, (System.nanoTime() - start) / 1e6);
        } catch (AssertionError | RuntimeException e) {
            failures.add(name);
            System.out.printf("FAIL  %-50s %s%n", name, e.getMessage());
        }
    }

    /**
     * Prints the summary line.
     *
     * @return the process exit code, 0 when everything passed
     */
    private int report() {
        System.out.println();
        System.out.println((testsRun - failures.size()) + " / " + testsRun + " tests passed");
        return failures.isEmpty() ? 0 : 1;
    }

    /**
     * Fails the current test with a message when the condition is false.
     *
     * @param condition the thing that must be true
     * @param message what went wrong
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Loads Monza, turning the checked IOException into a test failure.
     *
     * @return the track
     */
    private static Track monza() {
        try {
            return TrackLoader.load("monza");
        } catch (IOException e) {
            throw new AssertionError("could not load monza: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the roster, turning the checked IOException into a test failure.
     *
     * @return the drivers
     */
    private static List<Driver> roster() {
        try {
            return RosterLoader.load();
        } catch (IOException e) {
            throw new AssertionError("could not load roster: " + e.getMessage(), e);
        }
    }

    /**
     * Checks the fixed race length constant.
     */
    private void testRaceLength() {
        require(Tuning.RACE_LAPS == 10, "RACE_LAPS should be 10 but is " + Tuning.RACE_LAPS);
    }

    /**
     * Checks the roster size, team count, and that numbers and codes are unique.
     */
    private void testRoster() {
        List<Driver> drivers = roster();
        require(drivers.size() == 22, "expected 22 drivers, got " + drivers.size());
        HashSet<String> teams = new HashSet<String>();
        HashSet<Integer> numbers = new HashSet<Integer>();
        HashSet<String> codes = new HashSet<String>();
        for (Driver d : drivers) {
            teams.add(d.getTeam().getName());
            require(numbers.add(d.getNumber()), "duplicate car number " + d.getNumber());
            require(codes.add(d.getCode()), "duplicate code " + d.getCode());
            require(d.getCode().length() == 3, "code must be 3 letters: " + d.getCode());
        }
        require(teams.size() == 11, "expected 11 teams, got " + teams.size());
    }

    /**
     * Checks lap length, sample spacing, and that the heading turns through exactly one full
     * clockwise circle (which proves the loop is closed and in the right direction).
     */
    private void testTrackGeometry() {
        Track track = monza();
        require(Math.abs(track.getLapLength() - 5793) < 1e-9, "lap length should be 5793");
        require(Math.abs(track.getSpacing() - Tuning.SAMPLE_SPACING) < 0.1, "spacing " + track.getSpacing());
        double totalTurn = 0;
        double measured = 0;
        for (int i = 0; i < track.getSampleCount(); i++) {
            TrackSample a = track.getSample(i);
            TrackSample b = track.getSample(i + 1);
            totalTurn += Track.angleDifference(b.getHeading(), a.getHeading());
            measured += Math.hypot(b.getX() - a.getX(), b.getY() - a.getY());
        }
        require(Math.abs(totalTurn + 2 * Math.PI) < 0.01, "heading should turn -2 pi, turned " + totalTurn);
        require(Math.abs(measured - track.getLapLength()) / track.getLapLength() < 0.01,
                "resampled loop length " + measured + " is not close to the lap length");
    }

    /**
     * Checks that the target speed never exceeds the corner limit and that braking between
     * neighbouring samples is achievable.
     */
    private void testSpeedProfile() {
        Track track = monza();
        double grip = LapCalibrator.gripScaleFor(track);
        SpeedProfile profile = new SpeedProfile(track, grip, Tuning.TOP_SPEED);
        for (int i = 0; i < track.getSampleCount(); i++) {
            double v = profile.speedAt(track.getSample(i).getDistance());
            double next = profile.speedAt(track.getSample(i + 1).getDistance());
            require(v <= profile.cornerLimitAt(i) + 1e-9, "sample " + i + " above its corner limit");
            double maxEntry = Math.sqrt(next * next + 2 * CarPhysics.braking(next, grip) * track.getSpacing());
            require(v <= maxEntry + 1e-6, "sample " + i + " cannot brake in time");
            require(v > 5, "sample " + i + " is implausibly slow: " + v);
        }
    }

    /**
     * Checks that a clean lap after calibration is within the brief's 3 percent of the target.
     */
    private void testCalibration() {
        Track track = monza();
        double grip = LapCalibrator.gripScaleFor(track);
        double lap = LapCalibrator.cleanLapSeconds(track, grip);
        double target = track.getProfile().getMedianRaceLapSeconds();
        double error = Math.abs(lap - target) / target;
        System.out.printf("      monza: grip scale %.3f, clean lap %.2f s, target %.2f s, error %.2f%%%n",
                grip, lap, target, error * 100);
        require(error <= Tuning.CALIBRATION_TOLERANCE, "clean lap " + lap + " misses target " + target);
    }

    /**
     * Checks that car counts outside the allowed range are refused.
     */
    private void testConfigLimits() {
        boolean tooFew = false;
        boolean tooMany = false;
        try {
            new RaceConfig(Tuning.MIN_CARS - 1, 10, 1L);
        } catch (IllegalArgumentException e) {
            tooFew = true;
        }
        try {
            new RaceConfig(Tuning.MAX_CARS + 1, 10, 1L);
        } catch (IllegalArgumentException e) {
            tooMany = true;
        }
        require(tooFew && tooMany, "out-of-range car counts must throw");
        require(new RaceConfig(22, 10, 1L).getCarCount() == 22, "22 cars must be allowed");
    }

    /**
     * Runs a race until it ends, with a step cap so a bug cannot hang the tests.
     *
     * @param race the race
     */
    private static void runToEnd(Race race) {
        int maxSteps = (int) (60 * 60 / Tuning.SIM_STEP_SECONDS);
        for (int i = 0; i < maxSteps && !race.isOver(); i++) {
            race.step();
        }
        require(race.isOver(), "race did not finish within one simulated hour");
    }

    /**
     * Checks that two races with the same seed end with identical results.
     */
    private void testDeterminism() {
        Track track = monza();
        List<Driver> drivers = roster();
        Race a = new Race(new RaceConfig(22, 3, 42L), track, drivers);
        Race b = new Race(new RaceConfig(22, 3, 42L), track, drivers);
        runToEnd(a);
        runToEnd(b);
        for (int i = 0; i < a.getStandings().size(); i++) {
            Car ca = a.getStandings().get(i);
            Car cb = b.getStandings().get(i);
            require(ca.getDriver() == cb.getDriver(), "position " + (i + 1) + " differs");
            require(ca.getFinishTime() == cb.getFinishTime(), "finish time differs at P" + (i + 1));
        }
        require(a.getPlayerCar().getDriver() == b.getPlayerCar().getDriver(), "player car differs");
    }

    /**
     * Checks that, among cars still racing on the lead lap, the gap to the leader never shrinks
     * going down the order, and that intervals are never negative.
     *
     * @param race the race in progress
     */
    private static void checkGapsIncrease(Race race) {
        List<Car> order = race.getStandings();
        double previous = 0;
        for (int i = 1; i < order.size(); i++) {
            Car car = order.get(i);
            require(race.intervalToCarAhead(car) >= 0, "negative interval");
            if (car.isFinished() || order.get(i - 1).isFinished() || race.lapsBehindLeader(car) > 0) {
                continue;
            }
            double gap = race.gapToLeader(car);
            require(gap >= previous - 1e-9, "gap shrinks at P" + (i + 1) + ": " + gap + " after " + previous);
            previous = gap;
        }
    }

    /**
     * Runs a full 22-car, 10-lap race and checks, at every step, that no two cars overlap and no
     * value is NaN or infinite; at the end, that lap counts and the finishing order make sense.
     * Also checks the race completes inside the time budget.
     */
    private void testFullRaceInvariants() {
        Track track = monza();
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 7L), track, roster());
        long start = System.currentTimeMillis();
        int steps = 0;
        double closest = Double.MAX_VALUE;
        while (!race.isOver()) {
            race.step();
            steps++;
            require(steps < 60 * 60 * 60, "race did not finish within one simulated hour");
            for (Car car : race.getCars()) {
                require(Double.isFinite(car.getDistance()) && Double.isFinite(car.getSpeed()),
                        "non-finite state for " + car.getDriver().getCode());
                require(car.getSpeed() >= 0, "negative speed");
                Car ahead = race.carAheadOnTrack(car);
                double gap = race.trackGap(car, ahead);
                closest = Math.min(closest, gap);
                require(gap >= Tuning.MIN_SEPARATION - 1e-6,
                        "overlap: " + car.getDriver().getCode() + " is " + gap + " m behind " + ahead.getDriver().getCode());
            }
            if (steps % 30 == 0) {
                checkGapsIncrease(race);
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("      %d steps, %.1f s race time, %d ms wall time, closest gap %.2f m%n",
                steps, race.getTime(), elapsed, closest);
        List<Car> order = race.getStandings();
        Car winner = order.get(0);
        require(winner.getLapsCompleted() == Tuning.RACE_LAPS, "winner completed " + winner.getLapsCompleted() + " laps");
        for (int i = 0; i < order.size(); i++) {
            Car car = order.get(i);
            require(car.getFinishPosition() == i + 1, "finish position mismatch at P" + (i + 1));
            require(car.getLapsCompleted() >= Tuning.RACE_LAPS - 1, car.getDriver().getCode() + " lap count wrong");
            if (i > 0) {
                require(car.getFinishTime() >= order.get(i - 1).getFinishTime(), "finish times out of order");
            }
            double sum = 0;
            for (int lap = 1; lap <= car.getLapsCompleted(); lap++) {
                double t = race.getLapTime(car, lap);
                require(t > 0 && Double.isFinite(t), "bad lap time");
                sum += t;
            }
            require(Math.abs(sum - car.getFinishTime()) < 1e-6, "lap times do not add up to finish time");
        }
        require(elapsed < FULL_RACE_BUDGET_MS, "full race took " + elapsed + " ms, budget " + FULL_RACE_BUDGET_MS);
    }
}
