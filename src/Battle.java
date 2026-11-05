/**
 * One fight for one corner between an attacker and the car just ahead of it.
 * A battle opens shortly before the corner's braking point, collects both drivers' stances
 * (the player's from the prompt, computer drivers' from the default policy), and is resolved
 * when the attacker reaches the braking point.
 */
public final class Battle {

    /** The car trying to pass. */
    private final Car attacker;

    /** The car being attacked. */
    private final Car defender;

    /** The corner being fought over. */
    private final Corner corner;

    /** Attacker distance (total meters) at which the battle is resolved: the braking point. */
    private final double resolveAt;

    /** Time gap between the cars when the battle opened, in seconds. */
    private final double gapSeconds;

    /** Race time when the battle opened. */
    private final double openedAt;

    /** True if the player's car is one of the two cars and the player is being asked to choose. */
    private final boolean promptsPlayer;

    /** The attacker's stance. */
    private Stance attackerStance;

    /** The defender's stance. */
    private Stance defenderStance;

    /** True once the player's stance can no longer change. */
    private boolean playerLocked;

    /** How it ended, or null while open. */
    private BattleOutcome outcome;

    /** The one-line result shown on the radio. */
    private String message = "";

    /**
     * Opens a battle.
     *
     * @param attacker the car trying to pass
     * @param defender the car ahead
     * @param corner the corner
     * @param resolveAt attacker distance at which to resolve
     * @param gapSeconds time gap at opening
     * @param openedAt race time at opening
     * @param promptsPlayer true if the player chooses a stance for this battle
     */
    public Battle(Car attacker, Car defender, Corner corner, double resolveAt, double gapSeconds,
                  double openedAt, boolean promptsPlayer) {
        this.attacker = attacker;
        this.defender = defender;
        this.corner = corner;
        this.resolveAt = resolveAt;
        this.gapSeconds = gapSeconds;
        this.openedAt = openedAt;
        this.promptsPlayer = promptsPlayer;
    }

    /**
     * Sets the player's stance, if the prompt is still open.
     *
     * @param stance the chosen stance
     */
    public void choosePlayerStance(Stance stance) {
        if (!promptsPlayer || playerLocked || outcome != null) {
            return;
        }
        if (attacker.isPlayer()) {
            attackerStance = stance;
        } else {
            defenderStance = stance;
        }
    }

    /**
     * Ends the prompt: the player's stance can no longer change.
     */
    public void lockPlayerStance() {
        playerLocked = true;
    }

    /**
     * Returns the player's chosen stance.
     *
     * @return the stance, or null if not chosen yet
     */
    public Stance getPlayerStance() {
        return attacker.isPlayer() ? attackerStance : defenderStance;
    }

    /**
     * Returns the rival in a battle involving the player.
     *
     * @return the other car
     */
    public Car getRival() {
        return attacker.isPlayer() ? defender : attacker;
    }

    /**
     * Records the result.
     *
     * @param result how it ended
     * @param text the one-line message
     */
    public void finish(BattleOutcome result, String text) {
        outcome = result;
        message = text;
        playerLocked = true;
    }

    /**
     * Sets the attacker's stance.
     *
     * @param stance the stance
     */
    public void setAttackerStance(Stance stance) {
        attackerStance = stance;
    }

    /**
     * Sets the defender's stance.
     *
     * @param stance the stance
     */
    public void setDefenderStance(Stance stance) {
        defenderStance = stance;
    }

    /**
     * Returns the attacker.
     *
     * @return the attacking car
     */
    public Car getAttacker() {
        return attacker;
    }

    /**
     * Returns the defender.
     *
     * @return the defending car
     */
    public Car getDefender() {
        return defender;
    }

    /**
     * Returns the corner.
     *
     * @return the corner
     */
    public Corner getCorner() {
        return corner;
    }

    /**
     * Returns the attacker distance at which the battle resolves.
     *
     * @return total meters
     */
    public double getResolveAt() {
        return resolveAt;
    }

    /**
     * Returns the time gap when the battle opened.
     *
     * @return seconds
     */
    public double getGapSeconds() {
        return gapSeconds;
    }

    /**
     * Returns the race time when the battle opened.
     *
     * @return seconds
     */
    public double getOpenedAt() {
        return openedAt;
    }

    /**
     * Returns whether the player is choosing a stance for this battle.
     *
     * @return true for a player prompt
     */
    public boolean promptsPlayer() {
        return promptsPlayer;
    }

    /**
     * Returns whether the player's stance is locked.
     *
     * @return true once locked
     */
    public boolean isPlayerLocked() {
        return playerLocked;
    }

    /**
     * Returns the attacker's stance.
     *
     * @return the stance, or null if not set
     */
    public Stance getAttackerStance() {
        return attackerStance;
    }

    /**
     * Returns the defender's stance.
     *
     * @return the stance, or null if not set
     */
    public Stance getDefenderStance() {
        return defenderStance;
    }

    /**
     * Returns how the battle ended.
     *
     * @return the outcome, or null while open
     */
    public BattleOutcome getOutcome() {
        return outcome;
    }

    /**
     * Returns whether the battle has been resolved.
     *
     * @return true once resolved
     */
    public boolean isResolved() {
        return outcome != null;
    }

    /**
     * Returns the result message.
     *
     * @return the message, empty while open
     */
    public String getMessage() {
        return message;
    }
}
