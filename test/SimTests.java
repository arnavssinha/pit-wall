import java.util.ArrayList;
import java.util.List;

/**
 * Headless tests for the simulation, run by test.sh without any test framework.
 * Each test method throws an AssertionError on failure; main counts the results
 * and exits with a non-zero code if anything failed.
 */
public final class SimTests {

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
            System.out.printf("PASS  %-55s %6.0f ms%n", name, (System.nanoTime() - start) / 1e6);
        } catch (AssertionError | RuntimeException e) {
            failures.add(name);
            System.out.printf("FAIL  %-55s %s%n", name, e.getMessage());
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
     * Checks the fixed race length constant.
     */
    private void testRaceLength() {
        require(Tuning.RACE_LAPS == 10, "RACE_LAPS should be 10 but is " + Tuning.RACE_LAPS);
    }
}
