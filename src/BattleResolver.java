import java.util.Locale;
import java.util.Random;

/**
 * Decides how a battle ends, using the formula from the brief (Section 3.6):
 * passScore = paceAdvantage + stanceBonus + ersBonus + tireAdvantage + closeness
 *             - trackOvertakingDifficulty - defenderBonus - cornerDifficulty,
 * passProbability = logistic(k times passScore).
 * Before the pass roll, an attacking driver risks a lock-up, running wide or contact; that risk
 * grows with worn tires, hot brakes, a wet track and the driver's aggression.
 * All weights are named constants in Tuning, and the one random generator is the race's own.
 */
public final class BattleResolver {

    /**
     * Not instantiated; all methods are static.
     */
    private BattleResolver() {
    }

    /**
     * Works out the pass score for a battle in which the attacker attacks.
     *
     * @param battle the battle (stances must be set)
     * @param attackerGrip attacker's current tire and brake grip
     * @param defenderGrip defender's current tire and brake grip
     * @param attackerErs whether the attacker spends battery on the move
     * @param defenderErs whether the defender spends battery to defend
     * @param trackDifficulty the track's overtaking difficulty, 0 (easy) to 1 (hard)
     * @return the pass score; higher means a pass is more likely
     */
    public static double passScore(Battle battle, double attackerGrip, double defenderGrip,
                                   boolean attackerErs, boolean defenderErs, double trackDifficulty) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        double paceAdvantage = (speedScale(a) - speedScale(d)) * Tuning.PASS_PACE_WEIGHT;
        double tireAdvantage = (attackerGrip - defenderGrip) * Tuning.PASS_TIRE_WEIGHT;
        double closeness = (1.0 - battle.getGapSeconds() / Tuning.BATTLE_GAP_SECONDS) * Tuning.PASS_CLOSENESS_WEIGHT;
        double stanceBonus = battle.getAttackerStance() == Stance.ATTACK ? Tuning.PASS_ATTACK_BONUS : 0;
        double ersBonus = (attackerErs ? Tuning.PASS_ERS_BONUS : 0) - (defenderErs ? Tuning.PASS_ERS_DEFENCE : 0);
        double defenderBonus = battle.getDefenderStance() == Stance.DEFEND ? Tuning.PASS_DEFEND_BONUS : 0;
        double trackPenalty = trackDifficulty * Tuning.PASS_TRACK_WEIGHT;
        double cornerPenalty = battle.getCorner().getDifficulty() * Tuning.PASS_CORNER_WEIGHT;
        return Tuning.PASS_BASE_SCORE + paceAdvantage + stanceBonus + ersBonus + tireAdvantage + closeness
                - trackPenalty - defenderBonus - cornerPenalty;
    }

    /**
     * Turns a pass score into a probability with the logistic curve 1 / (1 + e to the minus k score).
     * A score of 0 means a coin flip; large positive scores approach certainty.
     *
     * @param score the pass score
     * @return probability from 0 to 1
     */
    public static double passProbability(double score) {
        return 1.0 / (1.0 + Math.exp(-Tuning.PASS_LOGISTIC_K * score));
    }

    /**
     * Chance that an attacking move goes wrong (lock-up, running wide or contact).
     *
     * @param attacker the attacking car
     * @param wetness track wetness from 0 to 1
     * @return risk from 0 to 1
     */
    public static double attackRisk(Car attacker, double wetness) {
        double brakeHeat = Math.max(0, attacker.getBrakes().getTemperature() - Tuning.RISK_BRAKE_TEMP);
        double risk = Tuning.RISK_BASE
                + attacker.getTire().getWear() * Tuning.RISK_PER_WEAR
                + brakeHeat / Tuning.RISK_BRAKE_SPAN
                + wetness * Tuning.RISK_PER_WETNESS
                + (attacker.getAggression() - 0.5) * Tuning.RISK_PER_AGGRESSION;
        return Math.max(0, Math.min(Tuning.RISK_MAX, risk));
    }

    /**
     * Resolves a battle and records the outcome and message on it.
     *
     * @param battle the battle (both stances set)
     * @param attackerGrip attacker's current grip
     * @param defenderGrip defender's current grip
     * @param attackerErs whether the attacker spends battery
     * @param defenderErs whether the defender spends battery
     * @param trackDifficulty the track's overtaking difficulty
     * @param wetness track wetness at the corner
     * @param random the race's random generator
     * @return the outcome
     */
    public static BattleOutcome resolve(Battle battle, double attackerGrip, double defenderGrip,
                                        boolean attackerErs, boolean defenderErs, double trackDifficulty,
                                        double wetness, Random random) {
        BattleOutcome outcome;
        if (battle.getAttackerStance() != Stance.ATTACK) {
            outcome = BattleOutcome.NO_ATTEMPT;
        } else if (random.nextDouble() < attackRisk(battle.getAttacker(), wetness)) {
            double kind = random.nextDouble();
            if (kind < Tuning.INCIDENT_LOCKUP_SHARE) {
                outcome = BattleOutcome.LOCKED_UP;
            } else if (kind < Tuning.INCIDENT_LOCKUP_SHARE + Tuning.INCIDENT_RUN_WIDE_SHARE) {
                outcome = BattleOutcome.RAN_WIDE;
            } else {
                outcome = BattleOutcome.CONTACT;
            }
        } else {
            double score = passScore(battle, attackerGrip, defenderGrip, attackerErs, defenderErs, trackDifficulty);
            outcome = random.nextDouble() < passProbability(score) ? BattleOutcome.PASSED : BattleOutcome.HELD;
        }
        battle.finish(outcome, describe(battle, outcome));
        return outcome;
    }

    /**
     * Writes the one-line result, using "You" for the player's car.
     *
     * @param battle the battle
     * @param outcome how it ended
     * @return the message
     */
    public static String describe(Battle battle, BattleOutcome outcome) {
        String a = name(battle.getAttacker(), true);
        String aObj = name(battle.getAttacker(), false);
        String d = name(battle.getDefender(), true);
        String dObj = name(battle.getDefender(), false);
        int turn = battle.getCorner().getNumber();
        switch (outcome) {
            case PASSED:
                return String.format(Locale.ROOT, "%s lunged past %s into Turn %d", a, dObj, turn);
            case HELD:
                if (battle.getDefenderStance() == Stance.DEFEND) {
                    return String.format(Locale.ROOT, "%s defended Turn %d and held off %s", d, turn, aObj);
                }
                return String.format(Locale.ROOT, "%s tried at Turn %d but %s held on", a, turn, dObj);
            case NO_ATTEMPT:
                if (battle.getDefenderStance() == Stance.DEFEND) {
                    return String.format(Locale.ROOT, "%s covered Turn %d but %s never came: time lost", d, turn, aObj);
                }
                return String.format(Locale.ROOT, "%s stayed behind %s through Turn %d", a, dObj, turn);
            case LOCKED_UP:
                return String.format(Locale.ROOT, "%s locked up into Turn %d, lost %.1f s", a, turn, Tuning.LOCKUP_TIME_LOSS);
            case RAN_WIDE:
                return String.format(Locale.ROOT, "%s ran wide at Turn %d, lost %.1f s", a, turn, Tuning.RUN_WIDE_TIME_LOSS);
            case CONTACT:
                return String.format(Locale.ROOT, "Contact! %s and %s touched at Turn %d", a, dObj, turn);
            default:
                return String.format(Locale.ROOT, "Blue flags: %s lets %s through", d, aObj);
        }
    }

    /**
     * Returns the name used in messages: "You" or "you" for the player, otherwise the driver code.
     *
     * @param car the car
     * @param subject true at the start of a sentence
     * @return the name
     */
    private static String name(Car car, boolean subject) {
        if (car.isPlayer()) {
            return subject ? "You" : "you";
        }
        return car.getDriver().getCode();
    }

    /**
     * Returns a car's speed multiplier from race pace and push level (not ERS).
     *
     * @param car the car
     * @return speed scale
     */
    private static double speedScale(Car car) {
        return car.getPaceFactor() * car.getPushLevel().getPace();
    }
}
