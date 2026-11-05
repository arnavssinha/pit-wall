import acm.graphics.GCompound;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The scrolling, zooming world: grass, run-off, gravel, the track itself, and the cars.
 * Positions come from TrackDrawing in drawing units, then are multiplied by the zoom to get
 * pixels, with y flipped so north is up. Zooming calls GCompound.scale, which this acm.jar
 * applies to every child, so the static scenery never has to be rebuilt.
 * Static scenery is kept to a few hundred polygons: long strips are drawn in chunks of about
 * 200 m, and kerbs and gravel only where the track bends.
 * Drawing order (back to front): grass stripes, run-off, gravel, asphalt, kerbs, lines, cars.
 */
public final class WorldView extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Track samples per chunk polygon. */
    private static final int CHUNK_SAMPLES = 40;

    /** Width of a kerb strip in drawing units. */
    private static final double KERB_WIDTH = 2.0;

    /** Width of the paved run-off beside the track, in drawing units. */
    private static final double RUNOFF_WIDTH = 7.0;

    /** Width of the gravel traps on the outside of corners, in drawing units. */
    private static final double GRAVEL_WIDTH = 16.0;

    /**
     * Samples a gravel trap may run on past the end of its corner, but only along straight track,
     * so it never wraps into the next corner of a chicane.
     */
    private static final int GRAVEL_RUN_ON = 6;

    /** Height of each mowed-grass stripe, in drawing units. */
    private static final double GRASS_STRIPE = 60.0;

    /** Number of squares across the chequered start line. */
    private static final int CHECKER_SQUARES = 10;

    /** Converts track meters to drawing units. */
    private final transient TrackDrawing drawing;

    /** One sprite per car. */
    private final transient List<CarSprite> sprites = new ArrayList<CarSprite>();

    /** Current zoom in pixels per drawing unit. */
    private double zoom;

    /**
     * Builds the scenery and the car sprites at a zoom of 1 pixel per drawing unit.
     *
     * @param race the race to draw
     * @param drawing converts track meters to drawing units
     */
    public WorldView(Race race, TrackDrawing drawing) {
        this.drawing = drawing;
        this.zoom = 1.0;
        addGrassStripes();
        addRunoff();
        addGravel();
        addAsphalt();
        addKerbs();
        addEdgeLines();
        addStartLineAndGrid(race.getConfig().getCarCount());
        CarSprite playerSprite = null;
        for (Car car : race.getCars()) {
            CarSprite sprite = new CarSprite(car, car == race.getPlayerCar());
            sprites.add(sprite);
            if (car == race.getPlayerCar()) {
                playerSprite = sprite;
            } else {
                addSprite(sprite);
            }
        }
        addSprite(playerSprite);
    }

    /**
     * Adds every part of a car sprite, plus its ring if it has one.
     *
     * @param sprite the sprite
     */
    private void addSprite(CarSprite sprite) {
        GOval ring = sprite.getRing();
        if (ring != null) {
            add(ring);
        }
        for (GPolygon part : sprite.getParts()) {
            add(part);
        }
    }

    /**
     * Changes the zoom by scaling every child about the world origin.
     *
     * @param newZoom pixels per drawing unit
     */
    public void setZoom(double newZoom) {
        if (Math.abs(newZoom - zoom) > 1e-6) {
            scale(newZoom / zoom);
            zoom = newZoom;
        }
    }

    /**
     * Returns the current zoom.
     *
     * @return pixels per drawing unit
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * Moves every car sprite to its car's current position.
     */
    public void updateCars() {
        for (CarSprite sprite : sprites) {
            sprite.update(drawing, zoom);
        }
    }

    /**
     * Converts a drawing x to a pixel x inside this compound.
     *
     * @param x drawing units east
     * @return pixels
     */
    public double toPixelX(double x) {
        return x * zoom;
    }

    /**
     * Converts a drawing y to a pixel y inside this compound (flipped so north is up).
     *
     * @param y drawing units north
     * @return pixels
     */
    public double toPixelY(double y) {
        return -y * zoom;
    }

    /**
     * Adds a vertex at a sideways offset from the centerline to a polygon, at zoom 1.
     *
     * @param poly the polygon
     * @param sample index of the centerline sample
     * @param offset drawing units to the left of the direction of travel
     */
    private void addEdgeVertex(GPolygon poly, int sample, double offset) {
        double d = drawing.getTrack().getSample(sample).getDistance();
        poly.addVertex(drawing.offsetX(d, offset), -drawing.offsetY(d, offset));
    }

    /**
     * Builds a filled strip between two sideways offsets over a range of samples.
     *
     * @param from first sample index
     * @param to last sample index (may run past the end; it wraps)
     * @param inner offset of one edge in drawing units
     * @param outer offset of the other edge in drawing units
     * @param color fill and outline color
     * @return the polygon, already added
     */
    private GPolygon strip(int from, int to, double inner, double outer, Color color) {
        GPolygon poly = new GPolygon();
        for (int i = from; i <= to; i++) {
            addEdgeVertex(poly, i, outer);
        }
        for (int i = to; i >= from; i--) {
            addEdgeVertex(poly, i, inner);
        }
        poly.setFilled(true);
        poly.setFillColor(color);
        poly.setColor(color);
        add(poly);
        return poly;
    }

    /**
     * Draws alternating light and dark grass bands across the whole area, like a mowed lawn.
     */
    private void addGrassStripes() {
        double margin = 4000;
        double left = drawing.minX() - margin;
        double width = drawing.maxX() - drawing.minX() + 2 * margin;
        int stripe = 0;
        for (double y = drawing.minY() - margin; y < drawing.maxY() + margin; y += GRASS_STRIPE) {
            if (stripe % 2 == 1) {
                GRect band = new GRect(left, -(y + GRASS_STRIPE), width, GRASS_STRIPE);
                band.setFilled(true);
                band.setFillColor(Palette.GRASS_LIGHT);
                band.setColor(Palette.GRASS_LIGHT);
                add(band);
            }
            stripe++;
        }
    }

    /**
     * Draws the paved run-off area on both sides of the whole lap.
     */
    private void addRunoff() {
        double outer = TrackDrawing.TRACK_WIDTH / 2 + KERB_WIDTH + RUNOFF_WIDTH;
        int n = drawing.getTrack().getSampleCount();
        for (int start = 0; start < n; start += CHUNK_SAMPLES) {
            strip(start, start + CHUNK_SAMPLES + 1, -outer, outer, Palette.RUNOFF);
        }
    }

    /**
     * Draws gravel traps on the outside of each corner: one strip per run of bending samples.
     * The outside of a left-hand bend is the right side, and the other way round.
     */
    private void addGravel() {
        Track track = drawing.getTrack();
        int n = track.getSampleCount();
        double inner = TrackDrawing.TRACK_WIDTH / 2 + KERB_WIDTH + RUNOFF_WIDTH * 0.5;
        int i = 0;
        while (i < n) {
            double k = track.getSample(i).getCurvature();
            if (Math.abs(k) < Tuning.CORNER_CURVATURE) {
                i++;
                continue;
            }
            int start = i;
            while (i < n && Math.abs(track.getSample(i).getCurvature()) >= Tuning.CORNER_CURVATURE
                    && Math.signum(track.getSample(i).getCurvature()) == Math.signum(k)) {
                i++;
            }
            int from = start;
            while (from > start - GRAVEL_RUN_ON && isStraight(from - 1)) {
                from--;
            }
            int to = i;
            while (to < i + GRAVEL_RUN_ON && isStraight(to + 1)) {
                to++;
            }
            double side = k > 0 ? -1 : 1;
            strip(from, to, side * inner, side * (inner + GRAVEL_WIDTH), Palette.GRAVEL);
        }
    }

    /**
     * Returns whether a sample is on a near-straight part of the track.
     *
     * @param index sample index (wrapped)
     * @return true if the bend there is gentler than a corner
     */
    private boolean isStraight(int index) {
        return Math.abs(drawing.getTrack().getSample(index).getCurvature()) < Tuning.CORNER_CURVATURE / 2;
    }

    /**
     * Draws the asphalt as overlapping chunks so there are no hairline gaps between them.
     */
    private void addAsphalt() {
        double half = TrackDrawing.TRACK_WIDTH / 2;
        int n = drawing.getTrack().getSampleCount();
        for (int start = 0; start < n; start += CHUNK_SAMPLES) {
            strip(start, start + CHUNK_SAMPLES + 1, -half, half, Palette.ASPHALT);
        }
    }

    /**
     * Draws red and white kerbs on both edges wherever the track bends sharply.
     */
    private void addKerbs() {
        Track track = drawing.getTrack();
        double half = TrackDrawing.TRACK_WIDTH / 2;
        for (int i = 0; i < track.getSampleCount(); i++) {
            if (Math.abs(track.getSample(i).getCurvature()) < Tuning.CORNER_CURVATURE) {
                continue;
            }
            Color color = i % 2 == 0 ? Palette.KERB_RED : Palette.KERB_WHITE;
            strip(i, i + 1, half, half + KERB_WIDTH, color);
            strip(i, i + 1, -half, -(half + KERB_WIDTH), color);
        }
    }

    /**
     * Draws the two white edge lines as unfilled closed polygons around the whole lap.
     */
    private void addEdgeLines() {
        double offset = TrackDrawing.TRACK_WIDTH / 2 - 0.5;
        for (int side = -1; side <= 1; side += 2) {
            GPolygon line = new GPolygon();
            for (int i = 0; i < drawing.getTrack().getSampleCount(); i++) {
                addEdgeVertex(line, i, side * offset);
            }
            line.setFilled(false);
            line.setColor(Palette.EDGE_LINE);
            add(line);
        }
    }

    /**
     * Draws the chequered start/finish line and a white bracket for each grid slot.
     *
     * @param slots number of grid slots
     */
    private void addStartLineAndGrid(int slots) {
        double half = TrackDrawing.TRACK_WIDTH / 2;
        double square = TrackDrawing.TRACK_WIDTH / CHECKER_SQUARES;
        double depth = square / TrackDrawing.LAYOUT_SCALE;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < CHECKER_SQUARES; col++) {
                Color c = (row + col) % 2 == 0 ? Color.WHITE : Palette.CAR_OUTLINE;
                double left = -half + col * square;
                add(band(row * depth, depth, left, left + square, c));
            }
        }
        for (int i = 0; i < slots; i++) {
            double d = -(Tuning.GRID_FIRST_SLOT_OFFSET + i * Tuning.GRID_SLOT_SPACING)
                    + TrackDrawing.CAR_LENGTH / (2 * TrackDrawing.LAYOUT_SCALE) + 1.0;
            double lateral = (i % 2 == 0 ? 1 : -1) * Tuning.GRID_LATERAL_OFFSET;
            double center = lateral * TrackDrawing.TRACK_WIDTH / Tuning.TRACK_WIDTH;
            add(band(d, 0.8, center - 2.4, center + 2.4, Palette.EDGE_LINE));
        }
    }

    /**
     * Builds a small quad across the track at an exact distance, used for painted lines.
     *
     * @param distance distance along the lap in meters
     * @param depth length of the band along the track in meters
     * @param left sideways offset of one end in drawing units
     * @param right sideways offset of the other end in drawing units
     * @param color fill color
     * @return the polygon, not yet added
     */
    private GPolygon band(double distance, double depth, double left, double right, Color color) {
        GPolygon poly = new GPolygon();
        double[][] corners = {{distance, left}, {distance + depth, left}, {distance + depth, right}, {distance, right}};
        for (double[] c : corners) {
            poly.addVertex(drawing.offsetX(c[0], c[1]), -drawing.offsetY(c[0], c[1]));
        }
        poly.setFilled(true);
        poly.setFillColor(color);
        poly.setColor(color);
        return poly;
    }
}
