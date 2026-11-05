import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;

/**
 * The game's colors and fonts in one place: a clean, retro-inspired top-down look.
 * Team colors arrive from the simulation as "#RRGGBB" text and are turned into colors here.
 */
public final class Palette {

    /** Grass around the track. */
    public static final Color GRASS = new Color(46, 92, 58);

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
