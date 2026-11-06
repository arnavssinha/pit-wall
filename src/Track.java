import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A closed racing circuit: its profile plus the centerline as evenly spaced samples.
 * Cars are located by their distance along the lap (from the start/finish line) and a
 * sideways offset from the centerline. This class turns that pair into x, y and heading.
 * Building a track does three things to the raw points: rescales them so the loop matches
 * the official lap length, resamples them every Tuning.SAMPLE_SPACING meters, and works out
 * the heading and smoothed curvature at each sample.
 */
public final class Track {

    /** Names, lap length and target lap time. */
    private final TrackProfile profile;

    /** The evenly spaced centerline samples, in driving order, starting at the start/finish line. */
    private final ArrayList<TrackSample> samples;

    /** Exact spacing between samples in meters (the lap length divided by the sample count). */
    private final double spacing;

    /** The racing line cars follow, drawn across the whole (stylized, very wide) track. */
    private final RacingLine racingLine;

    /**
     * The racing line on the real (unrounded) layout at real track width, used only for corner speeds.
     * The drawn track is rounded off and very wide, so its line nearly straightens a chicane; taking
     * corner speeds from the real shape keeps a chicane slow and a fast sweeper fast, as on the circuit.
     */
    private final RacingLine speedLine;

    /** Smallest x of the centerline in meters, used to frame the camera and minimap. */
    private final double minX;

    /** Largest x of the centerline in meters. */
    private final double maxX;

    /** Smallest y of the centerline in meters. */
    private final double minY;

    /** Largest y of the centerline in meters. */
    private final double maxY;

    /**
     * Builds a track from raw centerline points.
     *
     * @param profile the track's descriptive data
     * @param rawPoints centerline points as {x, y} in meters, in driving order, as a closed loop
     *                  whose first point is on the start/finish line (the last point must not repeat the first)
     * @throws IllegalArgumentException if fewer than 10 points are given
     */
    public Track(TrackProfile profile, List<double[]> rawPoints) {
        if (rawPoints.size() < 10) {
            throw new IllegalArgumentException("A track needs at least 10 centerline points");
        }
        this.profile = profile;
        int count = (int) Math.round(profile.getLapLengthMeters() / Tuning.SAMPLE_SPACING);
        this.spacing = profile.getLapLengthMeters() / count;
        List<double[]> real = resample(scaleToLength(rawPoints, profile.getLapLengthMeters()), count, spacing);
        List<double[]> even = new ArrayList<double[]>();
        for (double[] p : real) {
            even.add(new double[] {p[0], p[1]});
        }
        limitCurvature(even, 1.0 / Tuning.MIN_CORNER_RADIUS);
        limitCurvature(real, 1.0 / Tuning.MIN_SPEED_RADIUS);
        even = resample(scaleToLength(even, profile.getLapLengthMeters()), count, spacing);
        this.samples = buildSamples(even, spacing);
        int n = samples.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        double[] hs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = samples.get(i).getX();
            ys[i] = samples.get(i).getY();
            hs[i] = samples.get(i).getHeading();
        }
        this.racingLine = new RacingLine(xs, ys, hs, spacing, Tuning.TRACK_WIDTH / 2 - Tuning.RACING_LINE_EDGE_MARGIN);
        ArrayList<TrackSample> realSamples = buildSamples(real, spacing);
        double[] rx = new double[n];
        double[] ry = new double[n];
        double[] rh = new double[n];
        for (int i = 0; i < n; i++) {
            rx[i] = realSamples.get(i).getX();
            ry[i] = realSamples.get(i).getY();
            rh[i] = realSamples.get(i).getHeading();
        }
        this.speedLine = new RacingLine(rx, ry, rh, spacing, Tuning.SPEED_LINE_HALF_WIDTH);
        double lowX = Double.MAX_VALUE;
        double highX = -Double.MAX_VALUE;
        double lowY = Double.MAX_VALUE;
        double highY = -Double.MAX_VALUE;
        for (TrackSample s : samples) {
            lowX = Math.min(lowX, s.getX());
            highX = Math.max(highX, s.getX());
            lowY = Math.min(lowY, s.getY());
            highY = Math.max(highY, s.getY());
        }
        minX = lowX;
        maxX = highX;
        minY = lowY;
        maxY = highY;
    }

    /**
     * Returns the track's descriptive data.
     *
     * @return the profile
     */
    public TrackProfile getProfile() {
        return profile;
    }

    /**
     * Returns the lap length.
     *
     * @return lap length in meters
     */
    public double getLapLength() {
        return profile.getLapLengthMeters();
    }

    /**
     * Returns the number of centerline samples.
     *
     * @return the sample count
     */
    public int getSampleCount() {
        return samples.size();
    }

    /**
     * Returns one sample.
     *
     * @param index sample index, wrapped around the loop if out of range
     * @return the sample
     */
    public TrackSample getSample(int index) {
        return samples.get(Math.floorMod(index, samples.size()));
    }

    /**
     * Returns all samples, read-only.
     *
     * @return the samples in driving order
     */
    public List<TrackSample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    /**
     * Returns the distance between neighbouring samples.
     *
     * @return spacing in meters
     */
    public double getSpacing() {
        return spacing;
    }

    /**
     * Returns the smallest x of the centerline.
     *
     * @return minimum x in meters
     */
    public double getMinX() {
        return minX;
    }

    /**
     * Returns the largest x of the centerline.
     *
     * @return maximum x in meters
     */
    public double getMaxX() {
        return maxX;
    }

    /**
     * Returns the smallest y of the centerline.
     *
     * @return minimum y in meters
     */
    public double getMinY() {
        return minY;
    }

    /**
     * Returns the largest y of the centerline.
     *
     * @return maximum y in meters
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * Returns the racing line's sideways position at a point, blended between samples.
     *
     * @param distance distance along the lap in meters
     * @return offset from the centerline in meters, positive to the left
     */
    public double lineAt(double distance) {
        double d = wrap(distance);
        int i = indexAt(d);
        double t = (d - samples.get(i).getDistance()) / spacing;
        return racingLine.offsetAt(i) + (racingLine.offsetAt(i + 1) - racingLine.offsetAt(i)) * t;
    }

    /**
     * Returns the racing line's curvature at a sample, which sets the corner speed there.
     *
     * @param index sample index (wrapped)
     * @return curvature in 1 per meter
     */
    public double getLineCurvature(int index) {
        return speedLine.curvatureAt(index);
    }

    /**
     * Wraps any distance into the range from 0 up to the lap length.
     *
     * @param distance a distance in meters, possibly negative or beyond one lap
     * @return the same point on the lap, in meters from the start/finish line
     */
    public double wrap(double distance) {
        double lap = getLapLength();
        double d = distance % lap;
        return d < 0 ? d + lap : d;
    }

    /**
     * Returns the index of the sample at or just behind a lap distance.
     *
     * @param distance distance along the lap in meters (any value; it is wrapped)
     * @return the sample index
     */
    public int indexAt(double distance) {
        int i = (int) Math.floor(wrap(distance) / spacing);
        return Math.min(i, samples.size() - 1);
    }

    /**
     * Returns the x position of a point on the track.
     *
     * @param distance distance along the lap in meters
     * @param lateral sideways offset in meters, positive to the left of the direction of travel
     * @return x in meters
     */
    public double xAt(double distance, double lateral) {
        double[] p = interpolate(distance);
        return p[0] - lateral * Math.sin(p[2]);
    }

    /**
     * Returns the y position of a point on the track.
     *
     * @param distance distance along the lap in meters
     * @param lateral sideways offset in meters, positive to the left of the direction of travel
     * @return y in meters
     */
    public double yAt(double distance, double lateral) {
        double[] p = interpolate(distance);
        return p[1] + lateral * Math.cos(p[2]);
    }

    /**
     * Returns the direction of travel at a point on the track.
     *
     * @param distance distance along the lap in meters
     * @return heading in radians, counter-clockwise from east
     */
    public double headingAt(double distance) {
        return interpolate(distance)[2];
    }

    /**
     * Blends the two samples around a distance to get a smooth position and heading.
     *
     * @param distance distance along the lap in meters
     * @return {x, y, heading}
     */
    private double[] interpolate(double distance) {
        double d = wrap(distance);
        int i = indexAt(d);
        TrackSample a = samples.get(i);
        TrackSample b = getSample(i + 1);
        double t = (d - a.getDistance()) / spacing;
        double x = a.getX() + (b.getX() - a.getX()) * t;
        double y = a.getY() + (b.getY() - a.getY()) * t;
        double turn = angleDifference(b.getHeading(), a.getHeading());
        return new double[] {x, y, a.getHeading() + turn * t};
    }

    /**
     * Returns the smallest signed angle that turns angle "from" into angle "to".
     *
     * @param to the end angle in radians
     * @param from the start angle in radians
     * @return the difference, between minus pi and pi
     */
    public static double angleDifference(double to, double from) {
        double d = (to - from) % (2 * Math.PI);
        if (d > Math.PI) {
            d -= 2 * Math.PI;
        } else if (d < -Math.PI) {
            d += 2 * Math.PI;
        }
        return d;
    }

    /**
     * Rounds off any corner tighter than the minimum radius, in place. The drawn track is much wider
     * than a real one, so a real hairpin would fold the inside edge over itself; easing only the
     * too-tight points (and a few neighbours) turns it into a smooth, wider corner and leaves the
     * rest of the layout alone. Each pass moves a flagged point halfway toward the midpoint of its
     * neighbours, which is a small local blur.
     *
     * @param points evenly spaced closed loop, changed in place
     * @param maxCurvature largest allowed curvature in 1 per meter
     */
    private static void limitCurvature(List<double[]> points, double maxCurvature) {
        int n = points.size();
        boolean[] flagged = new boolean[n];
        for (int pass = 0; pass < Tuning.CORNER_EASING_MAX_PASSES; pass++) {
            boolean any = false;
            for (int i = 0; i < n; i++) {
                flagged[i] = false;
            }
            for (int i = 0; i < n; i++) {
                if (curvatureAt(points, i) > maxCurvature) {
                    any = true;
                    for (int j = -Tuning.CORNER_EASING_REACH; j <= Tuning.CORNER_EASING_REACH; j++) {
                        flagged[Math.floorMod(i + j, n)] = true;
                    }
                }
            }
            if (!any) {
                return;
            }
            for (int i = 0; i < n; i++) {
                if (flagged[i]) {
                    double[] p = points.get(i);
                    double[] a = points.get(Math.floorMod(i - 1, n));
                    double[] b = points.get((i + 1) % n);
                    p[0] += 0.5 * ((a[0] + b[0]) / 2 - p[0]);
                    p[1] += 0.5 * ((a[1] + b[1]) / 2 - p[1]);
                }
            }
        }
    }

    /**
     * Returns the size of the curvature through a point and its two neighbours (one over the radius
     * of the circle through all three).
     *
     * @param points the closed loop
     * @param i index of the middle point
     * @return curvature in 1 per meter, never negative
     */
    private static double curvatureAt(List<double[]> points, int i) {
        int n = points.size();
        double[] a = points.get(Math.floorMod(i - 1, n));
        double[] b = points.get(i);
        double[] c = points.get((i + 1) % n);
        double ab = Math.hypot(b[0] - a[0], b[1] - a[1]);
        double bc = Math.hypot(c[0] - b[0], c[1] - b[1]);
        double ca = Math.hypot(a[0] - c[0], a[1] - c[1]);
        double cross = Math.abs((b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]));
        double product = ab * bc * ca;
        return product == 0 ? 0 : 2 * cross / product;
    }

    /**
     * Scales the raw loop about the origin so its perimeter equals the official lap length.
     *
     * @param points the raw loop
     * @param lapLength target perimeter in meters
     * @return the scaled points
     */
    private static List<double[]> scaleToLength(List<double[]> points, double lapLength) {
        double factor = lapLength / perimeter(points);
        List<double[]> out = new ArrayList<double[]>();
        for (double[] p : points) {
            out.add(new double[] {p[0] * factor, p[1] * factor});
        }
        return out;
    }

    /**
     * Measures the length of a closed loop of points.
     *
     * @param points the loop
     * @return total length in meters, including the segment back to the first point
     */
    private static double perimeter(List<double[]> points) {
        double total = 0;
        for (int i = 0; i < points.size(); i++) {
            double[] a = points.get(i);
            double[] b = points.get((i + 1) % points.size());
            total += Math.hypot(b[0] - a[0], b[1] - a[1]);
        }
        return total;
    }

    /**
     * Places points exactly "spacing" meters apart along the loop by walking its segments.
     *
     * @param points the loop
     * @param count number of output points
     * @param spacing distance between output points
     * @return the evenly spaced points
     */
    private static List<double[]> resample(List<double[]> points, int count, double spacing) {
        List<double[]> out = new ArrayList<double[]>();
        int segment = 0;
        double segmentStart = 0;
        for (int k = 0; k < count; k++) {
            double target = k * spacing;
            double[] a = points.get(segment);
            double[] b = points.get((segment + 1) % points.size());
            double length = Math.hypot(b[0] - a[0], b[1] - a[1]);
            while (segmentStart + length < target && segment < points.size() - 1) {
                segmentStart += length;
                segment++;
                a = points.get(segment);
                b = points.get((segment + 1) % points.size());
                length = Math.hypot(b[0] - a[0], b[1] - a[1]);
            }
            double t = length == 0 ? 0 : (target - segmentStart) / length;
            out.add(new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
        }
        return out;
    }

    /**
     * Works out heading and smoothed curvature at each evenly spaced point.
     * Heading uses the neighbours on both sides. Curvature is the change in heading per meter,
     * then averaged over a few samples so small wobbles in the source data do not make fake corners.
     *
     * @param points evenly spaced points
     * @param spacing distance between them
     * @return the samples
     */
    private static ArrayList<TrackSample> buildSamples(List<double[]> points, double spacing) {
        int n = points.size();
        double[] heading = new double[n];
        for (int i = 0; i < n; i++) {
            double[] prev = points.get((i - 1 + n) % n);
            double[] next = points.get((i + 1) % n);
            heading[i] = Math.atan2(next[1] - prev[1], next[0] - prev[0]);
        }
        double[] rawCurvature = new double[n];
        for (int i = 0; i < n; i++) {
            double turn = angleDifference(heading[(i + 1) % n], heading[(i - 1 + n) % n]);
            rawCurvature[i] = turn / (2 * spacing);
        }
        int half = Tuning.CURVATURE_SMOOTHING_WINDOW / 2;
        ArrayList<TrackSample> out = new ArrayList<TrackSample>();
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = -half; j <= half; j++) {
                sum += rawCurvature[Math.floorMod(i + j, n)];
            }
            double curvature = sum / (2 * half + 1);
            double[] p = points.get(i);
            out.add(new TrackSample(i, i * spacing, p[0], p[1], heading[i], curvature));
        }
        return out;
    }
}
