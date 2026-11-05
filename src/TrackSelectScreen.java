import acm.graphics.GPolygon;
import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Track select: one card per available track with an outline drawn from its centerline data, its
 * name, difficulty, lap length and what makes it different. Click a card's button (or press its
 * number key) to go to the setup screen.
 */
public final class TrackSelectScreen implements Screen {

    /** Card width in pixels. */
    private static final double CARD_W = 380;

    /** Card height in pixels. */
    private static final double CARD_H = 220;

    /** Gap between cards in pixels. */
    private static final double GAP = 24;

    /** Characters per description line. */
    private static final int WRAP = 30;

    /** The screen manager. */
    private final ScreenManager manager;

    /** The clickable buttons. */
    private final ButtonGroup buttons = new ButtonGroup();

    /**
     * Builds the screen.
     *
     * @param manager the screen manager
     */
    public TrackSelectScreen(ScreenManager manager) {
        this.manager = manager;
        GraphicsProgram p = manager.getProgram();
        p.add(Palette.background(manager.getWidth(), manager.getHeight()));
        p.add(Palette.label("CHOOSE A CIRCUIT", Palette.HEADLINE, Palette.TEXT), 40, 60);
        p.add(Palette.label("Esc: back to the menu", Palette.SMALL, Palette.TEXT_DIM), 42, 84);
        int column = 0;
        int row = 0;
        for (int i = 0; i < manager.getTrackIds().size(); i++) {
            Track track = manager.track(manager.getTrackIds().get(i));
            if (track == null) {
                return;
            }
            addCard(p, track, i, 40 + column * (CARD_W + GAP), 110 + row * (CARD_H + GAP));
            column++;
            if (column == 3) {
                column = 0;
                row++;
            }
        }
        p.add(Palette.label("Only circuits with track data are listed. More arrive with the data export.",
                Palette.SMALL, Palette.TEXT_DIM), 42, manager.getHeight() - 24);
    }

    /**
     * Adds one track card.
     *
     * @param p the program
     * @param track the track
     * @param index position in the list, for the number key
     * @param x left edge
     * @param y top edge
     */
    private void addCard(GraphicsProgram p, Track track, int index, double x, double y) {
        TrackProfile profile = track.getProfile();
        p.add(Palette.panel(CARD_W, CARD_H), x, y);
        GPolygon outline = TrackOutline.fit(track, 130, 170);
        p.add(outline, x + 14, y + 22);
        double tx = x + 160;
        p.add(Palette.label(profile.getName(), Palette.HEADLINE.deriveFont(22f), Palette.TEXT), tx, y + 34);
        p.add(Palette.label(profile.getCountry() + "   " + String.format(Locale.ROOT, "%.3f km", profile.getLapLengthMeters() / 1000),
                Palette.SMALL, Palette.TEXT_DIM), tx, y + 54);
        p.add(Palette.label("Difficulty: " + profile.getDifficultyLabel(), Palette.HUD, Palette.PLAYER), tx, y + 78);
        String text = profile.getDescription();
        double ly = y + 100;
        while (!text.isEmpty()) {
            int cut = text.length() <= WRAP ? text.length() : Math.max(1, text.lastIndexOf(' ', WRAP));
            p.add(Palette.label(text.substring(0, cut).trim(), Palette.SMALL, Palette.TEXT), tx, ly);
            text = text.substring(cut).trim();
            ly += 16;
        }
        UiButton select = new UiButton("Select  (" + (index + 1) + ")", 200, 36);
        p.add(select, tx, y + CARD_H - 50);
        String id = profile.getId();
        buttons.add(select, () -> manager.showSetup(id));
    }

    /**
     * Nothing moves on this screen.
     *
     * @param realSeconds real time since the previous frame
     */
    @Override
    public void frame(double realSeconds) {
        return;
    }

    /**
     * Number keys pick a track; Esc goes back.
     *
     * @param keyCode the key
     * @param shift whether Shift was held
     */
    @Override
    public void handleKey(int keyCode, boolean shift) {
        int index = keyCode - KeyEvent.VK_1;
        if (keyCode == KeyEvent.VK_ENTER) {
            index = 0;
        }
        if (index >= 0 && index < manager.getTrackIds().size()) {
            manager.showSetup(manager.getTrackIds().get(index));
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            manager.showMenu();
        }
    }

    /**
     * Clicks the button under the mouse.
     *
     * @param x x on the canvas
     * @param y y on the canvas
     */
    @Override
    public void handleClick(double x, double y) {
        buttons.click(x, y);
    }
}
