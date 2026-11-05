/**
 * Decides which part of the world is on screen.
 * In follow mode the camera centers on one car at a close zoom. In overview mode it shows the
 * whole track. Switching between them eases smoothly: both the zoom and the point being looked at
 * slide toward their new values a little each frame.
 */
public final class Camera {

    /** Zoom used when following a car, in pixels per drawing unit. Drawing choice. */
    public static final double FOLLOW_ZOOM = 4.0;

    /** Fraction of the remaining distance covered each frame when easing (larger is snappier). */
    private static final double EASE = 0.12;

    /** The world being moved and zoomed. */
    private final WorldView world;

    /** Converts track positions to drawing units, for bounds and car positions. */
    private final TrackDrawing drawing;

    /** Center of the screen area the world is shown in, x in pixels. */
    private final double centerX;

    /** Center of the screen area the world is shown in, y in pixels. */
    private final double centerY;

    /** Zoom that fits the whole track in the view area, in pixels per meter. */
    private final double overviewZoom;

    /** True for the whole-track view, false to follow a car. */
    private boolean overview;

    /** How far the view has moved from the track center (0) to the followed car (1). */
    private double followBlend;

    /**
     * Creates a camera that starts in follow mode.
     *
     * @param world the world view
     * @param drawing converts track positions to drawing units
     * @param left left edge of the screen area for the world, in pixels
     * @param top top edge of that area
     * @param width width of that area
     * @param height height of that area
     */
    public Camera(WorldView world, TrackDrawing drawing, double left, double top, double width, double height) {
        this.world = world;
        this.drawing = drawing;
        this.centerX = left + width / 2;
        this.centerY = top + height / 2;
        double margin = 40 + TrackDrawing.TRACK_WIDTH;
        double zx = (width - margin) / (drawing.maxX() - drawing.minX());
        double zy = (height - margin) / (drawing.maxY() - drawing.minY());
        this.overviewZoom = Math.min(zx, zy);
        this.overview = false;
        this.followBlend = 1.0;
        world.setZoom(FOLLOW_ZOOM);
    }

    /**
     * Switches between follow mode and the whole-track view.
     */
    public void toggleOverview() {
        overview = !overview;
    }

    /**
     * Returns whether the whole-track view is active.
     *
     * @return true in overview mode
     */
    public boolean isOverview() {
        return overview;
    }

    /**
     * Eases zoom and position one frame toward the current mode's target and moves the world.
     *
     * @param focus the car to follow in follow mode
     */
    public void update(Car focus) {
        double targetZoom = overview ? overviewZoom : FOLLOW_ZOOM;
        double zoom = world.getZoom();
        if (Math.abs(targetZoom - zoom) < targetZoom * 0.002) {
            zoom = targetZoom;
        } else {
            zoom += (targetZoom - zoom) * EASE;
        }
        world.setZoom(zoom);

        double targetBlend = overview ? 0.0 : 1.0;
        followBlend += (targetBlend - followBlend) * EASE;
        if (Math.abs(targetBlend - followBlend) < 0.001) {
            followBlend = targetBlend;
        }
        double trackX = (drawing.minX() + drawing.maxX()) / 2;
        double trackY = (drawing.minY() + drawing.maxY()) / 2;
        double carX = drawing.x(focus.getDistance(), focus.getLateralOffset());
        double carY = drawing.y(focus.getDistance(), focus.getLateralOffset());
        double lookX = trackX + (carX - trackX) * followBlend;
        double lookY = trackY + (carY - trackY) * followBlend;
        world.setLocation(centerX - world.toPixelX(lookX), centerY - world.toPixelY(lookY));
    }
}
