import acm.graphics.GCompound;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.util.ArrayList;
import java.util.List;

/**
 * A small map of the whole track in a corner of the screen, with one dot per car.
 * The player's dot is larger and drawn last so it is never hidden.
 */
public final class MiniMap extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Space between the panel edge and the map, in pixels. */
    private static final double PAD = 12;

    /** Dot diameter for rival cars, in pixels. */
    private static final double DOT = 6;

    /** Dot diameter for the player's car, in pixels. */
    private static final double PLAYER_DOT = 10;

    /** The race being shown. */
    private final transient Race race;

    /** One dot per car, in the same order as race.getCars(). */
    private final transient List<GOval> dots = new ArrayList<GOval>();

    /** Pixels per meter on the minimap. */
    private final double scale;

    /** Pixel offsets that center the track inside the panel. */
    private final double offsetX;

    /** Vertical pixel offset that centers the track inside the panel. */
    private final double offsetY;

    /**
     * Builds the minimap.
     *
     * @param race the race
     * @param width panel width in pixels
     * @param height panel height in pixels
     */
    public MiniMap(Race race, double width, double height) {
        this.race = race;
        Track track = race.getTrack();
        GRect panel = new GRect(0, 0, width, height);
        panel.setFilled(true);
        panel.setFillColor(Palette.PANEL);
        panel.setColor(Palette.PANEL_EDGE);
        add(panel);

        double spanX = track.getMaxX() - track.getMinX();
        double spanY = track.getMaxY() - track.getMinY();
        scale = Math.min((width - 2 * PAD) / spanX, (height - 2 * PAD) / spanY);
        offsetX = (width - spanX * scale) / 2 - track.getMinX() * scale;
        offsetY = (height - spanY * scale) / 2 + track.getMaxY() * scale;

        GPolygon outline = new GPolygon();
        for (int i = 0; i < track.getSampleCount(); i += 3) {
            TrackSample s = track.getSample(i);
            outline.addVertex(mapX(s.getX()), mapY(s.getY()));
        }
        outline.setFilled(false);
        outline.setColor(Palette.MINIMAP_TRACK);
        add(outline);

        GOval playerDot = null;
        for (Car car : race.getCars()) {
            boolean player = car == race.getPlayerCar();
            double size = player ? PLAYER_DOT : DOT;
            GOval dot = new GOval(size, size);
            dot.setFilled(true);
            dot.setFillColor(player ? Palette.PLAYER : Palette.teamColor(car.getDriver().getTeam()));
            dot.setColor(player ? Palette.TEXT : Palette.CAR_OUTLINE);
            dots.add(dot);
            if (player) {
                playerDot = dot;
            } else {
                add(dot);
            }
        }
        add(playerDot);
    }

    /**
     * Moves every dot to its car's position.
     */
    public void update() {
        Track track = race.getTrack();
        List<Car> cars = race.getCars();
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            GOval dot = dots.get(i);
            double x = mapX(track.xAt(car.getDistance(), 0));
            double y = mapY(track.yAt(car.getDistance(), 0));
            dot.setLocation(x - dot.getWidth() / 2, y - dot.getHeight() / 2);
        }
    }

    /**
     * Converts meters east to a pixel x on the minimap.
     *
     * @param x meters east
     * @return pixel x inside this compound
     */
    private double mapX(double x) {
        return offsetX + x * scale;
    }

    /**
     * Converts meters north to a pixel y on the minimap (flipped so north is up).
     *
     * @param y meters north
     * @return pixel y inside this compound
     */
    private double mapY(double y) {
        return offsetY - y * scale;
    }
}
