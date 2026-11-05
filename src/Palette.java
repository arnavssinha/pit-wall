import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;

/**
 * The game's colors and fonts in one place: a clean, retro-inspired top-down look.
 * Team colors arrive from the simulation as "#RRGGBB" text and are turned into colors here.
 */
public final class Palette {

    /** Open DRS flap on a car's rear wing. */
    public static final Color DRS = new Color(70, 235, 120);

    /** Grass around the track. */
    public static final Color GRASS = new Color(46, 92, 58);

    /** Lighter grass for the mowed stripes. */
    public static final Color GRASS_LIGHT = new Color(54, 104, 66);

    /** Paved run-off area beside the track. */
    public static final Color RUNOFF = new Color(88, 96, 92);

    /** Gravel traps on the outside of corners. */
    public static final Color GRAVEL = new Color(196, 178, 132);

    /** Track surface when soaked. */
    public static final Color WET_ASPHALT = new Color(36, 46, 64);

    /** Pit lane surface. */
    public static final Color PIT_LANE = new Color(70, 74, 82);

    /** Track surface. */
    public static final Color ASPHALT = new Color(58, 61, 69);

    /** White lines at the track edges. */
    public static final Color EDGE_LINE = new Color(225, 228, 232);

    /** Red kerb stripes. */
    public static final Color KERB_RED = new Color(206, 58, 58);

    /** White kerb stripes. */
    public static final Color KERB_WHITE = new Color(236, 236, 236);

    /** Panel background for the timing tower and other HUD boxes. */
    public static final Color PANEL = new Color(16, 20, 30, 225);

    /** Thin border around panels. */
    public static final Color PANEL_EDGE = new Color(70, 80, 100);

    /** Main HUD text. */
    public static final Color TEXT = new Color(237, 239, 244);

    /** Secondary HUD text, such as gaps and hints. */
    public static final Color TEXT_DIM = new Color(150, 158, 175);

    /** Highlight for the player's car and row. */
    public static final Color PLAYER = new Color(255, 210, 74);

    /** Background of the player's row in the timing tower. */
    public static final Color PLAYER_ROW = new Color(255, 210, 74, 60);

    /** Outline drawn around rival cars. */
    public static final Color CAR_OUTLINE = new Color(20, 20, 24);

    /** Minimap track line. */
    public static final Color MINIMAP_TRACK = new Color(200, 205, 215);

    /** Gauge background. */
    public static final Color GAUGE_BACK = new Color(40, 46, 60);

    /** Green band marking a gauge's ideal range. */
    public static final Color GAUGE_BAND = new Color(52, 140, 82);

    /** Gauge fill when all is well. */
    public static final Color GOOD = new Color(92, 200, 120);

    /** Gauge fill or text for a warning. */
    public static final Color WARNING = new Color(250, 170, 60);

    /** Gauge fill or text for danger. */
    public static final Color DANGER = new Color(235, 70, 70);

    /** ERS battery fill. */
    public static final Color ERS = new Color(90, 170, 250);

    /** Button background. */
    public static final Color BUTTON = new Color(44, 52, 70);

    /** Background of a disabled button. */
    public static final Color BUTTON_DISABLED = new Color(30, 34, 44);

    /** Purple, used for a fastest sector. */
    public static final Color PURPLE = new Color(190, 110, 255);

    /** Fill of the ERS exhaust flame (see-through blue). */
    public static final Color ERS_GLOW = new Color(90, 190, 255, 150);

    /** Rain streaks. */
    public static final Color RAIN = new Color(190, 215, 255, 170);

    /** Large headline font. */
    public static final Font HEADLINE = new Font("SansSerif", Font.BOLD, 26);

    /** Normal HUD font. */
    public static final Font HUD = new Font("SansSerif", Font.BOLD, 14);

    /** Small HUD font. */
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);

    /** Fixed-width font for timing numbers, so digits line up. */
    public static final Font MONO = new Font("Monospaced", Font.BOLD, 13);

    /** Colors already parsed from team hex strings. */
    private static final HashMap<String, Color> TEAM_COLORS = new HashMap<String, Color>();

    /**
     * Not instantiated; this class only holds constants.
     */
    private Palette() {
    }

    /** Background of menu and setup screens. */
    public static final Color SCREEN_BACK = new Color(20, 24, 34);

    /** Huge title font. */
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 64);

    /**
     * Makes a label with a font and color, ready to add.
     *
     * @param text the text
     * @param font the font
     * @param color the color
     * @return the label
     */
    public static GLabel label(String text, Font font, Color color) {
        GLabel label = new GLabel(text);
        label.setFont(font);
        label.setColor(color);
        return label;
    }

    /**
     * Makes a filled panel rectangle in the standard panel style, ready to add.
     *
     * @param width width in pixels
     * @param height height in pixels
     * @return the rectangle
     */
    public static GRect panel(double width, double height) {
        GRect rect = new GRect(width, height);
        rect.setFilled(true);
        rect.setFillColor(PANEL);
        rect.setColor(PANEL_EDGE);
        return rect;
    }

    /**
     * Makes a full-screen background rectangle for menu-style screens.
     *
     * @param width canvas width
     * @param height canvas height
     * @return the rectangle
     */
    public static GRect background(double width, double height) {
        GRect rect = new GRect(width, height);
        rect.setFilled(true);
        rect.setFillColor(SCREEN_BACK);
        rect.setColor(SCREEN_BACK);
        return rect;
    }

    /**
     * Returns the traditional color for a tire compound: red Soft, yellow Medium, white Hard,
     * green Intermediate, blue Wet.
     *
     * @param compound the compound
     * @return its color
     */
    public static Color compoundColor(Compound compound) {
        switch (compound) {
            case SOFT:
                return new Color(230, 50, 50);
            case MEDIUM:
                return new Color(250, 210, 50);
            case HARD:
                return new Color(240, 240, 240);
            case INTERMEDIATE:
                return new Color(60, 190, 80);
            default:
                return new Color(50, 120, 230);
        }
    }

    /**
     * Returns the color for a team.
     *
     * @param team the team
     * @return its livery color, or light grey if the hex text is malformed
     */
    public static Color teamColor(Team team) {
        String hex = team.getColorHex();
        Color color = TEAM_COLORS.get(hex);
        if (color == null) {
            try {
                color = new Color(Integer.parseInt(hex.replace("#", ""), 16));
            } catch (NumberFormatException e) {
                color = Color.LIGHT_GRAY;
            }
            TEAM_COLORS.put(hex, color);
        }
        return color;
    }
}
