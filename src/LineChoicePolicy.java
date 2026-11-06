import java.util.Random;

/**
 * How computer drivers (and the player's default, when the decision timer runs out) choose a line in
 * a fight. An attacker goes for it when it has an edge it can see: it is close, faster on the
 * straight (DRS, battery, tow), or on better tires; it dives inside most often, sometimes tries the
 * outside, and otherwise sits on the optimal line to get a better exit. A defender reads the
 * attacker's line most of the time and covers the inside when the attack comes that way.
 */
public final class LineChoicePolicy {

    /**
     * Not instantiated; all methods are static.
     */
    private LineChoicePolicy() {
    }

    /**
     * Works out how much of an edge the car behind has, as a rough score: positive is an advantage.
     *
     * @param attacker the car behind
     * @param defender the car ahead
     * @param gapSeconds gap in seconds
     * @return the edge score
     */
    public static double edge(Car attacker, Car defender, double gapSeconds) {
        double drs = attacker.isDrsOpen() || attacker.getDrsZone() >= 0 ? Tuning.AI_EDGE_DRS : 0;
        double battery = (attacker.getErs().getCharge() - defender.getErs().getCharge()) * Tuning.AI_EDGE_ERS;
        double grip = (attacker.getCurrentGrip() - defender.getCurrentGrip()) * Tuning.AI_EDGE_GRIP;
        double close = (Tuning.BATTLE_GAP_SECONDS - gapSeconds) * Tuning.AI_EDGE_CLOSE;
        return drs + battery + grip + close;
    }

    /**
     * Chooses a computer attacker's line.
     *
     * @param attacker the car behind
     * @param defender the car ahead
     * @param gapSeconds gap in seconds
     * @param random the race's random source
     * @return the line
     */
    public static LineChoice attackerLine(Car attacker, Car defender, double gapSeconds, Random random) {
        boolean tiresGone = attacker.getTire().getWear() >= Tuning.WEAR_CLIFF;
        double chance = Tuning.AI_ATTACK_BASE + attacker.getAggression() * Tuning.AI_ATTACK_SCALE
                + edge(attacker, defender, gapSeconds) - (tiresGone ? Tuning.AI_WORN_TIRE_CAUTION : 0);
        if (random.nextDouble() >= chance) {
            return gapSeconds < Tuning.DEFAULT_ATTACK_GAP ? LineChoice.OPTIMAL : LineChoice.STAY_BEHIND;
        }
        return random.nextDouble() < Tuning.AI_OUTSIDE_CHANCE ? LineChoice.OUTSIDE : LineChoice.INSIDE;
    }

    /**
     * Decides whether a computer driver brakes late: bolder drivers more often, never on worn tires.
     *
     * @param car the car
     * @param line its line
     * @param random the race's random source
     * @return true to brake late
     */
    public static boolean lateBrake(Car car, LineChoice line, Random random) {
        if (car.getTire().getWear() >= Tuning.WEAR_CLIFF || line == LineChoice.STAY_BEHIND) {
            return false;
        }
        double base = line == LineChoice.INSIDE ? Tuning.AI_LATE_BRAKE_INSIDE : Tuning.AI_LATE_BRAKE_OTHER;
        return random.nextDouble() < base * (0.5 + car.getAggression());
    }

    /**
     * Chooses a computer defender's line: it reads an attack down the inside most of the time and
     * covers it, sometimes covers when nothing is coming, and otherwise holds the optimal line.
     *
     * @param defender the car ahead
     * @param attackerLine the attacker's line, or null if not known yet
     * @param random the race's random source
     * @return the line
     */
    public static LineChoice defenderLine(Car defender, LineChoice attackerLine, Random random) {
        boolean inside = attackerLine == LineChoice.INSIDE;
        if (inside && random.nextDouble() < Tuning.AI_READ_ACCURACY) {
            return LineChoice.INSIDE;
        }
        if (attackerLine == LineChoice.OUTSIDE && random.nextDouble() < Tuning.AI_COVER_OUTSIDE) {
            return LineChoice.OUTSIDE;
        }
        return random.nextDouble() < Tuning.AI_FALSE_DEFEND ? LineChoice.INSIDE : LineChoice.OPTIMAL;
    }

    /**
     * The player's line when the decision timer runs out, from the player's push level: Push attacks
     * down the inside (or covers the inside), Conserve stays behind (or holds the line), Balanced attacks
     * only when close and otherwise sits on the optimal line.
     *
     * @param player the player's car
     * @param gapSeconds gap in seconds
     * @param attacking true if the player is behind
     * @return the line
     */
    public static LineChoice playerDefault(Car player, double gapSeconds, boolean attacking) {
        if (!attacking) {
            return player.getPushLevel() == PushLevel.PUSH ? LineChoice.INSIDE : LineChoice.OPTIMAL;
        }
        if (player.getPushLevel() == PushLevel.CONSERVE) {
            return LineChoice.STAY_BEHIND;
        }
        boolean close = gapSeconds < Tuning.DEFAULT_ATTACK_GAP && player.getTire().getWear() < Tuning.WEAR_CLIFF;
        return player.getPushLevel() == PushLevel.PUSH || close ? LineChoice.INSIDE : LineChoice.OPTIMAL;
    }
}
