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
        check("drawn cars cannot overlap at minimum gap", () -> testDrawnCarsFit());
        check("same seed gives the same race", () -> testDeterminism());
        check("full race: no overlap, NaN, laps, order, gaps", () -> testFullRaceInvariants());
        check("tire wear only ever increases", () -> testWearMonotonic());
        check("tire life: Soft 5-6, Medium 7-8, Hard 10+ laps", () -> testTireLife());
        check("Soft beats Hard fresh, loses when worn", () -> testCompoundOrdering());
        check("Push is faster but wears more; Conserve the opposite", () -> testPushTradeOff());
        check("wet tires lose in the dry and win in the wet", () -> testWetTires());
        check("brakes fade only when hot; ERS stays in 0-100%", () -> testBrakesAndErs());
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
        SpeedProfile profile = new SpeedProfile(track, grip * Compound.MEDIUM.getDryGrip(), Tuning.TOP_SPEED);
        for (int i = 0; i < track.getSampleCount(); i++) {
            double v = profile.speedAt(track.getSample(i).getDistance());
            double next = profile.speedAt(track.getSample(i + 1).getDistance());
            require(v <= profile.cornerLimitAt(i) + 1e-9, "sample " + i + " above its corner limit");
            double maxEntry = Math.sqrt(next * next + 2 * CarPhysics.braking(next, grip * Compound.MEDIUM.getDryGrip()) * track.getSpacing());
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
     * Checks that two cars at the minimum gap are drawn without overlapping, given that the
     * screen shrinks the layout and enlarges the cars, and that grid slots respect the gap.
     */
    private void testDrawnCarsFit() {
        double drawnGap = Tuning.MIN_SEPARATION * TrackDrawing.LAYOUT_SCALE;
        require(drawnGap >= TrackDrawing.CAR_LENGTH + 1.0,
                "cars at the minimum gap are " + drawnGap + " units apart but drawn " + TrackDrawing.CAR_LENGTH + " long");
        require(Tuning.GRID_SLOT_SPACING >= Tuning.MIN_SEPARATION, "grid slots closer than the minimum gap");
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
        Race a = new Race(new RaceConfig(22, 3, 42L), track, drivers, Compound.MEDIUM);
        Race b = new Race(new RaceConfig(22, 3, 42L), track, drivers, Compound.MEDIUM);
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
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 7L), track, roster(), Compound.MEDIUM);
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

    /**
     * Drives one car alone for a number of laps from a standing start, the same way the race does,
     * and records each lap's time and the tire wear at the end of each lap.
     *
     * @param compound tires fitted
     * @param push push level held all stint
     * @param laps laps to drive
     * @return two rows: row 0 lap times, row 1 wear at the end of each lap
     */
    private static double[][] runStint(Compound compound, PushLevel push, int laps) {
        Track track = monza();
        ProfileSet profiles = new ProfileSet(track, LapCalibrator.gripScaleFor(track));
        TrackConditions conditions = TrackConditions.neutral();
        Driver driver = new Driver(0, "TST", "Test driver", new Team("Test", "#FFFFFF"));
        Car car = new Car(0, driver, 1, 1.0, 0, 0, compound, false);
        car.setPushLevel(push);
        double dt = Tuning.SIM_STEP_SECONDS;
        double[][] out = new double[2][laps];
        double time = 0;
        double lapStart = 0;
        int lap = 0;
        double lastWear = 0;
        while (lap < laps) {
            double target = car.targetSpeed(profiles, conditions);
            car.approachSpeed(target, profiles.getGripScale(), dt);
            double move = car.getSpeed() * dt;
            car.advance(move, dt);
            car.updateSystems(dt, move, track, profiles, conditions, false);
            time += dt;
            require(car.getTire().getWear() >= lastWear, "wear went down");
            lastWear = car.getTire().getWear();
            if (car.getDistance() >= (lap + 1) * track.getLapLength()) {
                out[0][lap] = time - lapStart;
                out[1][lap] = lastWear;
                lapStart = time;
                lap++;
            }
        }
        return out;
    }

    /**
     * Average wear per lap over laps 2 to 4, converted to a neutral track (wear multiplier 1.0).
     *
     * @param stint result of runStint
     * @return wear per lap on a neutral track
     */
    private static double neutralWearPerLap(double[][] stint) {
        double perLap = (stint[1][3] - stint[1][0]) / 3.0;
        return perLap / monza().getProfile().getTrackWearMultiplier();
    }

    /**
     * Checks wear never decreases during a stint (runStint checks every step).
     */
    private void testWearMonotonic() {
        double[][] stint = runStint(Compound.SOFT, PushLevel.PUSH, 4);
        require(stint[1][3] > stint[1][0], "wear did not grow");
    }

    /**
     * Checks the brief's sprint scaling: laps to full wear at Balanced on a neutral track.
     */
    private void testTireLife() {
        double soft = 1.0 / neutralWearPerLap(runStint(Compound.SOFT, PushLevel.BALANCED, 4));
        double medium = 1.0 / neutralWearPerLap(runStint(Compound.MEDIUM, PushLevel.BALANCED, 4));
        double hard = 1.0 / neutralWearPerLap(runStint(Compound.HARD, PushLevel.BALANCED, 4));
        System.out.printf("      life in laps (neutral track): soft %.1f, medium %.1f, hard %.1f%n", soft, medium, hard);
        require(soft >= 5.0 && soft <= 6.0, "soft life " + soft);
        require(medium >= 7.0 && medium <= 8.0, "medium life " + medium);
        require(hard >= 10.0, "hard life " + hard);
    }

    /**
     * Checks a fresh Soft laps faster than a fresh Hard, and a worn Soft slower than a Hard of the same age.
     */
    private void testCompoundOrdering() {
        double[][] soft = runStint(Compound.SOFT, PushLevel.BALANCED, 9);
        double[][] hard = runStint(Compound.HARD, PushLevel.BALANCED, 9);
        System.out.printf("      lap 2: soft %.2f hard %.2f; lap 9: soft %.2f hard %.2f%n",
                soft[0][1], hard[0][1], soft[0][8], hard[0][8]);
        require(soft[0][1] < hard[0][1], "fresh soft should be faster than fresh hard");
        require(soft[0][8] > hard[0][8], "worn soft should be slower than hard");
    }

    /**
     * Checks Push laps faster and wears more than Balanced, and Conserve laps slower and wears less.
     */
    private void testPushTradeOff() {
        double[][] conserve = runStint(Compound.MEDIUM, PushLevel.CONSERVE, 4);
        double[][] balanced = runStint(Compound.MEDIUM, PushLevel.BALANCED, 4);
        double[][] push = runStint(Compound.MEDIUM, PushLevel.PUSH, 4);
        System.out.printf("      lap 2 on Medium: conserve %.2f, balanced %.2f, push %.2f%n",
                conserve[0][1], balanced[0][1], push[0][1]);
        require(push[0][1] < balanced[0][1] && balanced[0][1] < conserve[0][1], "lap times not ordered by push");
        require(push[1][3] > balanced[1][3] && balanced[1][3] > conserve[1][3], "wear not ordered by push");
        double gain = balanced[0][1] - push[0][1];
        require(gain < 1.0, "push gains " + gain + " s per lap, which makes pushing all race too strong");
    }

    /**
     * Checks grip from wetness alone: rain tires lose to slicks in the dry and beat them in the wet,
     * and full wets beat intermediates on a soaked track.
     */
    private void testWetTires() {
        double softDry = wetGrip(Compound.SOFT, 0.0);
        double wetDry = wetGrip(Compound.WET, 0.0);
        double softWet = wetGrip(Compound.SOFT, 1.0);
        double interWet = wetGrip(Compound.INTERMEDIATE, 1.0);
        double wetWet = wetGrip(Compound.WET, 1.0);
        double softDamp = wetGrip(Compound.SOFT, 0.6);
        double interDamp = wetGrip(Compound.INTERMEDIATE, 0.6);
        require(wetDry < softDry, "wets should lose in the dry");
        require(wetWet > softWet && wetWet > interWet, "wets should win when soaked");
        require(interDamp > softDamp, "intermediates should beat slicks on a damp track");
    }

    /**
     * Grip from compound and wetness only.
     *
     * @param compound the compound
     * @param wetness track wetness
     * @return dry grip times wetness factor
     */
    private static double wetGrip(Compound compound, double wetness) {
        return compound.getDryGrip() * new Tire(compound).wetnessFactor(wetness);
    }

    /**
     * Checks brake fade starts only above the fade temperature, and the battery never leaves 0 to 1.
     */
    private void testBrakesAndErs() {
        Brakes brakes = new Brakes();
        require(brakes.fadeFactor() == 1.0, "cold brakes should not fade");
        brakes.addHeat(Tuning.BRAKE_FADE_TEMP);
        require(brakes.fadeFactor() < 1.0, "very hot brakes should fade");
        Ers ers = new Ers();
        for (int i = 0; i < 5000; i++) {
            ers.update(Tuning.SIM_STEP_SECONDS, 0, true, true);
        }
        require(ers.getCharge() == 0 && !ers.isDeploying(), "battery should run flat and stop deploying");
        for (int i = 0; i < 5000; i++) {
            ers.update(Tuning.SIM_STEP_SECONDS, 40, false, true);
        }
        require(ers.getCharge() == 1.0, "battery should fill to exactly 100%");
    }
}
