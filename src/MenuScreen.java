import acm.graphics.GLabel;
import acm.graphics.GPolygon;
import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;

/**
 * The title screen: the game's name, a one-line pitch, the main buttons and a reminder of the controls.
 */
public final class MenuScreen implements Screen {

    /** Lines of the controls reminder. */
    private static final String[] CONTROLS = {
        "In the race the car drives itself. You make the calls:",
        "Q / W / E   push level: Conserve, Balanced, Push",
        "X   ERS deploy on or off",
        "A / S / D   in a battle: Attack, Defend, Hold (or click)",
        "B / N   box this lap / box next lap,  1-5 pick tires",
        "Space pause   T time speed   Z zoom   F / G camera   Esc menu",
    };

    /** The screen manager. */
    private final ScreenManager manager;

    /** The clickable buttons. */
    private final ButtonGroup buttons = new ButtonGroup();

    /**
     * Builds the menu.
     *
     * @param manager the screen manager
     */
    public MenuScreen(ScreenManager manager) {
        this.manager = manager;
        GraphicsProgram p = manager.getProgram();
        double w = manager.getWidth();
        double h = manager.getHeight();
        p.add(Palette.background(w, h));

        Track first = manager.track(manager.getTrackIds().get(0));
        if (first != null) {
            GPolygon art = TrackOutline.fit(first, w * 0.42, h * 0.8);
            art.setColor(Palette.PANEL_EDGE);
            p.add(art, w * 0.54, h * 0.1);
        }
        p.add(Palette.label("PIT WALL", Palette.TITLE, Palette.TEXT), 80, 160);
        p.add(Palette.label("An F1 race strategy game. You are the race engineer.", Palette.HUD, Palette.PLAYER), 84, 196);

        UiButton race = new UiButton("Race  (Enter)", 260, 48);
        p.add(race, 80, 240);
        buttons.add(race, () -> manager.showTrackSelect());
        UiButton labButton = new UiButton("Strategy Lab  (L)", 260, 48);
        p.add(labButton, 80, 300);
        buttons.add(labButton, () -> manager.showLab(manager.getTrackIds().get(0), null));

        double y = 390;
        for (String line : CONTROLS) {
            GLabel l = Palette.label(line, Palette.SMALL, Palette.TEXT_DIM);
            p.add(l, 84, y);
            y += 22;
        }
        p.add(Palette.label("10-lap sprint races. Track and weather data notes are in the README.",
                Palette.SMALL, Palette.TEXT_DIM), 84, h - 30);
    }

    /**
     * Nothing moves on the menu.
     *
     * @param realSeconds real time since the previous frame
     */
    @Override
    public void frame(double realSeconds) {
        return;
    }

    /**
     * Enter starts the race flow; L opens the Strategy Lab.
     *
     * @param keyCode the key
     * @param shift whether Shift was held
     */
    @Override
    public void handleKey(int keyCode, boolean shift) {
        if (keyCode == KeyEvent.VK_ENTER) {
            manager.showTrackSelect();
        } else if (keyCode == KeyEvent.VK_L) {
            manager.showLab(manager.getTrackIds().get(0), null);
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
