/**
 * One full screen of the game (menu, track select, setup, race, results, lab).
 * The main loop calls frame once per frame and passes keys and clicks to the current screen.
 * A screen switches to another by asking the ScreenManager.
 */
public interface Screen {

    /**
     * Runs one frame: advance anything that moves, then redraw.
     *
     * @param realSeconds real time since the previous frame
     */
    void frame(double realSeconds);

    /**
     * Reacts to a key press.
     *
     * @param keyCode the key, as a KeyEvent constant
     * @param shift whether Shift was held
     */
    void handleKey(int keyCode, boolean shift);

    /**
     * Reacts to a mouse click.
     *
     * @param x x on the canvas
     * @param y y on the canvas
     */
    void handleClick(double x, double y);
}
