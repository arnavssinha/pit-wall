import acm.graphics.GCanvas;
import acm.graphics.GOval;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the circular track and its centerline.
 * The geometry is unchanged from the original simplified branch.
 *
 * @author @harnoor
 * @author @samuel
 * @author @arnav
 */
public class Track {
    private double radius;
    private double trackWidth;

    // Keep references to ACM graphics so repeated draws replace old outlines.
    private final List<GOval> renderedBoundaries = new ArrayList<GOval>();
    private GCanvas renderedOn;

    /**
     * Creates a circular track with the given radius and width.
     *
     * @author @harnoor
     * @param radius radius of the center trajectory
     * @param trackWidth width of the track
     */
    public Track(double radius, double trackWidth) {
        this.radius = radius;
        this.trackWidth = trackWidth;
    }

    /**
     * Calculates the parametric X coordinate: x(t) = R * cos(t).
     *
     * @author @harnoor
     * @param t angular position, in radians
     * @return centerline X coordinate
     */
    public double getX(double t) {
        return radius * Math.cos(t);
    }

    /**
     * Calculates the parametric Y coordinate: y(t) = R * sin(t).
     *
     * @author @harnoor
     * @param t angular position, in radians
     * @return centerline Y coordinate
     */
    public double getY(double t) {
        return radius * Math.sin(t);
    }

    /**
     * Calculates the heading tangent to the circle: t + PI / 2.
     *
     * @author @harnoor
     * @param t angular position, in radians
     * @return direction of travel, in radians
     */
    public double getHeading(double t) {
        return t + (Math.PI / 2.0);
    }

    /**
     * Draws the outer boundary, inner boundary, and center trajectory using
     * ACM Graphics. Uses the original diameter calculations and coordinates.
     *
     * @author @samuel
     * @param canvas ACM canvas on which to draw the track
     * @param cx horizontal screen coordinate of the track center
     * @param cy vertical screen coordinate of the track center
     * @param scale pixels per world unit
     */
    public void draw(GCanvas canvas, int cx, int cy, double scale) {
        // An ACM canvas retains shapes, so erase previous outlines if redrawn.
        if (renderedOn != null) {
            for (GOval boundary : renderedBoundaries) {
                renderedOn.remove(boundary);
            }
        }
        renderedBoundaries.clear();

        double rOuter = radius + (trackWidth / 2.0);
        double rInner = radius - (trackWidth / 2.0);

        int outerDiameter = (int) (rOuter * 2 * scale);
        drawBoundary(canvas, cx, cy, outerDiameter);

        int innerDiameter = (int) (rInner * 2 * scale);
        drawBoundary(canvas, cx, cy, innerDiameter);

        int centerDiameter = (int) (radius * 2 * scale);
        drawBoundary(canvas, cx, cy, centerDiameter);

        renderedOn = canvas;
    }

    /**
     * Adds one outline circle to the ACM canvas and tracks it for redrawing.
     *
     * @author @samuel
     * @param canvas canvas receiving the circle
     * @param cx horizontal coordinate of the circle center
     * @param cy vertical coordinate of the circle center
     * @param diameter diameter in screen pixels
     */
    private void drawBoundary(GCanvas canvas, int cx, int cy, int diameter) {
        GOval boundary = new GOval(
                cx - diameter / 2, cy - diameter / 2, diameter, diameter);
        boundary.setFilled(false);
        canvas.add(boundary);
        renderedBoundaries.add(boundary);
    }

    /**
     * Returns the centerline radius.
     *
     * @author @arnav
     * @return the track radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Returns the track width.
     *
     * @author @arnav
     * @return the track width
     */
    public double getTrackWidth() {
        return trackWidth;
    }
}
