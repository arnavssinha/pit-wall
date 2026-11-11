import acm.graphics.GCanvas;
import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.program.GraphicsProgram;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * First-deliverable ACM graphics feasibility prototype: one circular lap.
 *
 * @author @samuel
 * @author @arnav
 */
public final class PitWallDemo extends GraphicsProgram {
    /** ACM window width. */
    public static final int APPLICATION_WIDTH = 960;
    /** ACM window height. */
    public static final int APPLICATION_HEIGHT = 720;

    private static final double CENTER_X = 480;
    private static final double CENTER_Y = 375;
    private static final int FRAMES_PER_LAP = 240;
    private static final int FRAME_DELAY_MS = 20;
    private static final Color BACKGROUND = new Color(17, 24, 37);

    /**
     * Launches the ACM graphics window.
     *
     * @author @arnav
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new PitWallDemo().start(args);
    }

    /**
     * Animates one lap and records positions in an ArrayList.
     *
     * @author @samuel
     */
    @Override
    public void run() {
        setBackground(BACKGROUND);
        GCanvas canvas = getGCanvas();
        Track track = new Track(190, 70);
        Car car = new Car(36, 18);

        track.draw(canvas, CENTER_X, CENTER_Y, 1.0);
        drawCheckpoints(canvas, track);
        addHeading(canvas);
        GLabel status = addStatusLabel(canvas);

        List<double[]> telemetry = new ArrayList<double[]>();
        GPolygon previousCar = null;
        for (int frame = 0; frame <= FRAMES_PER_LAP; frame++) {
            double t = 2.0 * Math.PI * frame / FRAMES_PER_LAP;
            car.update(t, track);
            if (previousCar != null) {
                canvas.remove(previousCar);
            }
            previousCar = car.draw(canvas, CENTER_X, CENTER_Y, 1.0);
            telemetry.add(new double[] {car.getX(), car.getY()});
            status.setLabel(formatStatus(car, telemetry.size()));
            pause(FRAME_DELAY_MS);
        }
        status.setLabel("Prototype complete: one lap, "
                + telemetry.size() + " positions captured.");
    }

    /**
     * Draws twelve centerline markers from a two-dimensional coordinate array.
     *
     * @author @arnav
     * @param canvas ACM canvas
     * @param track track supplying the checkpoints
     */
    private void drawCheckpoints(GCanvas canvas, Track track) {
        double[][] checkpoints = track.sampleCenterline(12);
        for (double[] point : checkpoints) {
            double px = CENTER_X + point[0];
            double py = CENTER_Y - point[1];
            GOval marker = new GOval(px - 3, py - 3, 6, 6);
            marker.setFilled(true);
            marker.setFillColor(new Color(250, 194, 83));
            marker.setColor(new Color(250, 194, 83));
            canvas.add(marker);
        }
    }

    /**
     * Adds the title and explanation.
     *
     * @author @arnav
     * @param canvas ACM canvas
     */
    private void addHeading(GCanvas canvas) {
        GLabel title = new GLabel("PIT WALL  |  ACM GRAPHICS PROTOTYPE", 35, 46);
        title.setFont("SansSerif-BOLD-23");
        title.setColor(Color.WHITE);
        canvas.add(title);

        GLabel subtitle = new GLabel(
                "Circular track, rotated car, 2D checkpoints and lap telemetry", 35, 76);
        subtitle.setFont("SansSerif-PLAIN-15");
        subtitle.setColor(new Color(190, 207, 225));
        canvas.add(subtitle);
    }

    /**
     * Adds the status line, initially showing a startup message.
     *
     * @author @arnav
     * @param canvas ACM canvas
     * @return status label that the demo will update
     */
    private GLabel addStatusLabel(GCanvas canvas) {
        GLabel status = new GLabel("Initializing prototype...", 35, 680);
        status.setFont("SansSerif-PLAIN-17");
        status.setColor(Color.WHITE);
        canvas.add(status);
        return status;
    }

    /**
     * Formats a readable status line.
     *
     * @author @arnav
     * @param car current car
     * @param sampleCount number of saved position samples
     * @return current heading and number of samples
     */
    private String formatStatus(Car car, int sampleCount) {
        return String.format(Locale.US,
                "Heading: %.0f degrees   |   Position samples: %d",
                Math.toDegrees(car.getAngle()), sampleCount);
    }
}
