import acm.graphics.GCompound;
import acm.graphics.GLine;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rain effects drawn over the track: a blue-grey tint that deepens as the track gets wetter, and
 * streaks of rain that fall while it is raining (more and brighter in heavier rain). The streaks
 * are made once and only moved each frame.
 */
public final class WeatherOverlay extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Number of rain streaks at full intensity. */
    private static final int STREAKS = 70;

    /** Streak fall speed in pixels per second. */
    private static final double FALL_SPEED = 900;

    /** Sideways drift of each streak per pixel fallen. */
    private static final double SLANT = 0.25;

    /** Streak length in pixels. */
    private static final double STREAK_LENGTH = 22;

    /** Strongest tint opacity (0 to 255) on a soaked track. */
    private static final int MAX_TINT = 70;

    /** Screen width in pixels. */
    private final double width;

    /** Screen height in pixels. */
    private final double height;

    /** The tint over the whole screen. */
    private final GRect tint;

    /** The rain streaks. */
    private final transient List<GLine> streaks = new ArrayList<GLine>();

    /** Random placement of streaks (purely visual, separate from the race's generator). */
    private final transient Random random = new Random(1);

    /** Tint level currently shown, in steps of opacity. */
    private int shownTint = -1;

    /**
     * Builds the overlay for a screen of the given size.
     *
     * @param width screen width in pixels
     * @param height screen height in pixels
     */
    public WeatherOverlay(double width, double height) {
        this.width = width;
        this.height = height;
        tint = new GRect(0, 0, width, height);
        tint.setFilled(true);
        tint.setVisible(false);
        add(tint);
        for (int i = 0; i < STREAKS; i++) {
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * height;
            GLine line = new GLine(x, y, x + STREAK_LENGTH * SLANT, y + STREAK_LENGTH);
            line.setColor(Palette.RAIN);
            line.setVisible(false);
            streaks.add(line);
            add(line);
        }
    }

    /**
     * Updates the tint and moves the rain streaks.
     *
     * @param conditions the current weather
     * @param realSeconds real time since the previous frame
     */
    public void update(TrackConditions conditions, double realSeconds) {
        int level = (int) Math.round(conditions.averageWetness() * MAX_TINT / 5.0) * 5;
        if (level != shownTint) {
            shownTint = level;
            tint.setVisible(level > 0);
            tint.setFillColor(new Color(40, 70, 110, level));
            tint.setColor(new Color(40, 70, 110, level));
        }
        double intensity = conditions.getRainIntensity();
        int active = (int) Math.round(intensity * STREAKS);
        double fall = FALL_SPEED * realSeconds;
        for (int i = 0; i < streaks.size(); i++) {
            GLine line = streaks.get(i);
            boolean on = i < active;
            if (line.isVisible() != on) {
                line.setVisible(on);
            }
            if (!on) {
                continue;
            }
            double x = line.getStartPoint().getX() + fall * SLANT;
            double y = line.getStartPoint().getY() + fall;
            if (y > height) {
                y = -STREAK_LENGTH;
                x = random.nextDouble() * width;
            }
            line.setStartPoint(x, y);
            line.setEndPoint(x + STREAK_LENGTH * SLANT, y + STREAK_LENGTH);
        }
    }
}
