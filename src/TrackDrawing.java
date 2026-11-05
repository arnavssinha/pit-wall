/**
 * Converts track positions (meters) into drawing units for the screen.
 * The drawing is deliberately not to scale: the layout is shrunk while the track width and the
 * cars are drawn much larger, so the screen shows more track and bigger cars and less empty grass.
 * The simulation still works in real meters; only the picture is stylized.
 * Drawing units are later turned into pixels by the camera zoom, with y flipped so north is up.
 */
public final class TrackDrawing {

    /** Layout shrink factor: one meter along the track becomes this many drawing units. Style choice. */
    public static final double LAYOUT_SCALE = 0.5;

    /** Drawn track width in drawing units (the real width is Tuning.TRACK_WIDTH meters). Style choice. */
    public static final double TRACK_WIDTH = 32.0;

    /** Drawn car length in drawing units. Style choice; Tuning.MIN_SEPARATION keeps cars from overlapping. */
    public static final double CAR_LENGTH = 10.0;

    /** Drawn car width in drawing units. Style choice. */
    public static final double CAR_WIDTH = 3.6;

    /** Sideways stretch so a lateral offset in meters lands in the right place on the wider drawn track. */
    private static final double LATERAL_SCALE = TRACK_WIDTH / Tuning.TRACK_WIDTH;

    /** The track being drawn. */
    private final Track track;

    /**
     * Creates the converter for one track.
     *
     * @param track the track
     */
    public TrackDrawing(Track track) {
        this.track = track;
    }

    /**
     * Returns the track being drawn.
     *
     * @return the track
     */
    public Track getTrack() {
        return track;
    }

    /**
     * Converts a sideways offset in meters to drawing units, on the widened drawn track.
     *
     * @param meters sideways offset in meters
     * @return the same offset in drawing units
     */
    public static double lateralToDrawing(double meters) {
        return meters * LATERAL_SCALE;
    }

    /**
     * Drawing x of a point on the track.
     *
     * @param distance distance along the lap in meters
     * @param lateral offset in meters (positive left); use offsetX for offsets already in drawing units
     * @return x in drawing units
     */
    public double x(double distance, double lateral) {
        return offsetX(distance, lateral * LATERAL_SCALE);
    }

    /**
     * Drawing y of a point on the track (north positive, not yet flipped for the screen).
     *
     * @param distance distance along the lap in meters
     * @param lateral offset in meters (positive left)
     * @return y in drawing units
     */
    public double y(double distance, double lateral) {
        return offsetY(distance, lateral * LATERAL_SCALE);
    }

    /**
     * Drawing x of a point offset sideways by a distance already in drawing units.
     *
     * @param distance distance along the lap in meters
     * @param drawOffset sideways offset in drawing units (positive left)
     * @return x in drawing units
     */
    public double offsetX(double distance, double drawOffset) {
        return track.xAt(distance, 0) * LAYOUT_SCALE - drawOffset * Math.sin(track.headingAt(distance));
    }

    /**
     * Drawing y of a point offset sideways by a distance already in drawing units.
     *
     * @param distance distance along the lap in meters
     * @param drawOffset sideways offset in drawing units (positive left)
     * @return y in drawing units
     */
    public double offsetY(double distance, double drawOffset) {
        return track.yAt(distance, 0) * LAYOUT_SCALE + drawOffset * Math.cos(track.headingAt(distance));
    }

    /**
     * Smallest drawing x of the centerline.
     *
     * @return x in drawing units
     */
    public double minX() {
        return track.getMinX() * LAYOUT_SCALE;
    }

    /**
     * Largest drawing x of the centerline.
     *
     * @return x in drawing units
     */
    public double maxX() {
        return track.getMaxX() * LAYOUT_SCALE;
    }

    /**
     * Smallest drawing y of the centerline.
     *
     * @return y in drawing units
     */
    public double minY() {
        return track.getMinY() * LAYOUT_SCALE;
    }

    /**
     * Largest drawing y of the centerline.
     *
     * @return y in drawing units
     */
    public double maxY() {
        return track.getMaxY() * LAYOUT_SCALE;
    }
}
