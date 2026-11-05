import acm.graphics.GOval;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The drawing of one car, seen from above: four tires, front and rear wings, a team-colored body
 * with sidepods, a center stripe, the cockpit and the driver's helmet.
 * ACM compounds cannot rotate, so every part is its own polygon built around the same center
 * point (the car's middle). Moving all parts to the car's position and rotating each by the same
 * angle turns the whole car. Parts are built once and only moved and turned each frame.
 * Sizes are in drawing units (see TrackDrawing); the world zoom scales them to pixels.
 */
public final class CarSprite {

    /**
     * Smallest length a car is drawn on screen, in pixels. When zoomed far out the parts are
     * enlarged to this size so cars stay visible in the whole-track view. Drawing choice.
     */
    private static final double MIN_SCREEN_LENGTH = 12;

    /** Tire color. */
    private static final Color TIRE = new Color(22, 22, 26);

    /** Cockpit and wing end-plate color. */
    private static final Color CARBON = new Color(35, 37, 44);

    /** The car this sprite draws. */
    private final Car car;

    /** Every part, in drawing order. */
    private final List<GPolygon> parts = new ArrayList<GPolygon>();

    /** Yellow ring around the player's car, or null for other cars. */
    private final GOval ring;

    /** Red warning triangle shown above a rival that is about to attack the player. */
    private final GPolygon alert;

    /** Rotation currently applied to the parts, in degrees. */
    private double appliedDegrees;

    /** Extra enlargement currently applied on top of the world zoom (1 means normal size). */
    private double appliedEnlargement;

    /**
     * Builds the sprite.
     *
     * @param car the car to draw
     * @param isPlayer true to add the yellow ring and helmet that mark the player's car
     */
    public CarSprite(Car car, boolean isPlayer) {
        this.car = car;
        Color team = Palette.teamColor(car.getDriver().getTeam());
        double l = TrackDrawing.CAR_LENGTH / 2;
        double w = TrackDrawing.CAR_WIDTH / 2;

        parts.add(rect(0.62 * l, 0.80 * l, -w, -0.62 * w, TIRE));
        parts.add(rect(0.62 * l, 0.80 * l, 0.62 * w, w, TIRE));
        parts.add(rect(-0.82 * l, -0.58 * l, -w, -0.58 * w, TIRE));
        parts.add(rect(-0.82 * l, -0.58 * l, 0.58 * w, w, TIRE));

        GPolygon body = new GPolygon();
        double[][] outline = {
            {0.92 * l, 0}, {0.55 * l, 0.16 * w}, {0.25 * l, 0.30 * w}, {0.12 * l, 0.62 * w},
            {-0.45 * l, 0.62 * w}, {-0.70 * l, 0.32 * w}, {-0.88 * l, 0.28 * w},
            {-0.88 * l, -0.28 * w}, {-0.70 * l, -0.32 * w}, {-0.45 * l, -0.62 * w},
            {0.12 * l, -0.62 * w}, {0.25 * l, -0.30 * w}, {0.55 * l, -0.16 * w},
        };
        for (double[] p : outline) {
            body.addVertex(p[0], p[1]);
        }
        style(body, team, team.darker());
        parts.add(body);

        parts.add(rect(0.84 * l, 1.0 * l, -0.95 * w, 0.95 * w, team.darker()));
        parts.add(rect(-1.0 * l, -0.86 * l, -0.78 * w, 0.78 * w, CARBON));
        parts.add(rect(-0.80 * l, 0.80 * l, -0.06 * w, 0.06 * w, Palette.EDGE_LINE));
        parts.add(rect(-0.18 * l, 0.22 * l, -0.24 * w, 0.24 * w, CARBON));

        GPolygon helmet = new GPolygon();
        double r = 0.17 * w;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            helmet.addVertex(0.02 * l + r * Math.cos(a), r * Math.sin(a));
        }
        style(helmet, isPlayer ? Palette.PLAYER : Color.WHITE, CARBON);
        parts.add(helmet);

        if (isPlayer) {
            ring = new GOval(TrackDrawing.CAR_LENGTH * 1.5, TrackDrawing.CAR_LENGTH * 1.5);
            ring.setColor(Palette.PLAYER);
            ring.setFilled(false);
        } else {
            ring = null;
        }
        alert = new GPolygon();
        double s = TrackDrawing.CAR_WIDTH;
        alert.addVertex(0, -s);
        alert.addVertex(s, s * 0.8);
        alert.addVertex(-s, s * 0.8);
        style(alert, Palette.DANGER, Color.WHITE);
        alert.setVisible(false);
        appliedDegrees = 0;
        appliedEnlargement = 1;
    }

    /**
     * Returns the attack warning triangle, so the world view can add it on top of the cars.
     *
     * @return the triangle
     */
    public GPolygon getAlert() {
        return alert;
    }

    /**
     * Shows or hides the attack warning above this car.
     *
     * @param visible true to show
     */
    public void setAlert(boolean visible) {
        if (alert.isVisible() != visible) {
            alert.setVisible(visible);
        }
    }

    /**
     * Builds a rectangle part around the car's center.
     *
     * @param back rear edge along the car (negative is toward the back)
     * @param front front edge along the car
     * @param right one side edge across the car
     * @param left other side edge across the car
     * @param color fill color
     * @return the polygon
     */
    private static GPolygon rect(double back, double front, double right, double left, Color color) {
        GPolygon poly = new GPolygon();
        poly.addVertex(front, right);
        poly.addVertex(front, left);
        poly.addVertex(back, left);
        poly.addVertex(back, right);
        style(poly, color, color);
        return poly;
    }

    /**
     * Fills a polygon and sets its outline color.
     *
     * @param poly the polygon
     * @param fill fill color
     * @param outline outline color
     */
    private static void style(GPolygon poly, Color fill, Color outline) {
        poly.setFilled(true);
        poly.setFillColor(fill);
        poly.setColor(outline);
    }

    /**
     * Returns every part, in drawing order, so the world view can add them.
     *
     * @return the parts
     */
    public List<GPolygon> getParts() {
        return parts;
    }

    /**
     * Returns the highlight ring, which only the player's car has.
     *
     * @return the ring, or null
     */
    public GOval getRing() {
        return ring;
    }

    /**
     * Returns the car this sprite draws.
     *
     * @return the car
     */
    public Car getCar() {
        return car;
    }

    /**
     * Moves and turns every part to match the car.
     * Screen y points down while track y points north, so y is flipped. ACM rotation is
     * counter-clockwise on screen, which matches the track heading after that flip.
     *
     * @param drawing converts track positions to drawing units
     * @param zoom pixels per drawing unit
     */
    public void update(TrackDrawing drawing, double zoom) {
        double d = car.getDistance();
        double lateral = car.getLateralOffset();
        double px = drawing.x(d, lateral) * zoom;
        double py = -drawing.y(d, lateral) * zoom;

        double enlargement = Math.max(1.0, MIN_SCREEN_LENGTH / (TrackDrawing.CAR_LENGTH * zoom));
        boolean rescale = Math.abs(enlargement / appliedEnlargement - 1) > 0.02;
        double heading = drawing.getTrack().headingAt(d);
        double turn = Math.toDegrees(Track.angleDifference(heading, Math.toRadians(appliedDegrees)));
        boolean rotate = Math.abs(turn) > 0.05;

        for (GPolygon part : parts) {
            part.setLocation(px, py);
            if (rescale) {
                part.scale(enlargement / appliedEnlargement);
            }
            if (rotate) {
                part.rotate(turn);
            }
        }
        if (rotate) {
            appliedDegrees += turn;
        }
        if (ring != null) {
            if (rescale) {
                ring.scale(enlargement / appliedEnlargement);
            }
            ring.setLocation(px - ring.getWidth() / 2, py - ring.getHeight() / 2);
        }
        if (rescale) {
            alert.scale(enlargement / appliedEnlargement);
            appliedEnlargement = enlargement;
        }
        if (alert.isVisible()) {
            alert.setLocation(px, py - TrackDrawing.CAR_LENGTH * zoom * appliedEnlargement);
        }
    }
}
