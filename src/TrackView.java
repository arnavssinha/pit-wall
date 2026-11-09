import acm.graphics.GCompound;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import java.awt.Color;

/**
 * Draws the world: grass, the track, the pit lane, the cars and the line choices in a fight.
 * Everything is in one GCompound that moves so the player's car stays in the middle of the screen.
 */
public final class TrackView extends GCompound {

    /** Pixels per meter. */
    private static final double ZOOM = 1.8;

    /** Drawn car length and width in meters (bigger than real so they are easy to see). */
    private static final double CAR_LENGTH = 14;

    /** Drawn car width in meters. */
    private static final double CAR_WIDTH = 6;

    /** Meters between the dots that show a line choice. */
    private static final double DOT_SPACING = 15;

    /** Dots per line choice. */
    private static final int DOTS = 45;

    /** Colors of the three line choices: A inside, S optimal, D outside. */
    public static final Color[] LINE_COLORS = {new Color(0x40C4FF), Color.WHITE, new Color(0xFF4FD8)};

    /** The race being drawn. */
    private final Race race;

    /** One polygon per car, in the same order as race.cars. */
    private final GPolygon[] carShapes;

    /** The angle each car shape is turned to now, in degrees. */
    private final double[] carAngles;

    /** Yellow arrow over the player's car. */
    private final GPolygon marker = new GPolygon();

    /** Dots for the three line choices: dots[line][i]. */
    private final GOval[][] dots = new GOval[3][DOTS];

    /** Builds the drawing of a race. */
    public TrackView(Race race) {
        this.race = race;
        Track track = race.track;
        GRect grass = new GRect(-2000, -3000, 5000, 5000);
        grass.setFilled(true);
        grass.setFillColor(new Color(0x2E6B34));
        grass.setColor(new Color(0x2E6B34));
        add(grass);
        add(strip(-Track.WIDTH / 2 + 2, Track.WIDTH / 2 - 2, 0, track.length(), new Color(0x4A4A4E)));
        add(strip(Race.PIT_LANE - 6, -Track.WIDTH / 2, -260, 260, new Color(0x55555A)));
        add(strip(-Track.WIDTH / 2, -Track.WIDTH / 2 + 2, 0, track.length(), Color.WHITE));
        add(strip(Track.WIDTH / 2 - 2, Track.WIDTH / 2, 0, track.length(), Color.WHITE));
        add(strip(-Track.WIDTH / 2, Track.WIDTH / 2, 0, 3, Color.WHITE));
        for (int line = 0; line < 3; line++) {
            for (int i = 0; i < DOTS; i++) {
                dots[line][i] = new GOval(6, 6);
                dots[line][i].setFilled(true);
                dots[line][i].setColor(LINE_COLORS[line]);
                dots[line][i].setVisible(false);
                add(dots[line][i]);
            }
        }
        carShapes = new GPolygon[race.cars.size()];
        carAngles = new double[race.cars.size()];
        for (int i = 0; i < carShapes.length; i++) {
            Car car = race.cars.get(i);
            GPolygon shape = new GPolygon();
            double l = CAR_LENGTH * ZOOM / 2;
            double w = CAR_WIDTH * ZOOM / 2;
            shape.addVertex(l, 0);
            shape.addVertex(l * 0.4, -w * 0.5);
            shape.addVertex(-l, -w);
            shape.addVertex(-l, w);
            shape.addVertex(l * 0.4, w * 0.5);
            shape.setFilled(true);
            shape.setFillColor(new Color(car.color));
            carShapes[i] = shape;
            add(shape);
        }
        marker.addVertex(0, 0);
        marker.addVertex(-8, -12);
        marker.addVertex(8, -12);
        marker.setFilled(true);
        marker.setFillColor(Color.YELLOW);
        add(marker);
    }

    /**
     * Returns a filled polygon covering the road between two sideways offsets and two distances.
     * The track is a list of points, so one polygon going up one side and back down the other works.
     */
    private GPolygon strip(double fromLateral, double toLateral, double fromDistance, double toDistance, Color color) {
        Track track = race.track;
        GPolygon strip = new GPolygon();
        for (double d = fromDistance; d <= toDistance; d += 5) {
            strip.addVertex(screenX(track.x(d, fromLateral)), screenY(track.y(d, fromLateral)));
        }
        for (double d = toDistance; d >= fromDistance; d -= 5) {
            strip.addVertex(screenX(track.x(d, toLateral)), screenY(track.y(d, toLateral)));
        }
        strip.setFilled(true);
        strip.setFillColor(color);
        strip.setColor(color);
        return strip;
    }

    /** Converts meters east to pixels. */
    private static double screenX(double x) {
        return x * ZOOM;
    }

    /** Converts meters north to pixels (screen y goes down). */
    private static double screenY(double y) {
        return -y * ZOOM;
    }

    /** Moves the cars, the player marker, the line dots and the camera. */
    public void update(double screenWidth, double screenHeight) {
        Track track = race.track;
        for (int i = 0; i < carShapes.length; i++) {
            Car car = race.cars.get(i);
            double angle = Math.toDegrees(track.heading(car.distance));
            carShapes[i].rotate(angle - carAngles[i]);
            carAngles[i] = angle;
            carShapes[i].setLocation(screenX(track.x(car.distance, car.lateral)), screenY(track.y(car.distance, car.lateral)));
            carShapes[i].setColor(tireColor(car.tire));
        }
        Car p = race.player;
        double px = screenX(track.x(p.distance, p.lateral));
        double py = screenY(track.y(p.distance, p.lateral));
        marker.setLocation(px, py - 16);
        marker.sendToFront();
        showLines();
        setLocation(screenWidth / 2 - px, screenHeight / 2 - py);
    }

    /** Shows the three line choices as dotted lines while the player is in a fight; the chosen one is bigger. */
    private void showLines() {
        Fight fight = race.getPlayerFight();
        Car p = race.player;
        for (int line = 0; line < 3; line++) {
            Fight.Line choice = Fight.Line.values()[line];
            Fight.Line chosen = fight == null ? null : fight.attacker == p ? fight.attackLine : fight.defendLine;
            double size = choice == chosen ? 9 : 5;
            for (int i = 0; i < DOTS; i++) {
                double d = p.distance + 20 + i * DOT_SPACING;
                boolean show = fight != null && d < fight.decideAt + 60;
                dots[line][i].setVisible(show);
                if (show) {
                    double lateral = race.laneFor(fight, choice, p);
                    dots[line][i].setSize(size, size);
                    dots[line][i].setLocation(screenX(race.track.x(d, lateral)) - size / 2,
                            screenY(race.track.y(d, lateral)) - size / 2);
                }
            }
        }
    }

    /** Returns the outline color for a tire: the colors used in real F1. */
    public static Color tireColor(Compound tire) {
        switch (tire) {
            case SOFT:
                return new Color(0xE8002D);
            case MEDIUM:
                return new Color(0xFFD12E);
            case HARD:
                return Color.WHITE;
            case INTER:
                return new Color(0x43B02A);
            default:
                return new Color(0x0067AD);
        }
    }
}
