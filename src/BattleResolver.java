import java.util.Locale;
import java.util.Random;

/**
 * Decides how a battle ends, using the formula from the brief (Section 3.6):
 * passScore = paceAdvantage + stanceBonus + ersBonus + tireAdvantage + closeness + overlap
 *             - trackOvertakingDifficulty - defenderBonus - cornerDifficulty,
 * where overlap rewards an attacker that has drawn alongside (or ahead) by the braking point.
 * An inside lunge has the biggest bonus and full risk; going around the outside has a smaller bonus
 * and much less risk, suffers less from the corner, and is hard for a defender covering the inside to stop.
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
     * @param overlap how far alongside the attacker is at the braking point: 0 nose to tail
     *                (as close as cars may follow), +1 level, -1 a further minimum gap back
     * @return the pass score; higher means a pass is more likely
     */
    public static double passScore(Battle battle, double attackerGrip, double defenderGrip,
                                   boolean attackerErs, boolean defenderErs, double trackDifficulty, double overlap) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        double paceAdvantage = (speedScale(a) - speedScale(d)) * Tuning.PASS_PACE_WEIGHT;
        double tireAdvantage = (attackerGrip - defenderGrip) * Tuning.PASS_TIRE_WEIGHT;
        double closeness = (1.0 - battle.getGapSeconds() / Tuning.BATTLE_GAP_SECONDS) * Tuning.PASS_CLOSENESS_WEIGHT;
        boolean outside = battle.getAttackerStance() == Stance.OUTSIDE;
        double stanceBonus = outside ? Tuning.PASS_OUTSIDE_BONUS
                : battle.getAttackerStance() == Stance.ATTACK ? Tuning.PASS_ATTACK_BONUS : 0;
        double ersBonus = (attackerErs ? Tuning.PASS_ERS_BONUS : 0) - (defenderErs ? Tuning.PASS_ERS_DEFENCE : 0);
        double defenderBonus = battle.getDefenderStance() == Stance.DEFEND ? Tuning.PASS_DEFEND_BONUS : 0;
        if (outside) {
            defenderBonus *= Tuning.OUTSIDE_DEFEND_SHARE;
        }
        double trackPenalty = trackDifficulty * Tuning.PASS_TRACK_WEIGHT;
        double cornerPenalty = battle.getCorner().getDifficulty() * Tuning.PASS_CORNER_WEIGHT
                * (outside ? Tuning.OUTSIDE_CORNER_SHARE : 1.0);
        double overlapBonus = Math.max(-1, Math.min(1, overlap)) * Tuning.PASS_OVERLAP_WEIGHT;
        return Tuning.PASS_BASE_SCORE + paceAdvantage + stanceBonus + ersBonus + tireAdvantage + closeness
                + overlapBonus - trackPenalty - defenderBonus - cornerPenalty;
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
     * @param stance the attacker's stance (an outside move is much safer)
     * @return risk from 0 to 1
     */
    public static double attackRisk(Car attacker, double wetness, Stance stance) {
        double brakeHeat = Math.max(0, attacker.getBrakes().getTemperature() - Tuning.RISK_BRAKE_TEMP);
        double risk = Tuning.RISK_BASE
                + attacker.getTire().getWear() * Tuning.RISK_PER_WEAR
                + brakeHeat / Tuning.RISK_BRAKE_SPAN
                + wetness * Tuning.RISK_PER_WETNESS
                + (attacker.getAggression() - 0.5) * Tuning.RISK_PER_AGGRESSION;
        if (stance == Stance.OUTSIDE) {
            risk *= Tuning.OUTSIDE_RISK_SHARE;
        }
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
     * @param overlap how far alongside the attacker is (see passScore)
     * @param random the race's random generator
     * @return the outcome
     */
    public static BattleOutcome resolve(Battle battle, double attackerGrip, double defenderGrip,
                                        boolean attackerErs, boolean defenderErs, double trackDifficulty,
                                        double wetness, double overlap, Random random) {
        BattleOutcome outcome;
        Stance stance = battle.getAttackerStance();
        if (stance != Stance.ATTACK && stance != Stance.OUTSIDE) {
            outcome = BattleOutcome.NO_ATTEMPT;
        } else if (random.nextDouble() < attackRisk(battle.getAttacker(), wetness, stance)) {
            double kind = random.nextDouble();
            if (kind < Tuning.INCIDENT_LOCKUP_SHARE) {
                outcome = BattleOutcome.LOCKED_UP;
            } else if (kind < Tuning.INCIDENT_LOCKUP_SHARE + Tuning.INCIDENT_RUN_WIDE_SHARE) {
                outcome = BattleOutcome.RAN_WIDE;
            } else {
                outcome = BattleOutcome.CONTACT;
            }
        } else {
            double score = passScore(battle, attackerGrip, defenderGrip, attackerErs, defenderErs, trackDifficulty, overlap);
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
                if (battle.getAttackerStance() == Stance.OUTSIDE) {
                    return String.format(Locale.ROOT, "%s went around the outside of %s at Turn %d!", a, dObj, turn);
                }
                return String.format(Locale.ROOT, "%s lunged down the inside of %s into Turn %d", a, dObj, turn);
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
