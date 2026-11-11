import acm.graphics.GCanvas;
import acm.graphics.GPolygon;

/**
 * Represents a car moving around a track.
 * Position and heading use the same world coordinates as the original version.
 *
 * @author @harnoor
 * @author @samuel
 * @author @arnav
 */
public class Car {
    private double length;
    private double width;

    // Position and orientation (in physics world units).
    private double x;
    private double y;
    private double angle; // Angle in radians.

    // ACM keeps graphics objects on the canvas until they are removed.
    private GPolygon renderedCar;
    private GCanvas renderedOn;

    /**
     * Creates a car at the origin, initially facing angle zero.
     *
     * @author @harnoor
     * @param length the car's length in world units
     * @param width the car's width in world units
     */
    public Car(double length, double width) {
        this.length = length;
        this.width = width;
        this.x = 0;
        this.y = 0;
        this.angle = 0;
    }

    /**
     * Updates the car's position and orientation to follow the track's
     * parametric curve.
     *
     * @author @harnoor
     * @param t parameter angle along the track, in radians
     * @param track the track being traversed
     */
    public void update(double t, Track track) {
        this.x = track.getX(t);
        this.y = track.getY(t);
        this.angle = track.getHeading(t);
    }

    /**
     * Draws the same centered, rotated outline rectangle as before, using ACM
     * Graphics instead of Graphics2D. Removes the previous ACM shape so drawing
     * successive frames does not leave old car outlines on the canvas.
     *
     * @author @samuel
     * @param canvas ACM canvas on which to draw the car
     * @param cx horizontal screen coordinate of the world origin
     * @param cy vertical screen coordinate of the world origin
     * @param scale pixels per world unit
     */
    public void draw(GCanvas canvas, int cx, int cy, double scale) {
        if (renderedCar != null && renderedOn != null) {
            renderedOn.remove(renderedCar);
        }

        // Keep the original world-to-screen transformation.
        double screenX = cx + (x * scale);
        double screenY = cy - (y * scale);

        // Keep the original integer pixel dimensions and rectangle placement.
        int drawW = (int) (length * scale);
        int drawH = (int) (width * scale);
        int left = -drawW / 2;
        int top = -drawH / 2;

        GPolygon outline = new GPolygon(screenX, screenY);
        addRotatedCorner(outline, left, top);
        addRotatedCorner(outline, left + drawW, top);
        addRotatedCorner(outline, left + drawW, top + drawH);
        addRotatedCorner(outline, left, top + drawH);
        outline.setFilled(false);

        canvas.add(outline);
        renderedCar = outline;
        renderedOn = canvas;
    }

    /**
     * Adds one corner rotated by the car's heading. The negative screen
     * rotation matches the original Graphics2D implementation.
     *
     * @author @samuel
     * @param outline polygon to which the corner is added
     * @param localX corner X before rotation
     * @param localY corner Y before rotation
     */
    private void addRotatedCorner(GPolygon outline, double localX, double localY) {
        double dx = localX * Math.cos(angle) + localY * Math.sin(angle);
        double dy = -localX * Math.sin(angle) + localY * Math.cos(angle);
        outline.addVertex(dx, dy);
    }

    /**
     * Returns the car's world X coordinate.
     *
     * @author @arnav
     * @return world X coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the car's world Y coordinate.
     *
     * @author @arnav
     * @return world Y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the current heading angle.
     *
     * @author @arnav
     * @return heading in radians
     */
    public double getAngle() {
        return angle;
    }
}
