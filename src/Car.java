import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class Car {
    private double length;
    private double width;
    
    // Position and orientation (in physics world units)
    private double x;
    private double y;
    private double angle; // angle in radians

    public Car(double length, double width) {
        this.length = length;
        this.width = width;
        this.x = 0;
        this.y = 0;
        this.angle = 0;
    }

    /**
     * Updates the car's position and orientation to follow the track's parametric curve.
     *
     * @param t Parameter angle along the track (radians)
     * @param track The track being traversed
     */
    public void update(double t, Track track) {
        this.x = track.getX(t);
        this.y = track.getY(t);
        this.angle = track.getHeading(t);
    }

    /**
     * Renders the car on screen with proper position, rotation, and scale.
     */
    public void draw(Graphics2D g2d, int cx, int cy, double scale) {
        // Save current transform
        AffineTransform oldTransform = g2d.getTransform();

        // Screen coordinate translation (flip Y axis for standard math coordinates)
        double screenX = cx + (x * scale);
        double screenY = cy - (y * scale);

        g2d.translate(screenX, screenY);
        g2d.rotate(-angle); // Rotate to match car heading

        // Draw car rectangle centered at origin
        int drawW = (int) (length * scale);
        int drawH = (int) (width * scale);

        g2d.drawRect(-drawW / 2, -drawH / 2, drawW, drawH);

        // Restore transform
        g2d.setTransform(oldTransform);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
}
