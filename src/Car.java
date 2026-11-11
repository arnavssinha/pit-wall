import acm.graphics.GCanvas;
import acm.graphics.GPolygon;
import java.awt.Color;

/**
 * A rectangular car on the circular track; world Y points up.
 *
 * @author @harnoor
 * @author @samuel
 */
public final class Car {
    private final double length;
    private final double width;
    private double x;
    private double y;
    private double angle;

    /**
     * Creates a car with the specified dimensions.
     *
     * @author @harnoor
     * @param length car length in world units
     * @param width car width in world units
     */
    public Car(double length, double width) {
        if (length <= 0 || width <= 0) {
            throw new IllegalArgumentException("Car dimensions must be positive.");
        }
        this.length = length;
        this.width = width;
    }

    /**
     * Moves the car to the track centerline at angle t.
     *
     * @author @harnoor
     * @param t angle along the track in radians
     * @param track track being traversed
     */
    public void update(double t, Track track) {
        x = track.getX(t);
        y = track.getY(t);
        angle = track.getHeading(t);
    }

    /**
     * Draws the car with ACM Graphics, returning the shape for later removal.
     *
     * @author @samuel
     * @param canvas ACM canvas
     * @param cx screen X of the track center
     * @param cy screen Y of the track center
     * @param scale pixels per world unit
     * @return drawn car polygon
     */
    public GPolygon draw(GCanvas canvas, double cx, double cy, double scale) {
        GPolygon shape = new GPolygon(cx + x * scale, cy - y * scale);
        double halfLength = length * scale / 2.0;
        double halfWidth = width * scale / 2.0;

        addCorner(shape, -halfLength, -halfWidth);
        addCorner(shape, halfLength, -halfWidth);
        addCorner(shape, halfLength, halfWidth);
        addCorner(shape, -halfLength, halfWidth);

        shape.setFilled(true);
        shape.setFillColor(new Color(30, 207, 215));
        shape.setColor(Color.WHITE);
        canvas.add(shape);
        return shape;
    }

    /**
     * Adds a rotated rectangle corner and flips mathematical Y for screen Y.
     *
     * @author @samuel
     * @param polygon the car polygon
     * @param forward distance along the car's length
     * @param sideways distance across the car's width
     */
    private void addCorner(GPolygon polygon, double forward, double sideways) {
        double dx = forward * Math.cos(angle) - sideways * Math.sin(angle);
        double dy = -forward * Math.sin(angle) - sideways * Math.cos(angle);
        polygon.addVertex(dx, dy);
    }

    /**
     * Returns the car X coordinate.
     *
     * @author @harnoor
     * @return world X coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the car Y coordinate.
     *
     * @author @harnoor
     * @return world Y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the car heading.
     *
     * @author @harnoor
     * @return heading in radians
     */
    public double getAngle() {
        return angle;
    }
}
