import acm.graphics.GCompound;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The scrolling, zooming world: striped grass, the track picture, the start line and grid marks,
 * and the cars on top.
 * Positions come from TrackDrawing in drawing units, then are multiplied by the zoom to get pixels,
 * with y flipped so north is up. Zooming calls GCompound.scale, which this acm.jar applies to every
 * child (including the track picture), so nothing static has to be rebuilt.
 * Drawing order (back to front): grass stripes, track picture, start line and grid, cars.
 */
public final class WorldView extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Height of each mowed-grass stripe, in drawing units. */
    private static final double GRASS_STRIPE = 60.0;

    /** How far the grass stripes reach beyond the track, in drawing units. */
    private static final double GRASS_MARGIN = 4000.0;

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
        add(TrackRaster.build(drawing, race.getPitLane().getTeams()));
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
        for (CarSprite sprite : sprites) {
            add(sprite.getAlert());
        }
    }

    /**
     * Adds every part of a car sprite: its exhaust glow, ring (player only) and body parts.
     *
     * @param sprite the sprite
     */
    private void addSprite(CarSprite sprite) {
        add(sprite.getGlow());
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
     * Moves every car sprite to its car's current position and shows the attack warning on one car.
     *
     * @param warned the rival about to attack the player, or null
     */
    public void updateCars(Car warned) {
        for (CarSprite sprite : sprites) {
            sprite.setAlert(sprite.getCar() == warned);
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
     * Draws alternating light and dark grass bands across a wide area, like a mowed lawn.
     * The canvas background is the dark grass; only the light bands are added.
     */
    private void addGrassStripes() {
        double left = drawing.minX() - GRASS_MARGIN;
        double width = drawing.maxX() - drawing.minX() + 2 * GRASS_MARGIN;
        int stripe = 0;
        for (double y = drawing.minY() - GRASS_MARGIN; y < drawing.maxY() + GRASS_MARGIN; y += GRASS_STRIPE) {
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
            double center = TrackDrawing.lateralToDrawing(lateral);
            add(band(d, 1.2, center - 3.0, center + 3.0, Palette.EDGE_LINE));
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
