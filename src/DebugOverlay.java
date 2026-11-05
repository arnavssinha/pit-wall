import acm.graphics.GCompound;
import acm.graphics.GContainer;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GRect;
import java.util.Locale;

/**
 * The F3 debug overlay: frames per second, frame time, simulation steps and the number of
 * ACM objects on screen. Numbers refresh twice a second so they are readable.
 */
public final class DebugOverlay extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Seconds between refreshes of the displayed numbers. */
    private static final double REFRESH_SECONDS = 0.5;

    /** The displayed text. */
    private final GLabel line1;

    /** Second line of displayed text. */
    private final GLabel line2;

    /** Frames counted since the last refresh. */
    private int frames;

    /** Simulation steps counted since the last refresh. */
    private int steps;

    /** Seconds of real time since the last refresh. */
    private double elapsed;

    /** Longest frame since the last refresh, in seconds. */
    private double worstFrame;

    /** Most recent frames-per-second value. */
    private double fps;

    /**
     * Builds the overlay.
     */
    public DebugOverlay() {
        GRect box = new GRect(0, 0, 300, 46);
        box.setFilled(true);
        box.setFillColor(Palette.PANEL);
        box.setColor(Palette.PANEL_EDGE);
        add(box);
        line1 = new GLabel("measuring...", 10, 19);
        line1.setFont(Palette.MONO);
        line1.setColor(Palette.TEXT);
        add(line1);
        line2 = new GLabel("", 10, 37);
        line2.setFont(Palette.MONO);
        line2.setColor(Palette.TEXT_DIM);
        add(line2);
    }

    /**
     * Records one frame and refreshes the text when enough time has passed.
     *
     * @param frameSeconds real time this frame took
     * @param simSteps simulation steps run this frame
     * @param screen the top-level container, for counting objects
     */
    public void record(double frameSeconds, int simSteps, GContainer screen) {
        frames++;
        steps += simSteps;
        elapsed += frameSeconds;
        worstFrame = Math.max(worstFrame, frameSeconds);
        if (elapsed >= REFRESH_SECONDS) {
            fps = frames / elapsed;
            line1.setLabel(String.format(Locale.ROOT, "FPS %5.1f   worst frame %4.1f ms", fps, worstFrame * 1000));
            line2.setLabel(String.format(Locale.ROOT, "objects %4d   sim steps/s %5.0f",
                    countObjects(screen), steps / elapsed));
            frames = 0;
            steps = 0;
            elapsed = 0;
            worstFrame = 0;
        }
    }

    /**
     * Returns the most recent frames-per-second measurement.
     *
     * @return frames per second
     */
    public double getFps() {
        return fps;
    }

    /**
     * Counts every ACM object inside a container, looking inside compounds too.
     *
     * @param container the container to count
     * @return number of objects
     */
    public static int countObjects(GContainer container) {
        int count = 0;
        for (int i = 0; i < container.getElementCount(); i++) {
            GObject obj = container.getElement(i);
            count++;
            if (obj instanceof GContainer) {
                count += countObjects((GContainer) obj);
            }
        }
        return count;
    }
}
