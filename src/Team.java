/**
 * A constructor team, such as McLaren or Ferrari.
 * The color is stored as text ("#RRGGBB") so the simulation never needs graphics classes;
 * the user interface turns it into a real color.
 */
public final class Team {

    /** Team name. */
    private final String name;

    /** Livery color as "#RRGGBB". */
    private final String colorHex;

    /**
     * Creates a team.
     *
     * @param name team name
     * @param colorHex livery color as "#RRGGBB"
     */
    public Team(String name, String colorHex) {
        this.name = name;
        this.colorHex = colorHex;
    }

    /**
     * Returns the team name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the livery color.
     *
     * @return the color as "#RRGGBB"
     */
    public String getColorHex() {
        return colorHex;
    }
}
