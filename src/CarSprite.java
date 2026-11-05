import acm.graphics.GPolygon;

/**
 * The drawing of one car: a small arrow-shaped polygon in team colors, pointing along the track.
 * The polygon is built once and only moved and turned each frame, so no new objects are created
 * while racing. Its size is in meters; the world view's zoom scales it to pixels.
 */
public final class CarSprite {

    /** The car this sprite draws. */
    private final Car car;

    /** The polygon on screen, nose pointing along +x before rotation. */
    private final GPolygon body;

    /**
     * Smallest length a car is drawn on screen, in pixels. When zoomed far out the polygon is
     * enlarged to this size so cars stay visible in the whole-track view. Drawing choice.
     */
    private static final double MIN_SCREEN_LENGTH = 11;

    /** Rotation currently applied to the polygon, in degrees. */
    private double appliedDegrees;

    /** Extra enlargement currently applied on top of the world zoom (1 means true size). */
    private double appliedEnlargement;

    /**
     * Builds the sprite.
     *
     * @param car the car to draw
     * @param isPlayer true to draw the player's car with a bright outline
     */
    public CarSprite(Car car, boolean isPlayer) {
        this.car = car;
        double half = Tuning.CAR_LENGTH / 2;
        double w = Tuning.CAR_WIDTH / 2;
        body = new GPolygon();
        body.addVertex(half, 0);
        body.addVertex(half * 0.35, -w * 0.55);
        body.addVertex(-half * 0.55, -w);
        body.addVertex(-half, -w);
        body.addVertex(-half, w);
        body.addVertex(-half * 0.55, w);
        body.addVertex(half * 0.35, w * 0.55);
        body.setFilled(true);
        body.setFillColor(Palette.teamColor(car.getDriver().getTeam()));
        body.setColor(isPlayer ? Palette.PLAYER : Palette.CAR_OUTLINE);
        appliedDegrees = 0;
        appliedEnlargement = 1;
    }

    /**
     * Returns the polygon so it can be added to the world view.
     *
     * @return the polygon
     */
    public GPolygon getBody() {
        return body;
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
     * Moves and turns the polygon to match the car.
     * Screen y points down while track y points north, so y is flipped. ACM rotation is
     * counter-clockwise on screen, which matches the track heading after that flip.
     *
     * @param track the track, used to turn distance into position
     * @param zoom pixels per meter
     */
    public void update(Track track, double zoom) {
        double d = car.getDistance();
        double lateral = car.getLateralOffset();
        body.setLocation(track.xAt(d, lateral) * zoom, -track.yAt(d, lateral) * zoom);
        double enlargement = Math.max(1.0, MIN_SCREEN_LENGTH / (Tuning.CAR_LENGTH * zoom));
        if (Math.abs(enlargement / appliedEnlargement - 1) > 0.02) {
            body.scale(enlargement / appliedEnlargement);
            appliedEnlargement = enlargement;
        }
        double heading = track.headingAt(d);
        double turn = Math.toDegrees(Track.angleDifference(heading, Math.toRadians(appliedDegrees)));
        if (Math.abs(turn) > 0.05) {
            body.rotate(turn);
            appliedDegrees += turn;
        }
    }
}
