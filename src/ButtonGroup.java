import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The clickable buttons on one screen and what each one does.
 * A HashMap links each button to its action; a click runs the action of the button under it.
 */
public final class ButtonGroup {

    /** Buttons in the order they were added (for hit testing). */
    private final List<UiButton> buttons = new ArrayList<UiButton>();

    /** What each button does. */
    private final HashMap<UiButton, Runnable> actions = new HashMap<UiButton, Runnable>();

    /**
     * Creates an empty group.
     */
    public ButtonGroup() {
        super();
    }

    /**
     * Registers a button and its action.
     *
     * @param button the button (already on the canvas)
     * @param action what to do when it is clicked
     */
    public void add(UiButton button, Runnable action) {
        buttons.add(button);
        actions.put(button, action);
    }

    /**
     * Runs the action of the enabled button under a point, if any.
     *
     * @param x x on the canvas
     * @param y y on the canvas
     * @return true if a button was clicked
     */
    public boolean click(double x, double y) {
        for (UiButton b : buttons) {
            if (b.isEnabled() && b.isVisible() && b.contains(x, y)) {
                actions.get(b).run();
                return true;
            }
        }
        return false;
    }
}
