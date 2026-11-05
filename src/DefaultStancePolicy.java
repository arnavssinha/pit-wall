import java.util.Random;

/**
 * Picks a battle stance when nobody chooses one: for computer drivers always, and for the player
 * when the prompt times out (and in the Strategy Lab, where it stands in for the player).
 * Computer drivers attack more when they are close, on better tires, or more aggressive; they
 * defend more when they can read that the car behind is about to attack.
 * The player's default follows the baseline push level and tire state (brief, Section 3.6).
 */
public final class DefaultStancePolicy {

    /**
     * Not instantiated; all methods are static.
     */
    private DefaultStancePolicy() {
    }

    /**
     * Chooses the attacker's stance.
     *
     * @param attacker the car behind
     * @param defender the car ahead
     * @param gapSeconds time gap between them
     * @param random the race's random generator
     * @return the stance
     */
    public static Stance attackerStance(Car attacker, Car defender, double gapSeconds, Random random) {
        boolean tiresOk = attacker.getTire().getWear() < Tuning.WEAR_CLIFF;
        if (attacker.isPlayer()) {
            return playerDefault(attacker, gapSeconds, true);
        }
        double tireEdge = defender.getTire().getWear() - attacker.getTire().getWear();
        double chance = attacker.getAggression() * Tuning.AI_ATTACK_SCALE
                + (gapSeconds < Tuning.DEFAULT_ATTACK_GAP ? Tuning.AI_CLOSE_ATTACK_BONUS : 0)
                + tireEdge * Tuning.AI_TIRE_EDGE_WEIGHT
                - (tiresOk ? 0 : Tuning.AI_WORN_TIRE_CAUTION);
        if (random.nextDouble() >= chance) {
            return Stance.HOLD;
        }
        return random.nextDouble() < Tuning.AI_OUTSIDE_CHANCE ? Stance.OUTSIDE : Stance.ATTACK;
    }

    /**
     * The player's stance when the prompt runs out, from the baseline push level and tire state:
     * Push means fight (Attack or Defend), Conserve means Hold, and Balanced fights only when close
     * (and, when attacking, only on tires that are not past the cliff). Uses no randomness, so the
     * prompt can show it in advance.
     *
     * @param player the player's car
     * @param gapSeconds time gap to the rival
     * @param attacking true if the player is the attacker
     * @return the default stance
     */
    public static Stance playerDefault(Car player, double gapSeconds, boolean attacking) {
        Stance fight = attacking ? Stance.ATTACK : Stance.DEFEND;
        if (player.getPushLevel() == PushLevel.PUSH) {
            return fight;
        }
        if (player.getPushLevel() == PushLevel.CONSERVE) {
            return Stance.HOLD;
        }
        if (attacking) {
            boolean tiresOk = player.getTire().getWear() < Tuning.WEAR_CLIFF;
            return gapSeconds < Tuning.DEFAULT_ATTACK_GAP && tiresOk ? fight : Stance.HOLD;
        }
        return gapSeconds < Tuning.DEFAULT_DEFEND_GAP ? fight : Stance.HOLD;
    }

    /**
     * Chooses the defender's stance.
     *
     * @param defender the car ahead
     * @param attacker the car behind
     * @param gapSeconds time gap between them
     * @param attackerIntent what the attacker is planning, which a computer driver reads correctly
     *                       most of the time
     * @param random the race's random generator
     * @return the stance
     */
    public static Stance defenderStance(Car defender, Car attacker, double gapSeconds, Stance attackerIntent,
                                        Random random) {
        if (defender.isPlayer()) {
            return playerDefault(defender, gapSeconds, false);
        }
        boolean coming = attackerIntent == Stance.ATTACK || attackerIntent == Stance.OUTSIDE;
        boolean readsAttack = coming && random.nextDouble() < Tuning.AI_READ_ACCURACY;
        boolean guesses = !coming && random.nextDouble() < Tuning.AI_FALSE_DEFEND;
        return readsAttack || guesses ? Stance.DEFEND : Stance.HOLD;
    }
}
