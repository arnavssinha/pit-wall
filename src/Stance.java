/**
 * A driver's choice for one corner during a battle (brief, Section 3.6).
 */
public enum Stance {

    /** Lunge down the inside under braking: the best chance to pass, the most risk and wear. */
    ATTACK("Inside"),

    /** Go around the outside: less likely to stick, but much safer and hard to cover from the inside. */
    OUTSIDE("Outside"),

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
