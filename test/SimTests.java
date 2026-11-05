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
        check("grid follows qualifying; player is on the grid", () -> testQualifyingGrid());
        check("slipstream helps only close behind on straights", () -> testSlipstream());
        check("corners found with braking zones and difficulty", () -> testCorners());
        check("pass chances: Monza-like high, Monaco-like very low", () -> testPassProbabilityBands());
        check("AI-only Monza races: pass rate in band", () -> testRacePassRate());
        check("pit stop: on the planned lap, new tires, 15-35 s lost", () -> testPitStop());
        check("two-compound rule disqualifies a no-stop car", () -> testCompoundRule());
        check("every strategy preset meets the two-compound rule", () -> testPresetsLegal());
        check("forecasts follow the weather statistics", () -> testForecasts());
        check("rain: track gets wet, AI fits rain tires, rule lifted", () -> testRainRace());
        check("wetness grid: cells dry at different rates", () -> testUnevenDrying());
        check("safety car: no passes, field bunches up", () -> testSafetyCar());
        check("retirement: car stops, classified DNF, race ends", () -> testRetirement());
        check("event queue hands out the earliest event first", () -> testEventQueue());
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
            if (car.isFinished() || car.isRetired() || order.get(i - 1).isFinished() || race.lapsBehindLeader(car) > 0) {
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
        int sideBySideSteps = 0;
        while (!race.isOver()) {
            race.step();
            steps++;
            require(steps < 60 * 60 * 60, "race did not finish within one simulated hour");
            for (Car car : race.getCars()) {
                require(Double.isFinite(car.getDistance()) && Double.isFinite(car.getSpeed())
                        && Double.isFinite(car.getLateralOffset()), "non-finite state for " + car.getDriver().getCode());
                require(car.getSpeed() >= 0, "negative speed");
                for (Car other : race.getCars()) {
                    if (other == car) {
                        continue;
                    }
                    double gap = race.trackGap(car, other);
                    if (gap < Tuning.MIN_SEPARATION - 1e-6) {
                        boolean allowed = car.getPassPartner() == other || car.isOffTrack() != other.isOffTrack()
                                || car.isRetired() || other.isRetired();
                        require(Race.sideBySide(car, other) && allowed,
                                "overlap: " + car.getDriver().getCode() + " is " + gap + " m behind "
                                + other.getDriver().getCode() + " without being alongside (pass or pit lane)");
                        sideBySideSteps++;
                    } else {
                        closest = Math.min(closest, gap);
                    }
                }
            }
            if (steps % 30 == 0) {
                checkGapsIncrease(race);
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("      %d steps, %.1f s race time, %d ms wall time, closest nose-to-tail gap %.2f m%n",
                steps, race.getTime(), elapsed, closest);
        System.out.printf("      %d battle attacks, %d passes, %d car-steps alongside%n",
                race.getBattles().getAttempts(), race.getBattles().getPasses(), sideBySideSteps);
        List<Car> order = race.getStandings();
        Car winner = order.get(0);
        require(winner.getLapsCompleted() == Tuning.RACE_LAPS, "winner completed " + winner.getLapsCompleted() + " laps");
        for (int i = 0; i < order.size(); i++) {
            Car car = order.get(i);
            if (car.isRetired()) {
                continue;
            }
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
        TrackConditions conditions = TrackConditions.neutral(track);
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

    /**
     * Checks the grid is in qualifying order, every car is on it once, and exactly one car is the player's.
     */
    private void testQualifyingGrid() {
        Race race = new Race(new RaceConfig(22, 10, 3L), monza(), roster());
        List<QualifyingEntry> quali = race.getQualifying();
        int players = 0;
        for (int i = 0; i < race.getCars().size(); i++) {
            Car car = race.getCars().get(i);
            require(car.getGridPosition() == i + 1, "grid positions out of order");
            require(car.getDriver() == quali.get(i).getDriver(), "grid does not follow qualifying at P" + (i + 1));
            if (i > 0) {
                require(quali.get(i).getLapTime() >= quali.get(i - 1).getLapTime(), "qualifying not sorted");
            }
            if (car.isPlayer()) {
                players++;
            }
        }
        require(players == 1 && race.getPlayerCar().isPlayer(), "exactly one player car expected");
    }

    /**
     * Checks the slipstream bonus: none in corners or beyond the gap limit, biggest when closest.
     */
    private void testSlipstream() {
        double speed = 80;
        require(Race.slipstreamFactor(40, speed, false) == 1.0, "no slipstream in corners");
        require(Race.slipstreamFactor(speed * 1.2, speed, true) == 1.0, "no slipstream beyond the gap limit");
        double near = Race.slipstreamFactor(20, speed, true);
        double far = Race.slipstreamFactor(60, speed, true);
        require(near > far && far > 1.0, "closer should mean more slipstream");
    }

    /**
     * Checks Monza has several corners with braking zones, in lap order, with difficulty from 0 to 1.
     */
    private void testCorners() {
        Race race = new Race(new RaceConfig(10, 1, 1L), monza(), roster());
        List<Corner> corners = race.getCorners();
        require(corners.size() >= 5, "expected at least 5 braking corners, found " + corners.size());
        for (int i = 0; i < corners.size(); i++) {
            Corner c = corners.get(i);
            require(c.getNumber() == i + 1, "corners not numbered in order");
            require(c.getDifficulty() >= 0 && c.getDifficulty() <= 1, "difficulty out of range");
        }
        System.out.println("      monza braking corners: " + corners.size());
    }

    /**
     * Checks the pass formula for two equal cars: on an easy track an attack against a car that
     * holds usually works and against a defender sometimes works; on a very hard track an attack
     * against a defender almost never works.
     */
    private void testPassProbabilityBands() {
        Race race = new Race(new RaceConfig(10, 1, 1L), monza(), roster());
        Car a = race.getCars().get(1);
        Car d = race.getCars().get(0);
        Corner corner = race.getCorners().get(0);
        double easyHold = chance(a, d, corner, Stance.HOLD, 0.15);
        double easyDefend = chance(a, d, corner, Stance.DEFEND, 0.15);
        double hardDefend = chance(a, d, corner, Stance.DEFEND, 0.9);
        System.out.printf("      pass chance: easy track vs Hold %.2f, vs Defend %.2f; hard track vs Defend %.2f%n",
                easyHold, easyDefend, hardDefend);
        require(easyHold > 0.5 && easyHold < 0.95, "easy track vs Hold out of band");
        require(easyDefend > 0.15 && easyDefend < easyHold, "easy track vs Defend out of band");
        require(hardDefend < 0.1, "hard track vs Defend should be very low");
    }

    /**
     * Pass probability for an attack at 0.5 s with no ERS and equal grip.
     *
     * @param a attacker
     * @param d defender
     * @param corner the corner
     * @param defence the defender's stance
     * @param trackDifficulty the track's overtaking difficulty
     * @return probability
     */
    private static double chance(Car a, Car d, Corner corner, Stance defence, double trackDifficulty) {
        Battle b = new Battle(a, d, corner, 0, 0.5, 0, false);
        b.setAttackerStance(Stance.ATTACK);
        b.setDefenderStance(defence);
        return BattleResolver.passProbability(BattleResolver.passScore(b, 1.0, 1.0, false, false, trackDifficulty));
    }

    /**
     * Runs three AI-only Monza races and checks the share of attacks that become passes, and that
     * passes do happen but not on every corner.
     */
    private void testRacePassRate() {
        int attempts = 0;
        int passes = 0;
        for (long seed = 11; seed <= 13; seed++) {
            Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, seed), monza(), roster());
            runToEnd(race);
            attempts += race.getBattles().getAttempts();
            passes += race.getBattles().getPasses();
        }
        double rate = passes / (double) Math.max(1, attempts);
        System.out.printf("      3 races: %d attacks, %d passes (%.0f%% success, %.0f passes per race)%n",
                attempts, passes, rate * 100, passes / 3.0);
        require(rate > 0.25 && rate < 0.65, "pass success rate " + rate + " out of band");
        require(passes / 3.0 >= 10 && passes / 3.0 <= 70, "passes per race out of band");
    }

    /**
     * Gives the player the one-stop-late plan, runs the race, and checks the stop happened at the
     * end of lap 6 with Softs fitted, and that the lap with the stop lost a realistic amount of time.
     */
    private void testPitStop() {
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 21L), monza(), roster());
        race.setPlayerPlan(StrategyPreset.ONE_STOP_LATE.makePlan(), PushLevel.BALANCED);
        Car player = race.getPlayerCar();
        int stopLap = 0;
        while (!race.isOver()) {
            race.step();
            if (stopLap == 0 && player.getStopsMade() == 1) {
                stopLap = player.getLapsCompleted();
            }
        }
        require(player.getStopsMade() == 1, "player should stop once, stopped " + player.getStopsMade());
        require(stopLap == 6, "stop should complete lap 6, completed lap " + stopLap);
        require(player.getTire().getCompound() == Compound.SOFT, "Softs should be fitted after the stop");
        double loss = race.getLapTime(player, 6) - race.getLapTime(player, 5)
                + race.getLapTime(player, 7) - race.getLapTime(player, 8);
        System.out.printf("      pit stop cost about %.1f s (laps 6 and 7 against laps 5 and 8)%n", loss);
        require(loss > 15 && loss < 35, "pit loss out of range: " + loss);
    }

    /**
     * Gives the player a plan with no stops; in a dry race that breaks the rule, so the player must be
     * disqualified and classified last, while every computer car (on legal presets) is classified.
     */
    private void testCompoundRule() {
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 22L), monza(), roster());
        race.setPlayerPlan(new PitPlan(Compound.HARD), PushLevel.BALANCED);
        runToEnd(race);
        List<Car> classification = race.getClassification();
        require(race.getPlayerCar().isDisqualified(), "no-stop player should be disqualified");
        require(classification.get(classification.size() - 1) == race.getPlayerCar(), "disqualified car should be last");
        for (Car car : race.getCars()) {
            if (!car.isPlayer() && !car.isRetired()) {
                require(!car.isDisqualified(), car.getDriver().getCode() + " broke the rule");
                require(car.getStopsMade() >= 1, car.getDriver().getCode() + " never stopped");
            }
        }
    }

    /**
     * Checks each strategy preset uses two different dry compounds.
     */
    private void testPresetsLegal() {
        for (StrategyPreset preset : StrategyPreset.values()) {
            require(preset.makePlan().usesTwoDryCompounds(), preset + " breaks the two-compound rule");
        }
        require(!new PitPlan(Compound.MEDIUM).usesTwoDryCompounds(), "a no-stop plan should fail the rule");
    }

    /**
     * Draws many forecasts and checks the rain fraction matches the statistics and every value is in range.
     */
    private void testForecasts() {
        WeatherStats stats;
        try {
            stats = WeatherStatsLoader.load().get("monza");
        } catch (IOException e) {
            throw new AssertionError("could not load weather: " + e.getMessage(), e);
        }
        require(stats != null, "no weather statistics for monza");
        java.util.Random random = new java.util.Random(5);
        int rainy = 0;
        int draws = 4000;
        for (int i = 0; i < draws; i++) {
            Forecast f = ForecastGenerator.generate(stats, monza().getProfile(), Tuning.RACE_LAPS, random);
            require(f.getAirTemp() >= Tuning.AIR_TEMP_MIN && f.getAirTemp() <= Tuning.AIR_TEMP_MAX, "air temp out of range");
            require(f.getTrackTemp() >= f.getAirTemp() && f.getTrackTemp() <= Tuning.TRACK_TEMP_MAX, "track temp out of range");
            require(f.getShownLap() >= 0 && f.getShownLap() <= Tuning.RACE_LAPS, "forecast lap out of range");
            if (f.getRain() != null) {
                rainy++;
                require(f.getShownChance() >= Tuning.FORECAST_RAIN_LOW, "rain coming but forecast chance low");
            }
        }
        double fraction = rainy / (double) draws;
        System.out.printf("      rain in %.1f%% of forecasts (statistics say %.0f%%, approximate=%s)%n",
                fraction * 100, stats.getRainChance() * 100, stats.isApproximate());
        require(Math.abs(fraction - stats.getRainChance()) < 0.03, "rain fraction does not match statistics");
    }

    /**
     * Runs a race with heavy rain from 2 to 7 minutes and checks the track gets wet, most computer
     * cars switch to rain tires, and the race counts as wet so nobody is disqualified for one dry compound.
     */
    private void testRainRace() {
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 31L), monza(), roster());
        race.setRain(new RainEvent(120, 300, 1.0));
        double peakWetness = 0;
        while (!race.isOver()) {
            race.step();
            peakWetness = Math.max(peakWetness, race.getConditions().averageWetness());
        }
        int rainTires = 0;
        int running = 0;
        for (Car car : race.getCars()) {
            if (!car.isPlayer() && !car.isRetired()) {
                running++;
                if (car.usedRainTires()) {
                    rainTires++;
                }
                require(!car.isDisqualified(), "nobody should be disqualified in a wet race");
            }
        }
        System.out.printf("      peak wetness %.2f, %d of %d computer cars used rain tires%n", peakWetness, rainTires, running);
        require(peakWetness > 0.6, "track never got properly wet");
        require(race.isWetRace(), "race should count as wet");
        require(rainTires >= running / 2, "most computer cars should switch to rain tires");
    }

    /**
     * Soaks the track, lets it dry for a minute, and checks some parts are drier than others.
     */
    private void testUnevenDrying() {
        Track track = monza();
        TrackConditions conditions = new TrackConditions(track, 35, 25, new java.util.Random(3));
        conditions.setWetness(1.0);
        for (int i = 0; i < 60 * 60; i++) {
            conditions.update(Tuning.SIM_STEP_SECONDS, 0);
        }
        double min = 1;
        double max = 0;
        for (int i = 0; i < track.getSampleCount(); i += 5) {
            double w = conditions.wetnessAt(track.getSample(i).getDistance());
            min = Math.min(min, w);
            max = Math.max(max, w);
        }
        System.out.printf("      after 60 s of drying: wettest %.2f, driest %.2f%n", max, min);
        require(max - min > 0.05, "all parts of the track dried at the same rate");
        require(max < 1.0, "track did not dry at all");
    }

    /**
     * Brings out the safety car early in a race and checks no passes happen while it is out and the
     * cars that stay out (no pit stop under the safety car) close up. Cars that pit are left out of
     * the spread, because a stop drops them back.
     */
    private void testSafetyCar() {
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 41L), monza(), roster());
        Car victim = race.getCars().get(5) == race.getPlayerCar() ? race.getCars().get(6) : race.getCars().get(5);
        double deployAt = 200;
        race.scheduleSafetyCar(deployAt, victim);
        while (race.getTime() < deployAt + 1) {
            race.step();
        }
        require(race.getSafetyCar().isActive(), "safety car should be out");
        int n = race.getCars().size();
        double[] startDistance = new double[n];
        int[] startStops = new int[n];
        for (Car car : race.getCars()) {
            startDistance[car.getId()] = car.getDistance();
            startStops[car.getId()] = car.getStopsMade();
        }
        int passesBefore = race.getBattles().getPasses();
        while (race.getSafetyCar().isActive()) {
            race.step();
        }
        double firstBefore = -Double.MAX_VALUE;
        double lastBefore = Double.MAX_VALUE;
        double firstAfter = -Double.MAX_VALUE;
        double lastAfter = Double.MAX_VALUE;
        int stayedOut = 0;
        for (Car car : race.getCars()) {
            boolean stayed = !car.isRetired() && !car.isInPitLane() && car.getStopsMade() == startStops[car.getId()];
            if (stayed) {
                stayedOut++;
                firstBefore = Math.max(firstBefore, startDistance[car.getId()]);
                lastBefore = Math.min(lastBefore, startDistance[car.getId()]);
                firstAfter = Math.max(firstAfter, car.getDistance());
                lastAfter = Math.min(lastAfter, car.getDistance());
            }
        }
        double before = firstBefore - lastBefore;
        double after = firstAfter - lastAfter;
        System.out.printf("      %d cars stayed out: spread %.0f m when the safety car came out, %.0f m when it went in%n",
                stayedOut, before, after);
        require(race.getBattles().getPasses() == passesBefore, "no passes allowed under the safety car");
        require(stayedOut >= 5, "too few cars stayed out to judge");
        require(after < before * 0.7, "field should bunch up behind the safety car");
        require(victim.isRetired(), "the stopped car should be out of the race");
    }

    /**
     * Retires a car mid-race and checks it parks, is classified behind the finishers, and the race ends.
     */
    private void testRetirement() {
        Race race = new Race(new RaceConfig(22, Tuning.RACE_LAPS, 51L), monza(), roster());
        Car car = race.getCars().get(10) == race.getPlayerCar() ? race.getCars().get(11) : race.getCars().get(10);
        race.scheduleRetirement(150, car);
        runToEnd(race);
        require(car.isRetired(), "car should have retired");
        require(car.getSpeed() == 0, "retired car should have stopped");
        List<Car> order = race.getClassification();
        int finishers = 0;
        for (Car c : order) {
            if (!c.isRetired() && !c.isDisqualified()) {
                finishers++;
            }
        }
        require(order.indexOf(car) >= finishers, "retired car should be classified behind every finisher");
    }

    /**
     * Adds events out of order and checks they come back earliest first, and only when due.
     */
    private void testEventQueue() {
        EventQueue queue = new EventQueue();
        queue.schedule(new RaceEvent(30, RaceEvent.Type.RAIN_STOP, null));
        queue.schedule(new RaceEvent(10, RaceEvent.Type.RAIN_START, null));
        queue.schedule(new RaceEvent(20, RaceEvent.Type.SAFETY_CAR_END, null));
        require(queue.takeDue(5).isEmpty(), "nothing is due yet");
        List<RaceEvent> due = queue.takeDue(25);
        require(due.size() == 2 && due.get(0).getTime() == 10 && due.get(1).getTime() == 20, "wrong order or count");
        require(queue.size() == 1 && queue.peek().getTime() == 30, "the last event should remain");
    }
}
