import acm.graphics.GCompound;
import acm.graphics.GLine;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.Locale;

/**
 * A bar chart drawn with ACM shapes. Bars grow up or down from a zero line, so it can show both
 * gains and losses; each bar has a name below it and its value written beside it.
 */
public final class BarChart extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Space at the top for the title, in pixels. */
    private static final double TOP = 30;

    /** Space at the bottom for the bar names, in pixels. */
    private static final double BOTTOM = 22;

    /** Room kept beyond the longest bar for its value label, in pixels. */
    private static final double LABEL_ROOM = 16;

    /**
     * Draws a bar chart.
     *
     * @param title chart title
     * @param width total width in pixels
     * @param height total height in pixels
     * @param names name under each bar
     * @param values value of each bar (may be negative)
     * @param format format for the value labels, such as "%+.2f"
     * @param good color for values that are good
     * @param bad color for values that are bad
     * @param lowerIsBetter true if smaller (or negative) values are the good ones
     */
    public BarChart(String title, double width, double height, String[] names, double[] values, String format,
                    Color good, Color bad, boolean lowerIsBetter) {
        add(Palette.panel(width, height));
        add(Palette.label(title, Palette.HUD, Palette.TEXT), 10, 20);
        double maxAbs = 1e-9;
        boolean anyNegative = false;
        for (double v : values) {
            maxAbs = Math.max(maxAbs, Math.abs(v));
            anyNegative |= v < 0;
        }
        double plotH = height - TOP - BOTTOM;
        double zero = anyNegative ? TOP + plotH / 2 : TOP + plotH;
        double scale = ((anyNegative ? plotH / 2 : plotH) - LABEL_ROOM) / maxAbs;
        GLine axis = new GLine(10, zero, width - 10, zero);
        axis.setColor(Palette.TEXT_DIM);
        add(axis);
        double slot = (width - 20) / Math.max(1, values.length);
        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            double h = Math.abs(v) * scale;
            double x = 10 + i * slot + slot * 0.2;
            GRect bar = new GRect(x, v >= 0 ? zero - h : zero, slot * 0.6, Math.max(1, h));
            bar.setFilled(true);
            boolean isGood = lowerIsBetter ? v <= 0 : v >= 0;
            bar.setFillColor(isGood ? good : bad);
            bar.setColor(isGood ? good : bad);
            add(bar);
            add(Palette.label(names[i], Palette.SMALL, Palette.TEXT_DIM), x, height - 6);
            double labelY = v >= 0 ? zero - h - 4 : zero + h + 12;
            add(Palette.label(String.format(Locale.ROOT, format, v), Palette.SMALL, Palette.TEXT), x, labelY);
        }
    }
}
