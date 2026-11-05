/**
 * One evenly spaced point on the track centerline.
 * The track is a closed loop of these samples, about 5 m apart. Cars find their
 * position, direction and the sharpness of the bend from the samples around them.
 */
public final class TrackSample {

    /** Position of this sample in the loop, starting at 0 on the start/finish line. */
    private final int index;

    /** Distance along the lap from the start/finish line, in meters. */
    private final double distance;

    /** East-west position in meters. */
    private final double x;

    /** North-south position in meters (north is positive). */
    private final double y;

    /** Direction of travel in radians, counter-clockwise from east. */
    private final double heading;

    /**
     * Signed curvature in 1 per meter: positive bends left, negative bends right.
     * Curvature is 1 divided by the corner radius, so a tight hairpin has a large value.
     */
    private final double curvature;

    /**
     * Creates a sample.
     *
     * @param index position in the loop
     * @param distance distance along the lap in meters
     * @param x east-west position in meters
     * @param y north-south position in meters
     * @param heading direction of travel in radians
     * @param curvature signed curvature in 1 per meter
     */
    public TrackSample(int index, double distance, double x, double y, double heading, double curvature) {
        this.index = index;
        this.distance = distance;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.curvature = curvature;
    }

    /**
     * Returns the position in the loop.
     *
     * @return the sample index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the distance along the lap.
     *
     * @return distance in meters from the start/finish line
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Returns the east-west position.
     *
     * @return x in meters
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the north-south position.
     *
     * @return y in meters
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the direction of travel.
     *
     * @return heading in radians, counter-clockwise from east
     */
    public double getHeading() {
        return heading;
    }

    /**
     * Returns the signed curvature.
     *
     * @return curvature in 1 per meter, positive for left bends
     */
    public double getCurvature() {
        return curvature;
    }
}
