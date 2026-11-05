import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runs the battles in one race: decides when they open, collects stances, resolves them at the
 * braking point, and carries out the passes that result.
 * The same rules apply to every pair of cars, so computer cars fight each other exactly as they
 * fight the player, and the Strategy Lab can run battles with no screen at all.
 * Only one battle can involve the player at a time, and prompts have a cooldown so they never pile up.
 */
public final class BattleManager {

    /** The race this manager belongs to. */
    private final Race race;

    /** The race's random generator. */
    private final Random random;

    /** Battles waiting to be resolved. */
    private final List<Battle> open = new ArrayList<Battle>();

    /** The battle the player is choosing a stance for, or the last one, until the screen clears it. */
    private Battle playerBattle;

    /** Stance the player picked in advance for the next battle, or null. */
    private Stance preselected;

    /** Race time of the last player prompt. */
    private double lastPromptTime = -Double.MAX_VALUE;

    /** Number of resolved battles in which the attacker attacked. */
    private int attempts;

    /** Number of completed passes from battles (blue flags not counted). */
    private int passes;

    /**
     * Creates the manager.
     *
     * @param race the race
     * @param random the race's random generator
     */
    public BattleManager(Race race, Random random) {
        this.race = race;
        this.random = random;
    }

    /**
     * Runs once per step before cars move: updates computer drivers' plans and opens new battles.
     */
    public void beforeMove() {
        for (Car car : race.getCars()) {
            updateIntent(car);
        }
        for (Car car : race.getCars()) {
            maybeOpen(car);
        }
        for (Battle battle : open) {
            if (battle.promptsPlayer() && battle.isPlayerLocked() && battle.getPlayerStance() == null) {
                Car player = race.getPlayerCar();
                boolean attacking = battle.getAttacker() == player;
                Stance fallback = DefaultStancePolicy.playerDefault(player, battle.getGapSeconds(), attacking);
                if (attacking) {
                    battle.setAttackerStance(fallback);
                } else {
                    battle.setDefenderStance(fallback);
                }
            }
            engage(battle);
        }
    }

    /**
     * Turns stances into visible moves as soon as they are known. An attacking car first tucks into
     * the slipstream, then pulls out: down the inside of the coming corner for Inside, the other way
     * for Outside, and the two cars may then run side by side. A computer attacker that finds the
     * door already shut switches to the other side. A defending car moves across to cover the inside line. A
     * car that holds simply stays on the racing line.
     *
     * @param battle the battle
     */
    private void engage(Battle battle) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        int inside = battle.getCorner().getInsideSide();
        Stance defence = battle.getDefenderStance();
        if (defence != null && !battle.isDefenderEngaged()) {
            battle.markDefenderEngaged();
            if (defence == Stance.DEFEND) {
                d.setLateralTarget(inside * Tuning.COVER_LATERAL, Tuning.COVER_SPEED);
            }
        }
        Stance attack = battle.getAttackerStance();
        boolean attacking = attack == Stance.ATTACK || attack == Stance.OUTSIDE;
        if (attack != null && !battle.isAttackerEngaged()) {
            battle.markAttackerEngaged();
            if (attacking) {
                a.beginPass(d, true, a.getLateralOffset());
                a.returnToLine(Tuning.LATERAL_SPEED);
                if (d.getPassPartner() == null) {
                    d.pairWith(a);
                }
            }
        }
        if (attacking && !battle.isPulledOut() && a.getPassPartner() == d && timeToPullOut(battle)) {
            battle.markPulledOut();
            int side = attack == Stance.ATTACK ? inside : -inside;
            boolean doorShut = Math.abs(d.getLateralOffset() - side * Tuning.PULL_OUT_LATERAL) < Tuning.LATERAL_CLEARANCE;
            if (doorShut && !a.isPlayer()) {
                side = -side;
                battle.setAttackerStance(side == inside ? Stance.ATTACK : Stance.OUTSIDE);
            }
            a.setLateralTarget(side * Tuning.PULL_OUT_LATERAL, Tuning.LATERAL_SPEED);
        }
    }

    /**
     * Decides when an attacker leaves the slipstream: once it has closed right up on the car ahead,
     * or when the braking point is close. Staying in the tow until then is how it builds up the speed
     * to draw alongside.
     *
     * @param battle the battle
     * @return true when it is time to pull out
     */
    private boolean timeToPullOut(Battle battle) {
        Car a = battle.getAttacker();
        double gap = gapSeconds(a, battle.getDefender());
        double toBraking = (battle.getResolveAt() - a.getDistance()) / Math.max(1, a.getSpeed());
        return gap < Tuning.PULL_OUT_GAP_SECONDS || toBraking < Tuning.PULL_OUT_BEFORE_BRAKING_SECONDS;
    }

    /**
     * Runs once per step after cars move: resolves battles whose braking point has been reached and
     * finishes passes that are complete. Once a passing car is half a car length ahead, both cars head
     * back to the racing line (the side-by-side room rule stops them touching), so the passing car
     * moves across in front and the passed car tucks in behind, as in a real overtake. An attacker that
     * was told to back off but is already half a car ahead keeps the place instead (it has made the move stick).
     */
    public void afterMove() {
        for (int i = open.size() - 1; i >= 0; i--) {
            Battle battle = open.get(i);
            if (battle.getAttacker().getDistance() >= battle.getResolveAt()) {
                open.remove(i);
                resolve(battle);
            }
        }
        for (Car car : race.getCars()) {
            Car partner = car.getPassPartner();
            if (!car.isPassAttacker() || partner == null) {
                continue;
            }
            double half = race.getTrack().getLapLength() / 2;
            double ahead = race.trackGap(partner, car);
            double behind = race.trackGap(car, partner);
            if (car.isPassAborting() && ahead > Tuning.MIN_SEPARATION / 2 && ahead < half) {
                car.grantPass();
            }
            if (car.isPassGranted() && ahead > Tuning.MIN_SEPARATION / 2 && ahead < half) {
                if (!car.isFollowingLine()) {
                    car.returnToLine(Tuning.LATERAL_SPEED);
                }
                if (!partner.isFollowingLine() && !partner.isOffTrack()) {
                    partner.returnToLine(Tuning.LATERAL_SPEED);
                }
            }
            boolean clearAhead = car.isPassGranted() && ahead >= Tuning.MIN_SEPARATION && ahead < half;
            boolean clearBehind = car.isPassAborting() && behind >= Tuning.MIN_SEPARATION && behind < half;
            boolean broken = partner.isRetired() || car.isRetired() || partner.isInPitLane() || car.isInPitLane()
                    || partner.isFinished() || car.isFinished();
            if (clearAhead || clearBehind || broken) {
                partner.endPass();
                car.endPass();
            }
        }
    }

    /**
     * Lets a computer driver decide early what it will do at the next corner, so an attack can be
     * shown as a warning one corner before it happens.
     *
     * @param car the car
     */
    private void updateIntent(Car car) {
        if (car.isPlayer() || car.isFinished() || car.isRetired()) {
            return;
        }
        Car ahead = race.carAheadOnTrack(car);
        if (ahead == null || gapSeconds(car, ahead) > Tuning.INTENT_GAP_SECONDS) {
            return;
        }
        Corner next = nextCorner(car);
        if (next == null) {
            return;
        }
        int key = cornerKey(car, next);
        if (car.getIntentKey() != key) {
            car.setIntent(DefaultStancePolicy.attackerStance(car, ahead, gapSeconds(car, ahead), random), key);
        }
    }

    /**
     * Opens a battle for a car if it is close behind another car and near a braking point.
     *
     * @param attacker the car that might attack
     */
    private void maybeOpen(Car attacker) {
        if (race.getSafetyCar().isActive() || attacker.isRetired()) {
            return;
        }
        if (attacker.isFinished() || attacker.isInPitLane() || attacker.getPassPartner() != null
                || attacker.getSpeed() < 1 || involved(attacker)) {
            return;
        }
        Car defender = race.carAheadOnTrack(attacker);
        if (defender == null || defender.isOffTrack() || defender.getPassPartner() != null || involved(defender)) {
            return;
        }
        double gap = gapSeconds(attacker, defender);
        if (gap > Tuning.BATTLE_GAP_SECONDS) {
            return;
        }
        Corner corner = nextCorner(attacker);
        if (corner == null) {
            return;
        }
        double toBraking = race.getTrack().wrap(corner.getBrakingPoint() - attacker.getDistance());
        if (toBraking > attacker.getSpeed() * Tuning.BATTLE_OPEN_LEAD_SECONDS) {
            return;
        }
        int key = cornerKey(attacker, corner);
        if (attacker.getLastBattleKey() == key) {
            return;
        }
        attacker.setLastBattleKey(key);

        boolean lapped = race.getStandings().indexOf(defender) > race.getStandings().indexOf(attacker);
        if (lapped || defender.isFinished()) {
            giveWay(attacker, defender, corner);
            return;
        }
        boolean playerInvolved = attacker.isPlayer() || defender.isPlayer();
        boolean prompt = playerInvolved && race.getTime() - lastPromptTime >= Tuning.PROMPT_COOLDOWN_SECONDS;
        Battle battle = new Battle(attacker, defender, corner, attacker.getDistance() + toBraking, gap,
                race.getTime(), prompt);
        Stance attackPlan = attacker.getIntentKey() == key ? attacker.getIntent()
                : DefaultStancePolicy.attackerStance(attacker, defender, gap, random);
        if (!attacker.isPlayer()) {
            battle.setAttackerStance(attackPlan);
        }
        if (!defender.isPlayer()) {
            battle.setDefenderStance(DefaultStancePolicy.defenderStance(defender, attacker, gap, attackPlan, random));
        }
        if (prompt) {
            lastPromptTime = race.getTime();
            playerBattle = battle;
            if (preselected != null) {
                battle.choosePlayerStance(preselected);
                preselected = null;
            }
        }
        open.add(battle);
    }

    /**
     * Resolves a battle: fills in any missing stance, rolls the outcome, applies time losses, heat
     * and wear, starts the pass if there was one, and posts the result message.
     *
     * @param battle the battle
     */
    private void resolve(Battle battle) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        battle.lockPlayerStance();
        if (battle.getAttackerStance() == null) {
            battle.setAttackerStance(DefaultStancePolicy.attackerStance(a, d, battle.getGapSeconds(), random));
        }
        if (battle.getDefenderStance() == null) {
            battle.setDefenderStance(DefaultStancePolicy.defenderStance(d, a, battle.getGapSeconds(), null, random));
        }
        engage(battle);
        boolean attackerErs = usesErs(a);
        boolean defenderErs = battle.getDefenderStance() == Stance.DEFEND && usesErs(d);
        double wetness = race.getConditions().wetnessAt(battle.getCorner().getBrakingPoint());
        BattleOutcome outcome = BattleResolver.resolve(battle, a.getCurrentGrip(), d.getCurrentGrip(), attackerErs,
                defenderErs, race.getTrack().getProfile().getOvertakingDifficulty(), wetness, overlap(a, d), random);
        applyCosts(battle, outcome, attackerErs, defenderErs);
        boolean attacked = battle.getAttackerStance() == Stance.ATTACK || battle.getAttackerStance() == Stance.OUTSIDE;
        if (attacked) {
            attempts++;
        }
        if (outcome == BattleOutcome.PASSED) {
            passes++;
            a.grantPass();
            if (a.getPassPartner() == d) {
                double attackerSide = Math.signum(a.getLateralTarget());
                d.setLateralTarget(-attackerSide * Tuning.DEFENDER_LATERAL, Tuning.COVER_SPEED);
            }
        } else if (a.getPassPartner() == d) {
            a.abortPass();
        } else if (d.getPassPartner() == null && !d.isOffTrack()) {
            d.returnToLine(Tuning.COVER_SPEED);
        }
        boolean aboutPlayer = a.isPlayer() || d.isPlayer();
        boolean newsworthy = outcome == BattleOutcome.PASSED || outcome == BattleOutcome.CONTACT
                || (battle.promptsPlayer() && attacked);
        if (aboutPlayer && newsworthy) {
            race.addMessage(battle.getMessage(), true);
        }
    }

    /**
     * Measures how far alongside the attacker is. The minimum gap (Tuning.MIN_SEPARATION) is as close as
     * cars may follow, so being exactly that far behind means nose to tail, right on the gearbox:
     * that counts as 0. Level (alongside) counts as +1, and a further minimum gap back counts as -1.
     *
     * @param attacker the attacking car
     * @param defender the defending car
     * @return overlap from -1 to +1
     */
    private double overlap(Car attacker, Car defender) {
        double half = race.getTrack().getLapLength() / 2;
        double ahead = race.trackGap(defender, attacker);
        double meters = ahead < half ? ahead : -race.trackGap(attacker, defender);
        return Math.max(-1, Math.min(1, (meters + Tuning.MIN_SEPARATION) / Tuning.MIN_SEPARATION));
    }

    /**
     * Applies the time, heat, wear and battery costs of a battle.
     *
     * @param battle the battle
     * @param outcome how it ended
     * @param attackerErs whether the attacker spent battery
     * @param defenderErs whether the defender spent battery
     */
    private void applyCosts(Battle battle, BattleOutcome outcome, boolean attackerErs, boolean defenderErs) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        if (battle.getAttackerStance() == Stance.ATTACK) {
            a.getBrakes().addHeat(Tuning.ATTACK_BRAKE_HEAT);
            a.getTire().addWear(Tuning.ATTACK_TIRE_WEAR);
        } else if (battle.getAttackerStance() == Stance.OUTSIDE) {
            a.getTire().addWear(Tuning.ATTACK_TIRE_WEAR / 2);
        }
        if (battle.getDefenderStance() == Stance.DEFEND) {
            d.addTimePenalty(Tuning.DEFEND_TIME_COST);
            d.getTire().addWear(Tuning.DEFEND_TIRE_WEAR);
        }
        if (attackerErs) {
            a.getErs().spend(Tuning.ERS_BATTLE_COST);
        }
        if (defenderErs) {
            d.getErs().spend(Tuning.ERS_BATTLE_COST);
        }
        switch (outcome) {
            case HELD:
                a.addTimePenalty(Tuning.FAILED_ATTACK_TIME_COST);
                break;
            case LOCKED_UP:
                a.addTimePenalty(Tuning.LOCKUP_TIME_LOSS);
                break;
            case RAN_WIDE:
                a.addTimePenalty(Tuning.RUN_WIDE_TIME_LOSS);
                break;
            case CONTACT:
                a.addTimePenalty(Tuning.CONTACT_ATTACKER_LOSS);
                d.addTimePenalty(Tuning.CONTACT_DEFENDER_LOSS);
                break;
            default:
                break;
        }
    }

    /**
     * Lets a faster car through: a lapped or finished car gives way with no fight.
     *
     * @param attacker the faster car
     * @param defender the car giving way
     * @param corner the corner where it happens
     */
    private void giveWay(Car attacker, Car defender, Corner corner) {
        Battle battle = new Battle(attacker, defender, corner, attacker.getDistance(), 0, race.getTime(), false);
        battle.finish(BattleOutcome.BLUE_FLAG, BattleResolver.describe(battle, BattleOutcome.BLUE_FLAG));
        startPass(attacker, defender, corner);
        if (attacker.isPlayer() || defender.isPlayer()) {
            race.addMessage(battle.getMessage(), true);
        }
    }

    /**
     * Starts a granted pass for a car giving way: the faster car goes to the inside of the corner and
     * the slower car moves to the outside.
     *
     * @param attacker the passing car
     * @param defender the car being passed
     * @param corner the corner, which decides which side is the inside
     */
    private void startPass(Car attacker, Car defender, Corner corner) {
        int inside = corner.getInsideSide();
        attacker.beginPass(defender, true, inside * Tuning.PASS_LATERAL);
        defender.beginPass(attacker, false, -inside * Tuning.DEFENDER_LATERAL);
        attacker.grantPass();
    }

    /**
     * Decides whether a car spends battery in a battle: the player's deploy switch, or the computer's rule.
     *
     * @param car the car
     * @return true if it uses ERS and has enough charge
     */
    private static boolean usesErs(Car car) {
        boolean wants = car.isPlayer() ? car.isErsWanted() : AiDriver.usesErsInBattle(car);
        return wants && car.getErs().getCharge() >= Tuning.ERS_BATTLE_COST;
    }

    /**
     * Returns whether a car is already in an open battle.
     *
     * @param car the car
     * @return true if attacking or defending in an open battle
     */
    private boolean involved(Car car) {
        for (Battle b : open) {
            if (b.getAttacker() == car || b.getDefender() == car) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the time gap from one car to the car ahead of it.
     *
     * @param car the following car
     * @param ahead the car in front
     * @return seconds at the following car's speed
     */
    private double gapSeconds(Car car, Car ahead) {
        return race.trackGap(car, ahead) / Math.max(1.0, car.getSpeed());
    }

    /**
     * Returns the next corner whose braking point is ahead of a car.
     *
     * @param car the car
     * @return the next corner, or null if the track has no corners with braking zones
     */
    private Corner nextCorner(Car car) {
        Corner best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Corner corner : race.getCorners()) {
            double d = race.getTrack().wrap(corner.getBrakingPoint() - car.getDistance());
            if (d < bestDistance) {
                bestDistance = d;
                best = corner;
            }
        }
        return best;
    }

    /**
     * Gives each corner visit a unique number: the lap in which the car reaches its braking point,
     * times 1000, plus the corner number.
     *
     * @param car the car
     * @param corner the corner ahead
     * @return the key
     */
    private int cornerKey(Car car, Corner corner) {
        double atBraking = car.getDistance() + race.getTrack().wrap(corner.getBrakingPoint() - car.getDistance());
        int lap = (int) Math.floor(atBraking / race.getTrack().getLapLength());
        return lap * 1000 + corner.getNumber();
    }

    /**
     * Calls off every open battle, for example when the safety car comes out (no overtaking).
     */
    public void cancelAll() {
        for (Battle battle : open) {
            Car a = battle.getAttacker();
            Car d = battle.getDefender();
            if (a.getPassPartner() == d) {
                a.abortPass();
            } else if (d.getPassPartner() == null && !d.isOffTrack()) {
                d.returnToLine(Tuning.COVER_SPEED);
            }
        }
        open.clear();
        if (playerBattle != null && !playerBattle.isResolved()) {
            playerBattle = null;
        }
    }

    /**
     * Sets the stance the player wants in the next battle, or in the current one if a prompt is open.
     *
     * @param stance the stance
     */
    public void choosePlayerStance(Stance stance) {
        if (playerBattle != null && !playerBattle.isPlayerLocked()) {
            playerBattle.choosePlayerStance(stance);
        } else {
            preselected = stance;
        }
    }

    /**
     * Returns the player's current or most recent prompted battle.
     *
     * @return the battle, or null
     */
    public Battle getPlayerBattle() {
        return playerBattle;
    }

    /**
     * Forgets the player's last battle once the screen has shown its result.
     */
    public void clearPlayerBattle() {
        playerBattle = null;
    }

    /**
     * Returns the stance chosen in advance for the next battle.
     *
     * @return the stance, or null
     */
    public Stance getPreselected() {
        return preselected;
    }

    /**
     * Returns how many resolved battles had an attack.
     *
     * @return number of attacks
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns how many passes battles produced.
     *
     * @return number of passes
     */
    public int getPasses() {
        return passes;
    }
}
