/**
 * The line a car takes into a corner during a fight, chosen by the player or a computer driver.
 * Inside is the shorter, tighter way in (it wins the corner if alongside but exits slowly);
 * Outside is the long way round (it needs to be ahead at the apex but keeps speed);
 * Optimal is the normal racing line (for an attacker: sit in the tow and get the better exit);
 * Stay behind means no attack this time.
 */
public enum LineChoice {

    /** The inside of the corner. */
    INSIDE("Inside"),

    /** The normal racing line. */
    OPTIMAL("Optimal"),

    /** The outside of the corner. */
    OUTSIDE("Outside"),

    /** No attack: drop back a little and save the tires. */
    STAY_BEHIND("Stay behind");

    /** Name shown to the player. */
    private final String displayName;

    /**
     * Creates a choice.
     *
     * @param displayName name shown to the player
     */
    LineChoice(String displayName) {
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

    /**
     * Returns whether this choice moves the car off the racing line.
     *
     * @return true for Inside and Outside
     */
    public boolean leavesLine() {
        return this == INSIDE || this == OUTSIDE;
    }
}
