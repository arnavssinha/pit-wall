/**
 * One fight between two cars for one corner: the car behind (attacker) and the car ahead (defender)
 * each choose a line (LineChoice) and whether to brake late. The choices become LinePlans that the
 * cars actually drive; who gets the corner is then settled on the road (see BattleManager).
 * When the player is in the fight, the battle also holds the player's pending choice.
 */
public final class Battle {

    /** The car behind. */
    private final Car attacker;

    /** The car ahead. */
    private final Car defender;

    /** The corner fought over. */
    private final Corner corner;

    /** Race time the battle opened. */
    private final double openedAt;

    /** Gap in seconds when the battle opened. */
    private final double gapSeconds;

    /** True if the player chooses a line in this battle. */
    private final boolean promptsPlayer;

    /** The attacker's line, or null until chosen. */
    private LineChoice attackerLine;

    /** True if the attacker brakes late. */
    private boolean attackerLate;

    /** The defender's line, or null until chosen. */
    private LineChoice defenderLine;

    /** True if the defender brakes late. */
    private boolean defenderLate;

    /** True once the player has picked a line (or the choice was made for them). */
    private boolean playerDecided;

    /** True once the player's choice can no longer change. */
    private boolean playerLocked;

    /** True once a computer attacker has switched sides because the door was shut. */
    private boolean switchedSides;

    /** True if the attacker was already level or ahead before the corner (a drag pass on the straight). */
    private boolean passedBeforeBraking;

    /** True if one of the cars locked up. */
    private boolean lockUp;

    /** True if the two cars touched. */
    private boolean contact;

    /** True once the attacker switched on the Overtake boost for this fight. */
    private boolean overtake;

    /** The result once over, or null. */
    private BattleOutcome outcome;

    /** Radio text describing what happened. */
    private String message = "";

    /**
     * Opens a battle.
     *
     * @param attacker the car behind
     * @param defender the car ahead
     * @param corner the corner fought over
     * @param gapSeconds gap between them in seconds
     * @param openedAt race time now
     * @param promptsPlayer true if the player chooses a line
     */
    public Battle(Car attacker, Car defender, Corner corner, double gapSeconds, double openedAt, boolean promptsPlayer) {
        this.attacker = attacker;
        this.defender = defender;
        this.corner = corner;
        this.gapSeconds = gapSeconds;
        this.openedAt = openedAt;
        this.promptsPlayer = promptsPlayer;
    }

    /**
     * Sets the player's line, unless the choice is locked.
     *
     * @param line the chosen line
     */
    public void choosePlayerLine(LineChoice line) {
        if (!promptsPlayer || playerLocked || outcome != null) {
            return;
        }
        if (isPlayerAttacking()) {
            attackerLine = line;
        } else if (line != LineChoice.STAY_BEHIND) {
            defenderLine = line;
        }
        playerDecided = true;
    }

    /**
     * Switches the player's late braking on or off, unless the choice is locked.
     */
    public void togglePlayerLateBrake() {
        if (!promptsPlayer || playerLocked || outcome != null) {
            return;
        }
        if (isPlayerAttacking()) {
            attackerLate = !attackerLate;
        } else {
            defenderLate = !defenderLate;
        }
    }

    /**
     * Marks the player's choice as made (for example by the default when the timer runs out).
     */
    public void markPlayerDecided() {
        playerDecided = true;
    }

    /**
     * Locks the player's choice.
     */
    public void lockPlayer() {
        playerLocked = true;
        playerDecided = true;
    }

    /**
     * Returns whether the player is the attacker.
     *
     * @return true if the player's car is behind
     */
    public boolean isPlayerAttacking() {
        return attacker.isPlayer();
    }

    /**
     * Returns the player's line.
     *
     * @return the line, or null if not chosen
     */
    public LineChoice getPlayerLine() {
        return isPlayerAttacking() ? attackerLine : defenderLine;
    }

    /**
     * Returns whether the player brakes late.
     *
     * @return true for late braking
     */
    public boolean isPlayerLate() {
        return isPlayerAttacking() ? attackerLate : defenderLate;
    }

    /**
     * Returns the player's opponent.
     *
     * @return the other car
     */
    public Car getRival() {
        return attacker.isPlayer() ? defender : attacker;
    }

    /**
     * Returns the opponent's line.
     *
     * @return the line, or null
     */
    public LineChoice getRivalLine() {
        return isPlayerAttacking() ? defenderLine : attackerLine;
    }

    /**
     * Returns whether the opponent brakes late.
     *
     * @return true for late braking
     */
    public boolean isRivalLate() {
        return isPlayerAttacking() ? defenderLate : attackerLate;
    }

    /**
     * Sets the attacker's choice.
     *
     * @param line the line
     * @param late true to brake late
     */
    public void setAttackerChoice(LineChoice line, boolean late) {
        attackerLine = line;
        attackerLate = late;
    }

    /**
     * Sets the defender's choice.
     *
     * @param line the line
     * @param late true to brake late
     */
    public void setDefenderChoice(LineChoice line, boolean late) {
        defenderLine = line;
        defenderLate = late;
    }

    /**
     * Ends the battle.
     *
     * @param result the outcome
     * @param text radio text
     */
    public void finish(BattleOutcome result, String text) {
        outcome = result;
        message = text;
        playerLocked = true;
        playerDecided = true;
    }

    /**
     * Records that a computer attacker switched sides.
     */
    public void markSwitchedSides() {
        switchedSides = true;
    }

    /**
     * Returns whether a computer attacker switched sides.
     *
     * @return true after a switch
     */
    public boolean hasSwitchedSides() {
        return switchedSides;
    }

    /**
     * Records a pass completed on the straight before the corner.
     */
    public void markPassedBeforeBraking() {
        passedBeforeBraking = true;
    }

    /**
     * Returns whether the pass was completed on the straight.
     *
     * @return true for a drag pass
     */
    public boolean wasPassedBeforeBraking() {
        return passedBeforeBraking;
    }

    /**
     * Records a lock-up by either car.
     */
    public void markLockUp() {
        lockUp = true;
    }

    /**
     * Returns whether either car locked up.
     *
     * @return true after a lock-up
     */
    public boolean hadLockUp() {
        return lockUp;
    }

    /**
     * Records contact between the cars.
     */
    public void markContact() {
        contact = true;
    }

    /**
     * Returns whether the cars touched.
     *
     * @return true after contact
     */
    public boolean hadContact() {
        return contact;
    }

    /**
     * Records that the attacker is using the Overtake boost (which includes late braking).
     */
    public void markOvertake() {
        overtake = true;
        attackerLate = true;
    }

    /**
     * Returns whether the attacker is using the Overtake boost.
     *
     * @return true with the boost on
     */
    public boolean isOvertake() {
        return overtake;
    }

    /**
     * Returns the attacker.
     *
     * @return the car behind
     */
    public Car getAttacker() {
        return attacker;
    }

    /**
     * Returns the defender.
     *
     * @return the car ahead
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
     * Returns the gap when the battle opened.
     *
     * @return seconds
     */
    public double getGapSeconds() {
        return gapSeconds;
    }

    /**
     * Returns when the battle opened.
     *
     * @return race time in seconds
     */
    public double getOpenedAt() {
        return openedAt;
    }

    /**
     * Returns whether the player chooses a line.
     *
     * @return true for the player's battles
     */
    public boolean promptsPlayer() {
        return promptsPlayer;
    }

    /**
     * Returns whether the player has made a choice.
     *
     * @return true once decided
     */
    public boolean isPlayerDecided() {
        return playerDecided;
    }

    /**
     * Returns whether the player's choice is locked.
     *
     * @return true once locked
     */
    public boolean isPlayerLocked() {
        return playerLocked;
    }

    /**
     * Returns the attacker's line.
     *
     * @return the line, or null
     */
    public LineChoice getAttackerLine() {
        return attackerLine;
    }

    /**
     * Returns whether the attacker brakes late.
     *
     * @return true for late braking
     */
    public boolean isAttackerLate() {
        return attackerLate;
    }

    /**
     * Returns the defender's line.
     *
     * @return the line, or null
     */
    public LineChoice getDefenderLine() {
        return defenderLine;
    }

    /**
     * Returns whether the defender brakes late.
     *
     * @return true for late braking
     */
    public boolean isDefenderLate() {
        return defenderLate;
    }

    /**
     * Returns the outcome.
     *
     * @return the outcome, or null while open
     */
    public BattleOutcome getOutcome() {
        return outcome;
    }

    /**
     * Returns whether the battle is over.
     *
     * @return true once finished
     */
    public boolean isResolved() {
        return outcome != null;
    }

    /**
     * Returns the radio text.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }
}
