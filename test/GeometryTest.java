/**
 * Headless geometry checks for the first prototype.
 *
 * @author @harnoor
 * @author @arnav
 */
public final class GeometryTest {
    /**
     * Checks track coordinates, headings, and sample storage.
     *
     * @author @arnav
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        Track track = new Track(100, 20);
        checkClose(track.getX(0), 100, "X at zero");
        checkClose(track.getY(Math.PI / 2), 100, "Y at quarter lap");
        checkClose(track.getHeading(0), Math.PI / 2, "heading at zero");

        Car car = new Car(20, 10);
        car.update(Math.PI / 2, track);
        checkClose(car.getX(), 0, "car X");
        checkClose(car.getY(), 100, "car Y");
        checkClose(car.getAngle(), Math.PI, "car heading");

        double[][] checkpoints = track.sampleCenterline(8);
        if (checkpoints.length != 8 || checkpoints[0].length != 2) {
            throw new AssertionError("Expected an 8-by-2 coordinate array.");
        }
        checkClose(checkpoints[0][0], 100, "first checkpoint");
        checkClose(track.getRadius(), 100, "radius");
        checkClose(track.getTrackWidth(), 20, "width");
        System.out.println("PASS: prototype geometry and 2D array checks");
    }

    /**
     * Verifies that a floating-point result matches a target.
     *
     * @author @arnav
     * @param actual actual measurement
     * @param expected expected measurement
     * @param label description of the test
     */
    private static void checkClose(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 1e-9) {
            throw new AssertionError(label + ": expected " + expected
                    + " but got " + actual);
        }
    }
}
