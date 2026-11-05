import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

/**
 * The team radio: the most recent messages, newest at the bottom. Messages about the player's
 * car are yellow; others are white. Lines live in an ArrayDeque: new ones join the end and the
 * oldest drops off the front when the panel is full.
 */
public final class RadioFeed extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 258;

    /** Number of lines shown. */
    private static final int LINES = 5;

    /** Height of one line in pixels. */
    private static final double LINE_HEIGHT = 30;

    /** Characters per line before wrapping. */
    private static final int WRAP = 34;

    /** The lines being shown, oldest first. */
    private final transient ArrayDeque<RaceMessage> queue = new ArrayDeque<RaceMessage>();

    /** One label pair per line (two rows each, for wrapped text). */
    private final GLabel[][] labels = new GLabel[LINES][2];

    /** How many of the race's messages have been taken already. */
    private int taken;

    /**
     * Builds the panel.
     */
    public RadioFeed() {
        GRect panel = new GRect(0, 0, WIDTH, 28 + LINES * LINE_HEIGHT);
        panel.setFilled(true);
        panel.setFillColor(Palette.PANEL);
        panel.setColor(Palette.PANEL_EDGE);
        add(panel);
        GLabel title = new GLabel("TEAM RADIO", 12, 19);
        title.setFont(Palette.HUD);
        title.setColor(Palette.TEXT_DIM);
        add(title);
        for (int i = 0; i < LINES; i++) {
            for (int row = 0; row < 2; row++) {
                labels[i][row] = new GLabel("", 12, 40 + i * LINE_HEIGHT + row * 13);
                labels[i][row].setFont(Palette.SMALL);
                add(labels[i][row]);
            }
        }
    }

    /**
     * Takes any new messages from the race and redraws the lines if something changed.
     *
     * @param messages all of the race's messages so far
     */
    public void update(List<RaceMessage> messages) {
        if (taken == messages.size()) {
            return;
        }
        while (taken < messages.size()) {
            queue.addLast(messages.get(taken));
            taken++;
            if (queue.size() > LINES) {
                queue.removeFirst();
            }
        }
        Iterator<RaceMessage> it = queue.iterator();
        for (int i = 0; i < LINES; i++) {
            RaceMessage m = it.hasNext() ? it.next() : null;
            String text = m == null ? "" : m.getText();
            String first = text.length() <= WRAP ? text : text.substring(0, wrapPoint(text));
            String second = text.substring(first.length()).trim();
            for (int row = 0; row < 2; row++) {
                labels[i][row].setLabel(row == 0 ? first : second);
                labels[i][row].setColor(m != null && m.isAboutPlayer() ? Palette.PLAYER : Palette.TEXT);
            }
        }
    }

    /**
     * Finds where to break a long line: the last space before the wrap width.
     *
     * @param text the message
     * @return index to break at
     */
    private static int wrapPoint(String text) {
        int space = text.lastIndexOf(' ', WRAP);
        return space > 0 ? space : WRAP;
    }
}
