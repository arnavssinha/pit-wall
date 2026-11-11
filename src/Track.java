import java.awt.Graphics2D;

public class Track {
    private double radius;
    private double trackWidth;

    public Track(double radius, double trackWidth) {
        this.radius = radius;
        this.trackWidth = trackWidth;
    }

    /**
     * Parametric X coordinate: x(t) = R * cos(t)
     */
    public double getX(double t) {
        return radius * Math.cos(t);
    }

    /**
     * Parametric Y coordinate: y(t) = R * sin(t)
     */
    public double getY(double t) {
        return radius * Math.sin(t);
    }

    /**
     * Heading tangent to the circle: heading = t + PI/2
     */
    public double getHeading(double t) {
        return t + (Math.PI / 2.0);
    }

    /**
     * Draws the inner and outer boundaries of the track along with the center trajectory.
     */
    public void draw(Graphics2D g2d, int cx, int cy, double scale) {
        double rOuter = radius + (trackWidth / 2.0);
        double rInner = radius - (trackWidth / 2.0);

        // Draw outer boundary
        int outerDiameter = (int) (rOuter * 2 * scale);
        g2d.drawOval(cx - outerDiameter / 2, cy - outerDiameter / 2, outerDiameter, outerDiameter);

        // Draw inner boundary
        int innerDiameter = (int) (rInner * 2 * scale);
        g2d.drawOval(cx - innerDiameter / 2, cy - innerDiameter / 2, innerDiameter, innerDiameter);

        // Draw centerline (center trajectory)
        int centerDiameter = (int) (radius * 2 * scale);
        g2d.drawOval(cx - centerDiameter / 2, cy - centerDiameter / 2, centerDiameter, centerDiameter);
    }

    public double getRadius() { return radius; }
    public double getTrackWidth() { return trackWidth; }
}
