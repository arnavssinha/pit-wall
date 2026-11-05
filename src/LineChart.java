import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.Locale;

/**
 * A simple line chart drawn with ACM shapes: a framed plot area, a few horizontal grid lines with
 * values, lap numbers along the bottom, and one colored line (with dots) per series, plus a legend.
 * Missing values (NaN) leave a gap in the line.
 */
public final class LineChart extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Space on the left for the value labels, in pixels. */
    private static final double LEFT = 46;

    /** Space at the top for the title, in pixels. */
    private static final double TOP = 26;

    /** Space at the bottom for the lap numbers, in pixels. */
    private static final double BOTTOM = 22;

    /** Number of horizontal grid lines. */
    private static final int GRID_LINES = 4;

    /** Width of the plot area. */
    private final double plotW;

    /** Height of the plot area. */
    private final double plotH;

    /** Smallest value on the vertical axis. */
    private final double yMin;

    /** Largest value on the vertical axis. */
    private final double yMax;

    /** Number of points along the horizontal axis. */
    private final int points;

    /** Where the next legend entry goes, along the title row. */
    private double legendX;

    /**
     * Builds an empty chart.
     *
     * @param title chart title
     * @param width total width in pixels
     * @param height total height in pixels
     * @param yMin smallest value shown
     * @param yMax largest value shown
     * @param format format for the value labels, such as "%.0f%%"
     * @param scale multiplier applied to values before formatting the labels (for example 100 for percent)
     * @param points number of points (laps) along the horizontal axis
     */
    public LineChart(String title, double width, double height, double yMin, double yMax, String format,
                     double scale, int points) {
        this.plotW = width - LEFT - 10;
        this.plotH = height - TOP - BOTTOM;
        this.yMin = yMin;
        this.yMax = yMax;
        this.points = points;
        add(Palette.panel(width, height));
        GLabel titleLabel = Palette.label(title, Palette.HUD, Palette.TEXT);
        add(titleLabel, 10, 18);
        legendX = 10 + titleLabel.getWidth() + 16;
        GRect area = new GRect(LEFT, TOP, plotW, plotH);
        area.setColor(Palette.PANEL_EDGE);
        add(area);
        for (int i = 0; i <= GRID_LINES; i++) {
            double value = yMin + (yMax - yMin) * i / GRID_LINES;
            double y = toY(value);
            GLine grid = new GLine(LEFT, y, LEFT + plotW, y);
            grid.setColor(Palette.GAUGE_BACK);
            add(grid);
            add(Palette.label(String.format(Locale.ROOT, format, value * scale), Palette.SMALL, Palette.TEXT_DIM), 6, y + 4);
        }
        for (int i = 0; i < points; i++) {
            add(Palette.label(String.valueOf(i + 1), Palette.SMALL, Palette.TEXT_DIM), toX(i) - 4, TOP + plotH + 16);
        }
    }

    /**
     * Draws one series.
     *
     * @param values one value per point (NaN for none)
     * @param color line color
     * @param name name shown in the legend
     */
    public void addSeries(double[] values, Color color, String name) {
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                continue;
            }
            if (i + 1 < values.length && !Double.isNaN(values[i + 1])) {
                GLine segment = new GLine(toX(i), toY(values[i]), toX(i + 1), toY(values[i + 1]));
                segment.setColor(color);
                add(segment);
            }
            GOval dot = new GOval(toX(i) - 3, toY(values[i]) - 3, 6, 6);
            dot.setFilled(true);
            dot.setFillColor(color);
            dot.setColor(color);
            add(dot);
        }
        GLabel legend = Palette.label(name, Palette.SMALL, color);
        add(legend, legendX, 18);
        legendX += legend.getWidth() + 12;
    }

    /**
     * Converts a point number to an x position.
     *
     * @param i point index from 0
     * @return x in pixels
     */
    private double toX(int i) {
        return LEFT + (points <= 1 ? plotW / 2 : plotW * i / (points - 1));
    }

    /**
     * Converts a value to a y position, clamped to the plot area.
     *
     * @param value the value
     * @return y in pixels
     */
    private double toY(double value) {
        double t = (value - yMin) / (yMax - yMin);
        t = Math.max(0, Math.min(1, t));
        return TOP + plotH * (1 - t);
    }
}
