import java.util.Locale;

/**
 * The rules of wheel-to-wheel racing, kept separate from the bookkeeping in BattleManager.
 * Who gets a corner is a "claim" worked out at turn-in from things the player can see: how far
 * alongside the car behind is, who is on the inside, the two cars' grip, who is braking late (and
 * whether that went wrong), and how hard the track is to pass on. Only a little randomness is added,
 * so a car properly alongside on the inside nearly always gets the corner and a car only at the
 * gearbox nearly never does, much like the real racing rules.
 */
public final class BattleResolver {

    /**
     * Not instantiated; all methods are static.
     */
    private BattleResolver() {
    }

    /**
     * Works out how strongly the car behind claims the corner. Positive means the car behind gets the
     * corner, negative means the car ahead keeps it. A car behind that is not attacking (no fight line,
     * or Stay behind) does not press its claim.
     *
     * @param rear the car behind (nose not yet past the other car)
     * @param front the car ahead
     * @param corner the corner being entered
     * @param gapMeters distance from the car behind to the car ahead along the track, 0 when level
     * @param trackDifficulty the track's overtaking difficulty (0 easy to 1 impossible)
     * @param noise a small random term (0 for the plain rules)
     * @return the claim
     */
    public static double claim(Car rear, Car front, Corner corner, double gapMeters, double trackDifficulty, double noise) {
        double alongside = Math.max(0, Math.min(1, 1 - gapMeters / Tuning.MIN_SEPARATION));
        double insideBy = (rear.getLateralOffset() - front.getLateralOffset()) * corner.getInsideSide();
        double side = Math.max(-1, Math.min(1, insideBy / Tuning.LATERAL_CLEARANCE));
        double late = lateBraking(rear) - lateBraking(front);
        double locked = lockedUp(front) - lockedUp(rear);
        LinePlan rearPlan = rear.getLinePlan();
        boolean rearAttacking = rearPlan != null && rearPlan.isAttacker() && rearPlan.getChoice() != LineChoice.STAY_BEHIND;
        double intent = rearAttacking ? 0 : -Tuning.CLAIM_NO_ATTACK_PENALTY;
        return Tuning.CLAIM_OVERLAP_WEIGHT * (alongside - Tuning.CLAIM_LEVEL_POINT)
                + Tuning.CLAIM_INSIDE_WEIGHT * side
                + Tuning.CLAIM_GRIP_WEIGHT * (rear.getCurrentGrip() - front.getCurrentGrip())
                + Tuning.CLAIM_LATE_BRAKE_WEIGHT * late
                + Tuning.CLAIM_LOCKUP_WEIGHT * locked
                - Tuning.CLAIM_TRACK_WEIGHT * trackDifficulty
                + intent
                + noise;
    }

    /**
     * Chance that a car braking late locks its brakes: higher on worn tires, with overheated brakes,
     * on a wet track and when pushing hard.
     *
     * @param car the late-braking car
     * @param wetness track wetness at the braking point, 0 to 1
     * @return probability from 0 to Tuning.RISK_MAX
     */
    public static double lockUpChance(Car car, double wetness) {
        double brakeHeat = Math.max(0, car.getBrakes().getTemperature() - Tuning.RISK_BRAKE_TEMP);
        double risk = Tuning.RISK_BASE
                + car.getTire().getWear() * Tuning.RISK_PER_WEAR
                + brakeHeat / Tuning.RISK_BRAKE_SPAN
                + wetness * Tuning.RISK_PER_WETNESS
                + (car.getPushLevel() == PushLevel.PUSH ? Tuning.RISK_WHEN_PUSHING : 0);
        return Math.max(0, Math.min(Tuning.RISK_MAX, risk));
    }

    /**
     * Describes lock-up risk in words, for the battle panel.
     *
     * @param car the car
     * @param wetness track wetness at the braking point
     * @return "low", "medium" or "high"
     */
    public static String riskWord(Car car, double wetness) {
        double risk = lockUpChance(car, wetness);
        return risk < Tuning.RISK_WORD_MEDIUM ? "low" : risk < Tuning.RISK_WORD_HIGH ? "medium" : "high";
    }

    /**
     * Writes the radio message for a finished battle.
     *
     * @param battle the battle
     * @param outcome how it ended
     * @return the message
     */
    public static String describe(Battle battle, BattleOutcome outcome) {
        String a = name(battle.getAttacker(), true);
        String d = name(battle.getDefender(), true);
        String dObj = name(battle.getDefender(), false);
        String aObj = name(battle.getAttacker(), false);
        int turn = battle.getCorner().getNumber();
        LineChoice line = battle.getAttackerLine();
        switch (outcome) {
            case PASSED:
                if (battle.wasPassedBeforeBraking()) {
                    return String.format(Locale.ROOT, "%s drove past %s on the straight before Turn %d", a, dObj, turn);
                }
                if (line == LineChoice.OUTSIDE) {
                    return String.format(Locale.ROOT, "%s went around the outside of %s at Turn %d!", a, dObj, turn);
                }
                if (line == LineChoice.INSIDE) {
                    String how = battle.isAttackerLate() ? "out-braked" : "dived down the inside of";
                    return String.format(Locale.ROOT, "%s %s %s into Turn %d", a, how, dObj, turn);
                }
                return String.format(Locale.ROOT, "%s got the better exit and passed %s after Turn %d", a, dObj, turn);
            case HELD:
                if (battle.getDefenderLine() == LineChoice.INSIDE) {
                    return String.format(Locale.ROOT, "%s covered the inside at Turn %d and held off %s", d, turn, aObj);
                }
                return String.format(Locale.ROOT, "%s kept %s behind through Turn %d", d, aObj, turn);
            case NO_ATTEMPT:
                return String.format(Locale.ROOT, "%s stayed behind %s through Turn %d", a, dObj, turn);
            case LOCKED_UP:
                LinePlan plan = battle.getAttacker().getLinePlan();
                String who = plan != null && plan.isLockedUp() ? a : d;
                return String.format(Locale.ROOT, "%s locked up into Turn %d and ran wide", who, turn);
            case CONTACT:
                return String.format(Locale.ROOT, "Contact! %s and %s touched at Turn %d", a, dObj, turn);
            default:
                return String.format(Locale.ROOT, "Blue flags: %s lets %s through", d, aObj);
        }
    }

    /**
     * Returns 1 if a car is braking late in its current fight line (and has not locked up), else 0.
     *
     * @param car the car
     * @return 1 or 0
     */
    private static double lateBraking(Car car) {
        LinePlan plan = car.getLinePlan();
        return plan != null && plan.isLateBrake() && !plan.isLockedUp() ? 1 : 0;
    }

    /**
     * Returns 1 if a car has locked up in its current fight line, else 0.
     *
     * @param car the car
     * @return 1 or 0
     */
    private static double lockedUp(Car car) {
        LinePlan plan = car.getLinePlan();
        return plan != null && plan.isLockedUp() ? 1 : 0;
    }

    /**
     * Returns a car's name for a message.
     *
     * @param car the car
     * @param subject true at the start of a sentence
     * @return "You", "you" or the three-letter code
     */
    private static String name(Car car, boolean subject) {
        if (car.isPlayer()) {
            return subject ? "You" : "you";
        }
        return car.getDriver().getCode();
    }
}
