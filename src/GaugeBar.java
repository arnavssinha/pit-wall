import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.awt.Color;

/**
 * One horizontal gauge: a name on the left, a bar in the middle and the value on the right.
 * It works in two styles. A fill gauge (wear, battery) fills from the left. A needle gauge
 * (temperatures) shows a thin marker, with an optional green band marking the ideal range.
 */
public final class GaugeBar extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Width of the name column in pixels. */
    private static final double NAME_WIDTH = 62;

    /** Width of the bar in pixels. */
    private static final double BAR_WIDTH = 116;

    /** Height of the bar in pixels. */
    private static final double BAR_HEIGHT = 12;

    /** Width of the needle in pixels. */
    private static final double NEEDLE_WIDTH = 3;

    /** Smallest value the bar shows. */
    private final double min;

    /** Largest value the bar shows. */
    private final double max;

    /** True for a needle gauge, false for a fill gauge. */
    private final boolean needle;

    /** The green ideal-range band, used by needle gauges. */
    private final GRect band;

    /** The filled part (fill gauges) or the needle (needle gauges). */
    private final GRect marker;

    /** The value text on the right. */
    private final GLabel valueLabel;

    /**
     * Builds a gauge.
     *
     * @param name label on the left
     * @param min smallest value shown
     * @param max largest value shown
     * @param needle true for a needle gauge, false for a fill gauge
     */
    public GaugeBar(String name, double min, double max, boolean needle) {
        this.min = min;
        this.max = max;
        this.needle = needle;
        GLabel nameLabel = new GLabel(name, 0, BAR_HEIGHT);
        nameLabel.setFont(Palette.SMALL);
        nameLabel.setColor(Palette.TEXT_DIM);
        add(nameLabel);
        GRect background = new GRect(NAME_WIDTH, 0, BAR_WIDTH, BAR_HEIGHT);
        background.setFilled(true);
        background.setFillColor(Palette.GAUGE_BACK);
        background.setColor(Palette.PANEL_EDGE);
        add(background);
        band = new GRect(NAME_WIDTH, 0, 0, BAR_HEIGHT);
        band.setFilled(true);
        band.setFillColor(Palette.GAUGE_BAND);
        band.setColor(Palette.GAUGE_BAND);
        add(band);
        marker = new GRect(NAME_WIDTH, 0, 0, BAR_HEIGHT);
        marker.setFilled(true);
        add(marker);
        valueLabel = new GLabel("", NAME_WIDTH + BAR_WIDTH + 8, BAR_HEIGHT);
        valueLabel.setFont(Palette.MONO);
        valueLabel.setColor(Palette.TEXT);
        add(valueLabel);
    }

    /**
     * Sets the green ideal-range band.
     *
     * @param low bottom of the ideal range
     * @param high top of the ideal range
     */
    public void setBand(double low, double high) {
        double x0 = toX(low);
        double x1 = toX(high);
        band.setBounds(x0, 0, Math.max(0, x1 - x0), BAR_HEIGHT);
    }

    /**
     * Shows a value.
     *
     * @param value the value, in the gauge's range
     * @param text the value as text, shown on the right
     * @param color color of the fill or needle
     */
    public void setValue(double value, String text, Color color) {
        double x = toX(value);
        if (needle) {
            marker.setBounds(x - NEEDLE_WIDTH / 2, -2, NEEDLE_WIDTH, BAR_HEIGHT + 4);
        } else {
            marker.setBounds(NAME_WIDTH, 0, x - NAME_WIDTH, BAR_HEIGHT);
        }
        if (!color.equals(marker.getFillColor())) {
            marker.setFillColor(color);
            marker.setColor(color);
        }
        if (!text.equals(valueLabel.getLabel())) {
            valueLabel.setLabel(text);
        }
    }

    /**
     * Converts a value to an x position on the bar, clamped to the bar's ends.
     *
     * @param value the value
     * @return x in pixels inside this compound
     */
    private double toX(double value) {
        double t = Math.max(0, Math.min(1, (value - min) / (max - min)));
        return NAME_WIDTH + t * BAR_WIDTH;
    }
}
