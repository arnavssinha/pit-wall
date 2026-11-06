/**
 * The racing line: where on the track's width a car drives at each point of the lap.
 * It is found by repeated smoothing, like pulling a stiff rubber band through the track: each point
 * slides sideways to where it makes the bend gentlest (a weighted average of its neighbours that
 * minimizes the change in direction), but never closer to the edge than a small margin. After
 * enough passes the line swings to the outside before a corner, clips the inside at the apex and
 * runs out to the outside on exit, the classic racing line. Gentler bends mean higher corner speeds,
 * so the speed profile uses this line's curvature.
 */
public final class RacingLine {

    /** Sideways offset of the line at each sample, in meters (positive is left of the centerline). */
    private final double[] offset;

    /** Signed curvature of the line at each sample, in 1 per meter. */
    private final double[] curvature;

    /**
     * Computes the racing line for a centerline.
     *
     * @param x sample x positions in meters
     * @param y sample y positions in meters
     * @param heading sample headings in radians
     * @param spacing distance between samples in meters
     * @param halfWidth how far from the centerline the line may go, in meters
     */
    public RacingLine(double[] x, double[] y, double[] heading, double spacing, double halfWidth) {
        int n = x.length;
        offset = new double[n];
        double limit = halfWidth;
        double[] nx = new double[n];
        double[] ny = new double[n];
        double[] px = new double[n];
        double[] py = new double[n];
        for (int i = 0; i < n; i++) {
            nx[i] = -Math.sin(heading[i]);
            ny[i] = Math.cos(heading[i]);
            px[i] = x[i];
            py[i] = y[i];
        }
        for (int stride = Tuning.RACING_LINE_COARSEST_STRIDE; stride >= 1; stride /= 2) {
            for (int pass = 0; pass < Tuning.RACING_LINE_PASSES; pass++) {
                for (int i = 0; i < n; i++) {
                    int a = Math.floorMod(i - 2 * stride, n);
                    int b = Math.floorMod(i - stride, n);
                    int c = (i + stride) % n;
                    int d = (i + 2 * stride) % n;
                    double tx = (-px[a] + 4 * px[b] + 4 * px[c] - px[d]) / 6.0;
                    double ty = (-py[a] + 4 * py[b] + 4 * py[c] - py[d]) / 6.0;
                    double o = (tx - x[i]) * nx[i] + (ty - y[i]) * ny[i];
                    o = Math.max(-limit, Math.min(limit, o));
                    offset[i] = o;
                    px[i] = x[i] + nx[i] * o;
                    py[i] = y[i] + ny[i] * o;
                }
            }
        }
        curvature = lineCurvature(px, py, spacing);
    }

    /**
     * Works out the line's curvature from its points: the change of direction per meter, averaged
     * over a few samples.
     *
     * @param px line x positions
     * @param py line y positions
     * @param spacing nominal distance between samples in meters
     * @return signed curvature per sample
     */
    private static double[] lineCurvature(double[] px, double[] py, double spacing) {
        int n = px.length;
        double[] dir = new double[n];
        for (int i = 0; i < n; i++) {
            int prev = Math.floorMod(i - 1, n);
            int next = (i + 1) % n;
            dir[i] = Math.atan2(py[next] - py[prev], px[next] - px[prev]);
        }
        double[] raw = new double[n];
        for (int i = 0; i < n; i++) {
            raw[i] = Track.angleDifference(dir[(i + 1) % n], dir[Math.floorMod(i - 1, n)]) / (2 * spacing);
        }
        int half = Tuning.CURVATURE_SMOOTHING_WINDOW / 2;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = -half; j <= half; j++) {
                sum += raw[Math.floorMod(i + j, n)];
            }
            out[i] = sum / (2 * half + 1);
        }
        return out;
    }

    /**
     * Returns the line's sideways offset at a sample.
     *
     * @param index sample index (wrapped)
     * @return offset in meters, positive to the left
     */
    public double offsetAt(int index) {
        return offset[Math.floorMod(index, offset.length)];
    }

    /**
     * Returns the line's curvature at a sample.
     *
     * @param index sample index (wrapped)
     * @return curvature in 1 per meter
     */
    public double curvatureAt(int index) {
        return curvature[Math.floorMod(index, curvature.length)];
    }
}
