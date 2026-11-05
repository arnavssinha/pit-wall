import acm.graphics.GCompound;
import acm.graphics.GPolygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The scrolling, zooming world: the track drawn as vector polygons, plus the cars.
 * Everything is placed in pixels equal to meters times the current zoom, with y flipped so north
 * is up. Zooming calls GCompound.scale, which this acm.jar applies to every child, so the static
 * track never has to be rebuilt. Static geometry is kept to a few hundred polygons: the asphalt
 * is drawn in chunks of about 200 m, and kerbs only where the track bends.
 */
public final class WorldView extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Track samples per asphalt chunk polygon. */
    private static final int CHUNK_SAMPLES = 40;

    /** Width of a kerb strip in meters. */
    private static final double KERB_WIDTH = 1.2;

    /** Samples per kerb stripe (each stripe is about 5 m long). */
    private static final int KERB_STRIPE_SAMPLES = 1;

    /** The track being drawn. */
    private final transient Track track;

    /** One sprite per car. */
    private final transient List<CarSprite> sprites = new ArrayList<CarSprite>();

    /** Current zoom in pixels per meter. */
    private double zoom;

    /**
     * Builds the track geometry and the car sprites at a zoom of 1 pixel per meter.
     *
     * @param race the race to draw
     */
    public WorldView(Race race) {
        this.track = race.getTrack();
        this.zoom = 1.0;
        addAsphalt();
        addKerbs();
        addEdgeLines();
        addStartLineAndGrid(race.getConfig().getCarCount());
        for (Car car : race.getCars()) {
            CarSprite sprite = new CarSprite(car, car == race.getPlayerCar());
            sprites.add(sprite);
        }
        for (CarSprite sprite : sprites) {
            if (sprite.getCar() != race.getPlayerCar()) {
                add(sprite.getBody());
            }
        }
        for (CarSprite sprite : sprites) {
            if (sprite.getCar() == race.getPlayerCar()) {
                add(sprite.getBody());
            }
        }
    }

    /**
     * Changes the zoom by scaling every child about the world origin.
     *
     * @param newZoom pixels per meter
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
     * @return pixels per meter
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * Moves every car sprite to its car's current position.
     */
    public void updateCars() {
        for (CarSprite sprite : sprites) {
            sprite.update(track, zoom);
        }
    }

    /**
     * Converts a track x coordinate to a pixel x inside this compound.
     *
     * @param x meters east
     * @return pixels
     */
    public double toPixelX(double x) {
        return x * zoom;
    }

    /**
     * Converts a track y coordinate to a pixel y inside this compound (flipped so north is up).
     *
     * @param y meters north
     * @return pixels
     */
    public double toPixelY(double y) {
        return -y * zoom;
    }

    /**
     * Adds a point at a lateral offset from the centerline to a polygon, at zoom 1.
     *
     * @param poly the polygon
     * @param sample index of the centerline sample
     * @param lateral meters to the left of the direction of travel
     */
    private void addEdgeVertex(GPolygon poly, int sample, double lateral) {
        TrackSample s = track.getSample(sample);
        double x = s.getX() - lateral * Math.sin(s.getHeading());
        double y = s.getY() + lateral * Math.cos(s.getHeading());
        poly.addVertex(x, -y);
    }

    /**
     * Builds a filled strip between two lateral offsets over a range of samples.
     *
     * @param from first sample index
     * @param to last sample index (may run past the end; it wraps)
     * @param inner lateral offset of one edge in meters
     * @param outer lateral offset of the other edge in meters
     * @return the polygon, not yet added
     */
    private GPolygon strip(int from, int to, double inner, double outer) {
        GPolygon poly = new GPolygon();
        for (int i = from; i <= to; i++) {
            addEdgeVertex(poly, i, outer);
        }
        for (int i = to; i >= from; i--) {
            addEdgeVertex(poly, i, inner);
        }
        poly.setFilled(true);
        return poly;
    }

    /**
     * Draws the asphalt as overlapping chunks so there are no hairline gaps between them.
     */
    private void addAsphalt() {
        double half = Tuning.TRACK_WIDTH / 2;
        int n = track.getSampleCount();
        for (int start = 0; start < n; start += CHUNK_SAMPLES) {
            GPolygon chunk = strip(start, start + CHUNK_SAMPLES + 1, -half, half);
            chunk.setFillColor(Palette.ASPHALT);
            chunk.setColor(Palette.ASPHALT);
            add(chunk);
        }
    }

    /**
     * Draws red and white kerbs on both edges wherever the track bends sharply.
     */
    private void addKerbs() {
        double half = Tuning.TRACK_WIDTH / 2;
        int n = track.getSampleCount();
        for (int i = 0; i < n; i += KERB_STRIPE_SAMPLES) {
            if (Math.abs(track.getSample(i).getCurvature()) < Tuning.CORNER_CURVATURE) {
                continue;
            }
            boolean red = (i / KERB_STRIPE_SAMPLES) % 2 == 0;
            for (int side = -1; side <= 1; side += 2) {
                GPolygon stripe = strip(i, i + KERB_STRIPE_SAMPLES, side * half, side * (half + KERB_WIDTH));
                stripe.setFillColor(red ? Palette.KERB_RED : Palette.KERB_WHITE);
                stripe.setColor(stripe.getFillColor());
                add(stripe);
            }
        }
    }

    /**
     * Draws the two white edge lines as unfilled closed polygons around the whole lap.
     */
    private void addEdgeLines() {
        double half = Tuning.TRACK_WIDTH / 2;
        for (int side = -1; side <= 1; side += 2) {
            GPolygon line = new GPolygon();
            for (int i = 0; i < track.getSampleCount(); i++) {
                addEdgeVertex(line, i, side * (half - 0.3));
            }
            line.setFilled(false);
            line.setColor(Palette.EDGE_LINE);
            add(line);
        }
    }

    /**
     * Draws the start/finish line and a short white mark for each grid slot.
     *
     * @param slots number of grid slots
     */
    private void addStartLineAndGrid(int slots) {
        double half = Tuning.TRACK_WIDTH / 2;
        add(band(0.0, 1.5, -half, half, Palette.EDGE_LINE));
        for (int i = 0; i < slots; i++) {
            double d = -(Tuning.GRID_FIRST_SLOT_OFFSET + i * Tuning.GRID_SLOT_SPACING) + Tuning.CAR_LENGTH / 2 + 0.6;
            double side = (i % 2 == 0 ? 1 : -1) * Tuning.GRID_LATERAL_OFFSET;
            add(band(d, 0.4, side - 1.2, side + 1.2, Palette.EDGE_LINE));
        }
    }

    /**
     * Builds a small quad across the track at an exact distance, used for painted lines.
     *
     * @param distance distance along the lap in meters
     * @param depth length of the band along the track in meters
     * @param left lateral offset of one end
     * @param right lateral offset of the other end
     * @param color fill color
     * @return the polygon
     */
    private GPolygon band(double distance, double depth, double left, double right, Color color) {
        GPolygon poly = new GPolygon();
        double[][] corners = {{distance, left}, {distance + depth, left}, {distance + depth, right}, {distance, right}};
        for (double[] c : corners) {
            poly.addVertex(track.xAt(c[0], c[1]), -track.yAt(c[0], c[1]));
        }
        poly.setFilled(true);
        poly.setFillColor(color);
        poly.setColor(color);
        return poly;
    }
}
