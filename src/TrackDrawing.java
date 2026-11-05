/**
 * Converts track positions (meters) into drawing units for the screen.
 * The same scale is used along and across the track, so a car turning or moving sideways is drawn
 * at its true angle. (The game world itself is stylized, with a wide track and large cars; see
 * Tuning.TRACK_WIDTH.)
 * Drawing units are later turned into pixels by the camera zoom, with y flipped so north is up.
 */
public final class TrackDrawing {

    /** Layout shrink factor: one meter along the track becomes this many drawing units. Style choice. */
    public static final double LAYOUT_SCALE = 0.5;

    /** Drawn track width in drawing units. */
    public static final double TRACK_WIDTH = Tuning.TRACK_WIDTH * LAYOUT_SCALE;

    /** Drawn car length in drawing units. Tuning.MIN_SEPARATION keeps cars from overlapping. */
    public static final double CAR_LENGTH = Tuning.CAR_LENGTH * LAYOUT_SCALE;

    /** Drawn car width in drawing units. */
    public static final double CAR_WIDTH = Tuning.CAR_WIDTH * LAYOUT_SCALE;

    /** Sideways scale: the same as along the track, so nothing is stretched. */
    private static final double LATERAL_SCALE = LAYOUT_SCALE;

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
     * Converts a sideways offset in meters to drawing units.
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
