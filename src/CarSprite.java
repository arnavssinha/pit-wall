import acm.graphics.GOval;
import acm.graphics.GObject;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
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

    /** Most the drawn car may rotate in one frame, in degrees, so it can never appear to spin. */
    private static final double MAX_TURN_PER_FRAME = 12.0;

    /** Tire color. */
    private static final Color TIRE = new Color(22, 22, 26);

    /** Cockpit and wing end-plate color. */
    private static final Color CARBON = new Color(35, 37, 44);

    /** Lock-up smoke. */
    private static final Color SMOKE = new Color(235, 235, 235, 170);

    /** Size of the lock-up smoke puff in pixels. */
    private static final double SMOKE_SIZE = 9;

    /** Width of the tire-condition bar in pixels. */
    private static final double WEAR_BAR_WIDTH = 24;

    /** Height of the tire-condition bar in pixels. */
    private static final double WEAR_BAR_HEIGHT = 3;

    /** Frames per blink of a punctured tire. */
    private static final int PUNCTURE_BLINK_FRAMES = 8;

    /** The car this sprite draws. */
    private final Car car;

    /** Every part, in drawing order. */
    private final List<GPolygon> parts = new ArrayList<GPolygon>();

    /** Yellow ring around the player's car, or null for other cars. */
    private final GOval ring;

    /** Blue exhaust flame shown behind the car while it deploys ERS. */
    private final GPolygon glow;

    /** The rear wing, which turns bright green while the DRS flap is open. */
    private final GPolygon rearWing;

    /** Whether the rear wing is currently drawn open. */
    private boolean wingOpen;

    /** Red warning triangle shown above a rival that is about to attack the player. */
    private final GPolygon alert;

    /** The four tires: their outline shows the compound color, and a punctured one turns red. */
    private final GPolygon[] tires = new GPolygon[4];

    /** Compound currently shown on the tires. */
    private Compound shownCompound;

    /** Background of the tire-condition bar under the car. */
    private final GRect wearBack;

    /** Tire-condition bar: full and green on new tires, short and red when worn out. */
    private final GRect wearBar;

    /** Puff of tire smoke shown when the car locks up. */
    private final GOval smoke;

    /** Red ring marking the player's rival during a fight. */
    private final GOval rivalRing;

    /** Frames drawn, used to make a punctured tire blink. */
    private int frames;

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

        tires[0] = rect(0.62 * l, 0.80 * l, -w, -0.62 * w, TIRE);
        tires[1] = rect(0.62 * l, 0.80 * l, 0.62 * w, w, TIRE);
        tires[2] = rect(-0.82 * l, -0.58 * l, -w, -0.58 * w, TIRE);
        tires[3] = rect(-0.82 * l, -0.58 * l, 0.58 * w, w, TIRE);
        for (GPolygon tire : tires) {
            parts.add(tire);
        }

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
        rearWing = rect(-1.0 * l, -0.86 * l, -0.78 * w, 0.78 * w, CARBON);
        parts.add(rearWing);
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
        glow = new GPolygon();
        glow.addVertex(-0.85 * l, -0.5 * w);
        glow.addVertex(-2.4 * l, 0);
        glow.addVertex(-0.85 * l, 0.5 * w);
        style(glow, Palette.ERS_GLOW, Palette.ERS);
        glow.setVisible(false);
        alert = new GPolygon();
        double s = TrackDrawing.CAR_WIDTH;
        alert.addVertex(0, -s);
        alert.addVertex(s, s * 0.8);
        alert.addVertex(-s, s * 0.8);
        style(alert, Palette.DANGER, Color.WHITE);
        alert.setVisible(false);
        appliedDegrees = 0;
        appliedEnlargement = 1;
        wearBack = new GRect(WEAR_BAR_WIDTH + 2, WEAR_BAR_HEIGHT + 2);
        wearBack.setFilled(true);
        wearBack.setFillColor(Palette.CAR_OUTLINE);
        wearBack.setColor(Palette.CAR_OUTLINE);
        wearBack.setVisible(false);
        wearBar = new GRect(WEAR_BAR_WIDTH, WEAR_BAR_HEIGHT);
        wearBar.setFilled(true);
        wearBar.setVisible(false);
        smoke = new GOval(SMOKE_SIZE, SMOKE_SIZE);
        smoke.setFilled(true);
        smoke.setFillColor(SMOKE);
        smoke.setColor(SMOKE);
        smoke.setVisible(false);
        rivalRing = new GOval(TrackDrawing.CAR_LENGTH * 1.5, TrackDrawing.CAR_LENGTH * 1.5);
        rivalRing.setColor(Palette.LINE_RIVAL);
        rivalRing.setVisible(false);
    }

    /**
     * Returns the indicator objects drawn on top of all cars (tire bar, lock-up smoke, rival ring).
     *
     * @return the objects
     */
    public List<GObject> getIndicators() {
        List<GObject> list = new ArrayList<GObject>();
        list.add(smoke);
        list.add(rivalRing);
        list.add(wearBack);
        list.add(wearBar);
        return list;
    }

    /**
     * Updates the indicators: compound color on the tires, a blinking red tire after a puncture,
     * the tire-condition bar (when details are shown for this car), lock-up smoke and the rival ring.
     *
     * @param px screen x of the car
     * @param py screen y of the car
     * @param zoom pixels per drawing unit
     * @param showDetail true to show the tire-condition bar
     * @param isRival true to ring the car as the player's rival
     */
    private void updateIndicators(double px, double py, double zoom, boolean showDetail, boolean isRival) {
        frames++;
        Compound compound = car.getTire().getCompound();
        if (compound != shownCompound) {
            shownCompound = compound;
            Color c = Palette.compoundColor(compound);
            for (GPolygon tire : tires) {
                tire.setColor(c);
            }
        }
        boolean blink = car.isPunctured() && (frames / PUNCTURE_BLINK_FRAMES) % 2 == 0;
        Color rearLeft = blink ? Palette.DANGER : TIRE;
        if (!tires[3].getFillColor().equals(rearLeft)) {
            tires[3].setFillColor(rearLeft);
        }
        double size = TrackDrawing.CAR_LENGTH * zoom * appliedEnlargement;
        if (showDetail) {
            double condition = Math.max(0, 1 - car.getTire().getWear());
            Color c = car.isPunctured() ? Palette.DANGER : car.getTire().getWear() < Tuning.WEAR_CLIFF * 0.75 ? Palette.GOOD
                    : car.getTire().getWear() < Tuning.WEAR_CLIFF ? Palette.WARNING : Palette.DANGER;
            double bx = px - WEAR_BAR_WIDTH / 2;
            double by = py + size * 0.6 + 4;
            wearBack.setBounds(bx - 1, by - 1, WEAR_BAR_WIDTH + 2, WEAR_BAR_HEIGHT + 2);
            wearBar.setLocation(bx, by);
            wearBar.setSize(Math.max(1, WEAR_BAR_WIDTH * condition), WEAR_BAR_HEIGHT);
            if (!wearBar.getFillColor().equals(c)) {
                wearBar.setFillColor(c);
                wearBar.setColor(c);
            }
        }
        if (wearBar.isVisible() != showDetail) {
            wearBar.setVisible(showDetail);
            wearBack.setVisible(showDetail);
        }
        LinePlan plan = car.getLinePlan();
        boolean locking = plan != null && plan.isLockedUp() && car.getDistance() < plan.getApexAt()
                && (frames / 3) % 2 == 0;
        if (smoke.isVisible() != locking) {
            smoke.setVisible(locking);
        }
        if (locking) {
            double heading = Math.toRadians(appliedDegrees);
            smoke.setBounds(px + Math.cos(heading) * size * 0.4 - SMOKE_SIZE / 2,
                    py - Math.sin(heading) * size * 0.4 - SMOKE_SIZE / 2, SMOKE_SIZE, SMOKE_SIZE);
        }
        if (rivalRing.isVisible() != isRival) {
            rivalRing.setVisible(isRival);
        }
        if (isRival) {
            double r = size * 0.9;
            rivalRing.setSize(2 * r, 2 * r);
            rivalRing.setLocation(px - r, py - r);
        }
    }

    /**
     * Returns the ERS exhaust flame, so the world view can add it under the car.
     *
     * @return the flame polygon
     */
    public GPolygon getGlow() {
        return glow;
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
     * @param showDetail true to show the tire-condition bar under the car
     * @param isRival true to ring the car as the player's rival in a fight
     */
    public void update(TrackDrawing drawing, double zoom, boolean showDetail, boolean isRival) {
        double d = car.getDistance();
        double lateral = car.getLateralOffset();
        double px = drawing.x(d, lateral) * zoom;
        double py = -drawing.y(d, lateral) * zoom;

        double enlargement = Math.max(1.0, MIN_SCREEN_LENGTH / (TrackDrawing.CAR_LENGTH * zoom));
        boolean rescale = Math.abs(enlargement / appliedEnlargement - 1) > 0.02;
        double probe = Tuning.HEADING_PROBE_METERS;
        double aheadLateral = lateral + car.getLateralRate() * probe;
        double heading = Math.atan2(drawing.y(d + probe, aheadLateral) - drawing.y(d, lateral),
                drawing.x(d + probe, aheadLateral) - drawing.x(d, lateral));
        double turn = Math.toDegrees(Track.angleDifference(heading, Math.toRadians(appliedDegrees)));
        turn = Math.max(-MAX_TURN_PER_FRAME, Math.min(MAX_TURN_PER_FRAME, turn));
        boolean rotate = Math.abs(turn) > 0.05;

        if (car.isDrsOpen() != wingOpen) {
            wingOpen = car.isDrsOpen();
            rearWing.setFillColor(wingOpen ? Palette.DRS : CARBON);
            rearWing.setColor(wingOpen ? Palette.DRS : CARBON);
        }
        boolean boosting = car.getErs().isDeploying();
        if (glow.isVisible() != boosting) {
            glow.setVisible(boosting);
        }
        glow.setLocation(px, py);
        if (rescale) {
            glow.scale(enlargement / appliedEnlargement);
        }
        if (rotate) {
            glow.rotate(turn);
        }
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
        updateIndicators(px, py, zoom, showDetail, isRival);
    }
}
