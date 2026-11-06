/**
 * Decides which part of the world is on screen.
 * In follow mode the camera centers on one car at a close zoom. In overview mode it shows the
 * whole track. Switching between them eases smoothly: both the zoom and the point being looked at
 * slide toward their new values a little each frame.
 */
public final class Camera {

    /** Zoom used when following a car, in pixels per drawing unit. Drawing choice. */
    public static final double FOLLOW_ZOOM = 3.2;

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

    /** 0 for normal following, 1 for framing the player's fight; eased in and out. */
    private double fightBlend;

    /** Width of the view in pixels. */
    private final double viewWidth;

    /** Height of the view in pixels. */
    private final double viewHeight;

    /** Pixels at the bottom of the view covered by the battle panel while a fight is framed. */
    private double panelReserve;

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
        this.viewWidth = width;
        this.viewHeight = height;
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
     * Sets how much of the bottom of the view the battle panel covers, so a framed fight stays visible.
     *
     * @param pixels height of the panel area
     */
    public void setPanelReserve(double pixels) {
        panelReserve = pixels;
    }

    /**
     * Moves and zooms the view. Normally it follows a car (or shows the whole track). During the
     * player's fight it frames the car and the corner together, above the battle panel, so every
     * colored line in the preview can be seen; it eases in and out of this view.
     *
     * @param focus the car to follow
     * @param battle the player's open fight, or null
     */
    public void update(Car focus, Battle battle) {
        boolean fight = battle != null && !battle.isResolved() && !overview && focus.isPlayer();
        fightBlend += ((fight ? 1.0 : 0.0) - fightBlend) * EASE;
        double carX = drawing.x(focus.getDistance(), focus.getLateralOffset());
        double carY = drawing.y(focus.getDistance(), focus.getLateralOffset());
        double fightX = carX;
        double fightY = carY;
        double fightZoom = FOLLOW_ZOOM;
        if (battle != null && !battle.isResolved()) {
            Track track = drawing.getTrack();
            double exit = focus.getDistance() + track.wrap(battle.getCorner().getApex() - focus.getDistance())
                    + Tuning.CORNER_EXIT_METERS / 2;
            double exitX = drawing.x(exit, track.lineAt(exit));
            double exitY = drawing.y(exit, track.lineAt(exit));
            fightX = (carX + exitX) / 2;
            fightY = (carY + exitY) / 2;
            double spanX = Math.abs(exitX - carX) + TrackDrawing.TRACK_WIDTH * 2;
            double spanY = Math.abs(exitY - carY) + TrackDrawing.TRACK_WIDTH * 2;
            double fit = Math.min(viewWidth / spanX, (viewHeight - panelReserve) / spanY);
            fightZoom = Math.max(FOLLOW_ZOOM * Tuning.FIGHT_MIN_ZOOM, Math.min(FOLLOW_ZOOM * Tuning.BATTLE_ZOOM, fit));
        }
        double normalZoom = overview ? overviewZoom : FOLLOW_ZOOM;
        double targetZoom = normalZoom + (fightZoom - normalZoom) * fightBlend;
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
        double followX = carX + (fightX - carX) * fightBlend;
        double followY = carY + (fightY - carY) * fightBlend;
        double lookX = trackX + (followX - trackX) * followBlend;
        double lookY = trackY + (followY - trackY) * followBlend;
        double screenY = centerY - panelReserve / 2 * fightBlend;
        world.setLocation(centerX - world.toPixelX(lookX), screenY - world.toPixelY(lookY));
    }
}
