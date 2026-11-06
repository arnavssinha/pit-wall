import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.util.List;

/**
 * Team radio as a short pop-up: the newest message appears in one panel for a few seconds and then
 * hides, so the screen stays clear. Messages about the player's car are yellow.
 */
public final class RadioFeed extends GCompound {

    /** Serialization id (GCompound is serializable). */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 560;

    /** Panel height in pixels. */
    public static final double HEIGHT = 30;

    /** Real seconds a message stays on screen. */
    private static final double SHOW_SECONDS = 5.0;

    /** Background. */
    private final GRect panel;

    /** The message text. */
    private final GLabel text;

    /** Number of race messages already shown. */
    private int taken;

    /** Real seconds the current message has been shown. */
    private double shownFor = SHOW_SECONDS;

    /**
     * Builds the (hidden) pop-up.
     */
    public RadioFeed() {
        panel = new GRect(0, 0, WIDTH, HEIGHT);
        panel.setFilled(true);
        panel.setFillColor(Palette.PANEL);
        panel.setColor(Palette.PANEL_EDGE);
        add(panel);
        GLabel title = new GLabel("RADIO", 10, 20);
        title.setFont(Palette.SMALL);
        title.setColor(Palette.TEXT_DIM);
        add(title);
        text = new GLabel("", 62, 20);
        text.setFont(Palette.HUD);
        add(text);
        setVisible(false);
    }

    /**
     * Shows the newest message, if there is a new one, and hides the pop-up once it has been up long enough.
     *
     * @param messages all race messages so far
     * @param realSeconds real time since the last frame
     */
    public void update(List<RaceMessage> messages, double realSeconds) {
        if (taken < messages.size()) {
            RaceMessage newest = messages.get(messages.size() - 1);
            taken = messages.size();
            text.setLabel(newest.getText());
            text.setColor(newest.isAboutPlayer() ? Palette.PLAYER : Palette.TEXT);
            panel.setSize(Math.max(WIDTH, text.getWidth() + 76), HEIGHT);
            shownFor = 0;
        } else {
            shownFor += realSeconds;
        }
        boolean visible = shownFor < SHOW_SECONDS;
        if (isVisible() != visible) {
            setVisible(visible);
        }
    }
}
