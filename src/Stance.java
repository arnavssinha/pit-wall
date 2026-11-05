/**
 * A driver's choice for one corner during a battle (brief, Section 3.6).
 */
public enum Stance {

    /** Brake later and carry more speed: more chance to pass, more risk and more wear. */
    ATTACK("Attack"),

    /** Tighter line, earlier braking: much harder to pass, but costs a little time either way. */
    DEFEND("Defend"),

    /** The neutral stance: no bonus and no extra risk. */
    HOLD("Hold");

    /** Name shown to the player. */
    private final String displayName;

    /**
     * Creates a stance.
     *
     * @param displayName name shown to the player
     */
    Stance(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the name shown to the player.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
