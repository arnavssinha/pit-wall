import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

/**
 * The circuit: centerline points, a racing line and the fastest safe speed at every point. Positions on track
 * are a distance along the lap plus a sideways offset (positive = left).
 */
public final class Track {

    /** Track width in meters (the game world is drawn about four times wider than a real track). */
    public static final double WIDTH = 62;

    /** Top speed in m/s (about 330 km/h). */
    public static final double TOP_SPEED = 92;

    /** Sideways grip: the most a car can accelerate into a corner, in m/s squared. */
    private static final double CORNER_GRIP = 26;

    /** Braking power in m/s squared. */
    public static final double BRAKING = 32;

    /** The racing line stays this far inside the track edges, in meters. */
    private static final double EDGE_MARGIN = 5;

    /** Centerline x of each point, in meters. */
    private final double[] x;

    /** Centerline y of each point, in meters. */
    private final double[] y;

    /** Sideways offset of the racing line at each point, in meters. */
    private final double[] line;

    /** Fastest safe speed at each point, in m/s. */
    private final double[] speed;

    /** Distance between neighbouring points, in meters. */
    private final double spacing;

    /** Distances (along the lap) where cars start braking for a corner. */
    private final ArrayList<Double> brakePoints = new ArrayList<Double>();

    /** Builds a track from evenly spaced centerline points. */
    public Track(ArrayList<double[]> points) {
        int n = points.size();
        x = new double[n];
        y = new double[n];
        double length = 0;
        for (int i = 0; i < n; i++) {
            x[i] = points.get(i)[0];
            y[i] = points.get(i)[1];
            double[] next = points.get((i + 1) % n);
            length += Math.hypot(next[0] - x[i], next[1] - y[i]);
        }
        spacing = length / n;
        line = findRacingLine();
        speed = findSpeeds();
        for (int i = 0; i < n; i++) {
            boolean startsDropping = speed[i] > speed[(i + 1) % n] && speed[i] >= speed[(i - 1 + n) % n];
            if (startsDropping && speed[i] > TOP_SPEED * 0.9) {
                brakePoints.add(i * spacing);
            }
        }
    }

    /** Loads a track from a file of "x,y" lines (from inside the jar, or from the project folder). */
    public static Track load(String path) throws IOException {
        InputStream in = Track.class.getResourceAsStream("/" + path);
        Reader reader = in != null ? new InputStreamReader(in, "UTF-8") : new FileReader(path);
        ArrayList<double[]> points = new ArrayList<double[]>();
        try (BufferedReader lines = new BufferedReader(reader)) {
            String text;
            while ((text = lines.readLine()) != null) {
                if (!text.startsWith("#") && !text.trim().isEmpty()) {
                    String[] parts = text.split(",");
                    points.add(new double[] {Double.parseDouble(parts[0]), Double.parseDouble(parts[1])});
                }
            }
        }
        return new Track(points);
    }

    /**
     * Finds the racing line by smoothing the path inside the track edges: each point moves toward the middle
     * of its neighbours, first with far-apart neighbours (long sweeps) and then nearer ones.
     */
    private double[] findRacingLine() {
        int n = x.length;
        double[] offset = new double[n];
        double limit = WIDTH / 2 - EDGE_MARGIN;
        for (int step = 32; step >= 1; step /= 2) {
            for (int pass = 0; pass < 150; pass++) {
                for (int i = 0; i < n; i++) {
                    int a = Math.floorMod(i - step, n);
                    int b = (i + step) % n;
                    double midX = (pointX(a, offset[a]) + pointX(b, offset[b])) / 2;
                    double midY = (pointY(a, offset[a]) + pointY(b, offset[b])) / 2;
                    double h = headingAt(i);
                    double toward = (midX - x[i]) * -Math.sin(h) + (midY - y[i]) * Math.cos(h);
                    offset[i] = Math.max(-limit, Math.min(limit, toward));
                }
            }
        }
        return offset;
    }

    /**
     * Works out the fastest safe speed at each point: limited by grip in corners, then lowered before each
     * corner so a car can brake in time.
     */
    private double[] findSpeeds() {
        int n = x.length;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            double curve = Math.abs(curvature(i));
            v[i] = curve < 1e-6 ? TOP_SPEED : Math.min(TOP_SPEED, Math.sqrt(CORNER_GRIP / curve));
        }
        for (int lap = 0; lap < 2; lap++) {
            for (int i = n - 1; i >= 0; i--) {
                double next = v[(i + 1) % n];
                v[i] = Math.min(v[i], Math.sqrt(next * next + 2 * BRAKING * spacing));
            }
        }
        return v;
    }

    /** Returns how sharply the racing line turns at a point (1 / radius), from three neighbouring points. */
    private double curvature(int i) {
        int n = x.length;
        int a = Math.floorMod(i - 3, n);
        int c = (i + 3) % n;
        double h1 = Math.atan2(pointY(i, line[i]) - pointY(a, line[a]), pointX(i, line[i]) - pointX(a, line[a]));
        double h2 = Math.atan2(pointY(c, line[c]) - pointY(i, line[i]), pointX(c, line[c]) - pointX(i, line[i]));
        return angle(h2 - h1) / (3 * spacing);
    }

    /** Returns the x of a point moved sideways from the centerline. */
    private double pointX(int i, double lateral) {
        return x[i] - lateral * Math.sin(headingAt(i));
    }

    /** Returns the y of a point moved sideways from the centerline. */
    private double pointY(int i, double lateral) {
        return y[i] + lateral * Math.cos(headingAt(i));
    }

    /** Returns the direction of the centerline at a point. */
    private double headingAt(int i) {
        int n = x.length;
        int a = Math.floorMod(i - 1, n);
        int b = (i + 1) % n;
        return Math.atan2(y[b] - y[a], x[b] - x[a]);
    }

    /** Wraps an angle into -pi to pi. */
    public static double angle(double a) {
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    /** Wraps a distance into one lap. */
    public double wrap(double d) {
        double lap = length();
        return ((d % lap) + lap) % lap;
    }

    /** Returns the point at or just before a distance. */
    private int index(double d) {
        return Math.min(x.length - 1, (int) (wrap(d) / spacing));
    }

    /** Returns x at a distance and sideways offset. */
    public double x(double d, double lateral) {
        return pointX(index(d), lateral);
    }

    /** Returns y at a distance and sideways offset. */
    public double y(double d, double lateral) {
        return pointY(index(d), lateral);
    }

    /** Returns the direction of travel at a distance. */
    public double heading(double d) {
        return headingAt(index(d));
    }

    /** Returns the racing line's sideways offset at a distance. */
    public double lineAt(double d) {
        return line[index(d)];
    }

    /** Returns the fastest safe speed at a distance. */
    public double speedAt(double d) {
        return speed[index(d)];
    }

    /** Returns whether a distance is on a flat-out straight (where DRS and boost work). */
    public boolean isStraight(double d) {
        return speed[index(d)] >= TOP_SPEED * 0.97;
    }

    /** Returns how far ahead the next braking point is. */
    public double toNextBrakePoint(double d) {
        double best = length();
        for (double p : brakePoints) {
            best = Math.min(best, wrap(p - d));
        }
        return best;
    }

    /** Returns the lap length. */
    public double length() {
        return spacing * x.length;
    }
}
