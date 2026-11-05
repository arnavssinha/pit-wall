import acm.graphics.GImage;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Paints the static track scenery into one picture, pixel by pixel, instead of out of polygons.
 * Every pixel finds the nearest point of the centerline and its sideways distance from it, and that
 * distance decides the color: asphalt, white edge line, kerb (red and white stripes in bends), paved
 * run-off, gravel on the outside of bends, the pit lane and garages, or transparent (the striped
 * grass drawn behind shows through). Because the colors depend only on distance, the track keeps a
 * clean shape at any width, even in tight corners where polygon strips would fold over themselves.
 * Each band edge is smoothed by blending two samples half a pixel apart. The pixels are computed once
 * per track and kept in a HashMap; ACM's GImage turns the pixel array (rows of ARGB colors) into a picture.
 */
public final class TrackRaster {

    /** Pixels per drawing unit in the picture. */
    private static final double PIXELS_PER_UNIT = 2.0;

    /** Picture margin around the centerline bounds, in drawing units (enough for gravel and pits). */
    private static final double MARGIN = 70.0;

    /** Bucket size for finding the nearest centerline point quickly, in drawing units. */
    private static final double BUCKET = 30.0;

    /** Kerb width in drawing units. */
    private static final double KERB = 3.0;

    /** Paved run-off width beyond the kerb, in drawing units. */
    private static final double RUNOFF = 10.0;

    /** Gravel trap width on the outside of bends, in drawing units. */
    private static final double GRAVEL = 22.0;

    /** Width of the white edge line, in drawing units. */
    private static final double EDGE_LINE = 0.7;

    /** Samples either side of a bend that still get kerbs, so kerbs run a little past the corner. */
    private static final int KERB_SPREAD = 2;

    /** Pit lane width in drawing units. */
    private static final double PIT_WIDTH = 11.0;

    /** Length of the pit entry and exit ramps, in meters along the lap. */
    private static final double PIT_RAMP = 100.0;


    /** Garage length along the lane, in meters. */
    private static final double GARAGE_LENGTH = 16.0;

    /** Garage depth away from the lane, in drawing units. */
    private static final double GARAGE_DEPTH = 8.0;

    /** Pictures already painted, by track id. */
    private static final HashMap<String, int[][]> CACHE = new HashMap<String, int[][]>();

    /** The drawing converter. */
    private final TrackDrawing drawing;

    /** Team colors, one garage each. */
    private final List<Color> garageColors;

    /** Centerline sample positions in drawing units. */
    private final double[] cx;

    /** Centerline sample y positions in drawing units. */
    private final double[] cy;

    /** Sideways (left) direction at each sample, x part. */
    private final double[] nx;

    /** Sideways (left) direction at each sample, y part. */
    private final double[] ny;

    /** Whether each sample gets kerbs. */
    private final boolean[] kerb;

    /** Side of the inside of the bend at each sample: +1 left, -1 right, 0 straight. */
    private final int[] inside;

    /** Left edge of the picture in drawing units. */
    private final double left;

    /** Top edge of the picture in drawing units (y points north). */
    private final double top;

    /** Picture width in pixels. */
    private final int width;

    /** Picture height in pixels. */
    private final int height;

    /** Number of buckets across. */
    private final int bucketsX;

    /** Number of buckets down. */
    private final int bucketsY;

    /** Sample indices in each bucket. */
    private final List<List<Integer>> buckets = new ArrayList<List<Integer>>();

    /**
     * Prepares to paint a track.
     *
     * @param drawing the drawing converter
     * @param teams the teams, for the garage colors
     */
    private TrackRaster(TrackDrawing drawing, List<Team> teams) {
        this.drawing = drawing;
        this.garageColors = new ArrayList<Color>();
        for (Team t : teams) {
            garageColors.add(Palette.teamColor(t));
        }
        Track track = drawing.getTrack();
        int n = track.getSampleCount();
        cx = new double[n];
        cy = new double[n];
        nx = new double[n];
        ny = new double[n];
        kerb = new boolean[n];
        inside = new int[n];
        for (int i = 0; i < n; i++) {
            TrackSample s = track.getSample(i);
            cx[i] = drawing.x(s.getDistance(), 0);
            cy[i] = drawing.y(s.getDistance(), 0);
            nx[i] = -Math.sin(s.getHeading());
            ny[i] = Math.cos(s.getHeading());
            double k = s.getCurvature();
            inside[i] = Math.abs(k) >= Tuning.CORNER_CURVATURE ? (k > 0 ? 1 : -1) : 0;
        }
        for (int i = 0; i < n; i++) {
            for (int j = -KERB_SPREAD; j <= KERB_SPREAD; j++) {
                kerb[i] |= inside[Math.floorMod(i + j, n)] != 0;
            }
        }
        left = drawing.minX() - MARGIN;
        top = drawing.maxY() + MARGIN;
        width = (int) Math.ceil((drawing.maxX() + MARGIN - left) * PIXELS_PER_UNIT);
        height = (int) Math.ceil((top - (drawing.minY() - MARGIN)) * PIXELS_PER_UNIT);
        bucketsX = (int) Math.ceil(width / PIXELS_PER_UNIT / BUCKET) + 1;
        bucketsY = (int) Math.ceil(height / PIXELS_PER_UNIT / BUCKET) + 1;
        for (int b = 0; b < bucketsX * bucketsY; b++) {
            buckets.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < n; i++) {
            int bx = (int) ((cx[i] - left) / BUCKET);
            int by = (int) ((top - cy[i]) / BUCKET);
            buckets.get(by * bucketsX + bx).add(i);
        }
    }

    /**
     * Returns the track picture, ready to add to the world at zoom 1 (one drawing unit per pixel).
     *
     * @param drawing the drawing converter
     * @param teams the teams, for the garage colors
     * @return the picture, positioned and sized in drawing units
     */
    public static GImage build(TrackDrawing drawing, List<Team> teams) {
        TrackRaster raster = new TrackRaster(drawing, teams);
        String id = drawing.getTrack().getProfile().getId() + ":" + teams.size();
        int[][] pixels = CACHE.get(id);
        if (pixels == null) {
            pixels = raster.paint();
            CACHE.put(id, pixels);
        }
        GImage image = new GImage(pixels);
        image.setBounds(raster.left, -raster.top, raster.width / PIXELS_PER_UNIT, raster.height / PIXELS_PER_UNIT);
        return image;
    }

    /**
     * Paints every pixel.
     *
     * @return rows of ARGB colors
     */
    private int[][] paint() {
        int[][] pixels = new int[height][width];
        double half = 0.5 / PIXELS_PER_UNIT;
        for (int r = 0; r < height; r++) {
            double y = top - (r + 0.5) / PIXELS_PER_UNIT;
            for (int c = 0; c < width; c++) {
                double x = left + (c + 0.5) / PIXELS_PER_UNIT;
                int nearest = nearestSample(x, y);
                if (nearest < 0) {
                    continue;
                }
                double lateral = (x - cx[nearest]) * nx[nearest] + (y - cy[nearest]) * ny[nearest];
                int a = colorAt(nearest, lateral - half * Math.signum(lateral));
                int b = colorAt(nearest, lateral + half * Math.signum(lateral));
                pixels[r][c] = a == b ? a : mix(a, b);
            }
        }
        return pixels;
    }

    /**
     * Finds the centerline sample nearest a point, among the buckets around it.
     *
     * @param x point x in drawing units
     * @param y point y in drawing units
     * @return the sample index, or -1 if nothing is near enough to matter
     */
    private int nearestSample(double x, double y) {
        int bx = (int) ((x - left) / BUCKET);
        int by = (int) ((top - y) / BUCKET);
        int best = -1;
        double bestD2 = Double.MAX_VALUE;
        for (int j = by - 2; j <= by + 2; j++) {
            for (int i = bx - 2; i <= bx + 2; i++) {
                if (i < 0 || j < 0 || i >= bucketsX || j >= bucketsY) {
                    continue;
                }
                for (int s : buckets.get(j * bucketsX + i)) {
                    double dx = x - cx[s];
                    double dy = y - cy[s];
                    double d2 = dx * dx + dy * dy;
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        best = s;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Decides the color at a sideways distance from a centerline sample.
     *
     * @param sample the nearest sample
     * @param lateral signed sideways distance in drawing units (positive left)
     * @return an ARGB color, 0 for transparent
     */
    private int colorAt(int sample, double lateral) {
        double d = Math.abs(lateral);
        double half = TrackDrawing.TRACK_WIDTH / 2;
        int pit = pitColor(sample, lateral);
        if (pit != 0 && d > half + KERB) {
            return pit;
        }
        if (d < half - EDGE_LINE - 0.4) {
            return Palette.ASPHALT.getRGB();
        }
        if (d < half - 0.4) {
            return Palette.EDGE_LINE.getRGB();
        }
        if (d < half) {
            return Palette.ASPHALT.getRGB();
        }
        if (d < half + KERB) {
            if (kerb[sample]) {
                return ((sample / 2) % 2 == 0 ? Palette.KERB_RED : Palette.KERB_WHITE).getRGB();
            }
            return Palette.RUNOFF.getRGB();
        }
        boolean outside = inside[sample] != 0 && Math.signum(lateral) == -inside[sample];
        double gravelStart = half + KERB + RUNOFF * 0.5;
        if (outside && d >= gravelStart && d < gravelStart + GRAVEL) {
            return Palette.GRAVEL.getRGB();
        }
        if (d < half + KERB + RUNOFF) {
            return Palette.RUNOFF.getRGB();
        }
        return pit;
    }

    /**
     * Returns the pit lane, pit wall or garage color at a point, or 0 if the point is not part of the pits.
     * The lane runs beside the track from the pit entry to the exit, with ramps at each end that slide
     * from the track edge out to the lane.
     *
     * @param sample the nearest sample
     * @param lateral signed sideways distance in drawing units
     * @return an ARGB color, or 0
     */
    private int pitColor(int sample, double lateral) {
        Track track = drawing.getTrack();
        TrackProfile profile = track.getProfile();
        double lap = track.getLapLength();
        double s = track.getSample(sample).getDistance();
        double fromEntry = track.wrap(s - profile.getPitEntryDistance());
        double length = track.wrap(profile.getPitExitDistance() - profile.getPitEntryDistance()) + PIT_RAMP;
        if (fromEntry > length) {
            return 0;
        }
        int side = profile.getPitSide();
        double laneCenter = side * TrackDrawing.lateralToDrawing(Tuning.PIT_LANE_OFFSET);
        double edge = side * TrackDrawing.TRACK_WIDTH / 2;
        double center = laneCenter;
        if (fromEntry < PIT_RAMP) {
            center = edge + (laneCenter - edge) * fromEntry / PIT_RAMP;
        } else if (fromEntry > length - PIT_RAMP) {
            center = edge + (laneCenter - edge) * (length - fromEntry) / PIT_RAMP;
        }
        double across = (lateral - center) * side;
        if (Math.abs(across) < PIT_WIDTH / 2) {
            return Palette.PIT_LANE.getRGB();
        }
        boolean inLane = fromEntry >= PIT_RAMP && fromEntry <= length - PIT_RAMP;
        if (inLane && across < -PIT_WIDTH / 2 && across > -PIT_WIDTH / 2 - 1.2) {
            return Palette.EDGE_LINE.getRGB();
        }
        if (inLane && across > PIT_WIDTH / 2 && across < PIT_WIDTH / 2 + GARAGE_DEPTH) {
            double first = Tuning.PIT_BOXES_CENTER - Tuning.PIT_BOX_SPACING * (garageColors.size() - 1) / 2.0;
            double fromBox = Track.angleDifference(s / lap * 2 * Math.PI, 0) / (2 * Math.PI) * lap;
            for (int g = 0; g < garageColors.size(); g++) {
                if (Math.abs(fromBox - (first + g * Tuning.PIT_BOX_SPACING)) < GARAGE_LENGTH / 2) {
                    return garageColors.get(g).getRGB();
                }
            }
        }
        return 0;
    }

    /**
     * Blends two ARGB colors half and half (including transparency), to smooth band edges.
     *
     * @param a one color
     * @param b the other color
     * @return the blend
     */
    private static int mix(int a, int b) {
        int alpha = (((a >>> 24) & 0xFF) + ((b >>> 24) & 0xFF)) / 2;
        int red = channelMix(a, b, 16);
        int green = channelMix(a, b, 8);
        int blue = channelMix(a, b, 0);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Averages one color channel of two colors, weighting each by its opacity so a transparent
     * pixel does not darken the blend.
     *
     * @param a one color
     * @param b the other color
     * @param shift bit position of the channel
     * @return the averaged channel value
     */
    private static int channelMix(int a, int b, int shift) {
        int alphaA = (a >>> 24) & 0xFF;
        int alphaB = (b >>> 24) & 0xFF;
        if (alphaA + alphaB == 0) {
            return 0;
        }
        int va = (a >> shift) & 0xFF;
        int vb = (b >> shift) & 0xFF;
        return (va * alphaA + vb * alphaB) / (alphaA + alphaB);
    }
}
