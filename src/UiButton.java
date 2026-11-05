import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.awt.Color;

/**
 * A clickable button made from ACM shapes: a rectangle with a centered label.
 * It can be highlighted (chosen) or disabled (greyed out). Clicks are detected by the screen
 * that owns it, using contains(x, y).
 */
public final class UiButton extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** The button's background. */
    private final GRect box;

    /** The button's text. */
    private final GLabel label;

    /** Whether the button can be clicked. */
    private boolean enabled = true;

    /**
     * Builds a button.
     *
     * @param text the label
     * @param width width in pixels
     * @param height height in pixels
     */
    public UiButton(String text, double width, double height) {
        box = new GRect(0, 0, width, height);
        box.setFilled(true);
        add(box);
        label = new GLabel(text);
        label.setFont(Palette.HUD);
        add(label, (width - label.getWidth()) / 2, (height + label.getAscent()) / 2 - 2);
        setHighlighted(false);
    }

    /**
     * Changes the button's text and centers it again.
     *
     * @param text the new label
     */
    public void setLabel(String text) {
        label.setLabel(text);
        label.setLocation((box.getWidth() - label.getWidth()) / 2, label.getY());
    }

    /**
     * Shows the button as chosen or not.
     *
     * @param highlighted true to highlight
     */
    public void setHighlighted(boolean highlighted) {
        Color fill = !enabled ? Palette.BUTTON_DISABLED : highlighted ? Palette.PLAYER : Palette.BUTTON;
        box.setFillColor(fill);
        box.setColor(highlighted ? Palette.TEXT : Palette.PANEL_EDGE);
        label.setColor(highlighted ? Palette.CAR_OUTLINE : enabled ? Palette.TEXT : Palette.TEXT_DIM);
    }

    /**
     * Enables or disables the button.
     *
     * @param value true to enable
     */
    public void setEnabled(boolean value) {
        enabled = value;
        setHighlighted(false);
    }

    /**
     * Returns whether the button can be clicked.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
