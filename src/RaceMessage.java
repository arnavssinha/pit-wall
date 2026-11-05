/**
 * A short message produced by the race, such as a battle result, for the radio feed.
 */
public final class RaceMessage {

    /** Race time when it happened, in seconds. */
    private final double time;

    /** The text. */
    private final String text;

    /** True if it concerns the player's car. */
    private final boolean aboutPlayer;

    /**
     * Creates a message.
     *
     * @param time race time in seconds
     * @param text the text
     * @param aboutPlayer true if it concerns the player's car
     */
    public RaceMessage(double time, String text, boolean aboutPlayer) {
        this.time = time;
        this.text = text;
        this.aboutPlayer = aboutPlayer;
    }

    /**
     * Returns the race time.
     *
     * @return seconds
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns the text.
     *
     * @return the message
     */
    public String getText() {
        return text;
    }

    /**
     * Returns whether it concerns the player.
     *
     * @return true if about the player's car
     */
    public boolean isAboutPlayer() {
        return aboutPlayer;
    }
}
