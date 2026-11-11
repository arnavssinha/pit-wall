import acm.graphics.GCanvas;
import acm.graphics.GOval;
import java.awt.Color;

/**
 * Circular track geometry and its simple ACM drawing.
 *
 * @author @harnoor
 * @author @samuel
 */
public final class Track {
    private final double radius;
    private final double trackWidth;

    /**
     * Creates a circular track.
     *
     * @author @harnoor
     * @param radius centerline radius in world units
     * @param trackWidth drivable width in world units
     */
    public Track(double radius, double trackWidth) {
        if (radius <= 0 || trackWidth <= 0 || trackWidth >= 2 * radius) {
            throw new IllegalArgumentException("Invalid track radius or width.");
        }
        this.radius = radius;
        this.trackWidth = trackWidth;
    }

    /**
     * Calculates centerline X with a parametric circle.
     *
     * @author @harnoor
     * @param t angle in radians
     * @return world X coordinate
     */
    public double getX(double t) {
        return radius * Math.cos(t);
    }

    /**
     * Calculates centerline Y with a parametric circle.
     *
     * @author @harnoor
     * @param t angle in radians
     * @return world Y coordinate
     */
    public double getY(double t) {
        return radius * Math.sin(t);
    }

    /**
     * Calculates the tangent direction of travel.
     *
     * @author @harnoor
     * @param t angle in radians
     * @return heading in radians
     */
    public double getHeading(double t) {
        return t + Math.PI / 2.0;
    }

    /**
     * Samples positions around the centerline into a two-dimensional array.
     *
     * @author @harnoor
     * @param count number of points
     * @return count-by-two array of X and Y coordinates
     */
    public double[][] sampleCenterline(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Sample count must be positive.");
        }
        double[][] points = new double[count][2];
        for (int i = 0; i < count; i++) {
            double t = i * 2.0 * Math.PI / count;
            points[i][0] = getX(t);
            points[i][1] = getY(t);
        }
        return points;
    }

    /**
     * Draws the track band and centerline using ACM GOval objects.
     *
     * @author @samuel
     * @param canvas ACM canvas
     * @param cx screen X of the track center
     * @param cy screen Y of the track center
     * @param scale pixels per world unit
     */
    public void draw(GCanvas canvas, double cx, double cy, double scale) {
        double outer = (radius + trackWidth / 2.0) * scale;
        double inner = (radius - trackWidth / 2.0) * scale;
        double middle = radius * scale;

        GOval outside = new GOval(cx - outer, cy - outer, 2 * outer, 2 * outer);
        outside.setFilled(true);
        outside.setFillColor(new Color(73, 89, 108));
        outside.setColor(new Color(130, 152, 176));
        canvas.add(outside);

        GOval inside = new GOval(cx - inner, cy - inner, 2 * inner, 2 * inner);
        inside.setFilled(true);
        inside.setFillColor(new Color(17, 24, 37));
        inside.setColor(new Color(130, 152, 176));
        canvas.add(inside);

        GOval center = new GOval(cx - middle, cy - middle, 2 * middle, 2 * middle);
        center.setColor(new Color(192, 208, 222));
        canvas.add(center);
    }

    /**
     * Returns the centerline radius.
     *
     * @author @harnoor
     * @return radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Returns the drivable track width.
     *
     * @author @harnoor
     * @return width
     */
    public double getTrackWidth() {
        return trackWidth;
    }
}
