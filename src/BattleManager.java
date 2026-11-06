import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Runs the fights between cars. A fight (Battle) opens when a car closes to within about a second of
 * the car ahead before a corner, or when the player asks to attack. Both drivers choose a line and
 * whether to brake late; the choices become LinePlans that the cars really drive. Nothing is decided
 * by a hidden dice roll at the braking point: the cars race on the road, and when two cars are side by
 * side at a corner's turn-in, the corner goes to the stronger claim (how far alongside, who is on the
 * inside, grip, late braking; see BattleResolver). The loser gives way and tucks in behind. The same
 * corner rule applies to any two cars side by side, fight or not, so a car can pass two or three cars
 * in a row, and a defender that covered the inside can be passed again on the exit (a switchback).
 */
public final class BattleManager {

    /** The race. */
    private final Race race;

    /** The race's random source, so a seed repeats exactly. */
    private final Random random;

    /** Battles under way. */
    private final List<Battle> open = new ArrayList<Battle>();

    /** Pairs of cars already given a ruling at a corner (pair and corner key packed in a long). */
    private final HashSet<Long> ruled = new HashSet<Long>();

    /** Each car's distance at the end of the last step, by car id, to spot braking points being passed. */
    private final double[] lastDistance;

    /** The player's current battle (kept for a moment after it ends so the result can be shown). */
    private Battle playerBattle;

    /** A line the player picked before any battle; used for the next one. */
    private LineChoice preselected;

    /** Race time the player was last asked to choose. */
    private double lastPromptTime = -Double.MAX_VALUE;

    /** Race time the player last completed a pass (a next attack straight after is offered at once). */
    private double lastPlayerPassTime = -Double.MAX_VALUE;

    /** True once the start lines into the first corner have been planned. */
    private boolean startPlanned;

    /** The player's line into the first corner. */
    private LineChoice startLine = LineChoice.OPTIMAL;

    /** True when the player pressed the attack key and the request has not been handled yet. */
    private boolean attackRequested;

    /** Number of attacks made (Inside or Outside), for statistics. */
    private int attempts;

    /** Number of fights the attacker won, for statistics. */
    private int passes;

    /**
     * Creates the manager.
     *
     * @param race the race
     * @param random the race's random source
     */
    public BattleManager(Race race, Random random) {
        this.race = race;
        this.random = random;
        this.lastDistance = new double[Tuning.MAX_CARS];
    }

    /**
     * Runs once per step before cars move: opens new battles, makes the choices that are due, and
     * turns choices into line plans.
     */
    public void beforeMove() {
        for (Car car : race.getCars()) {
            updateIntent(car);
        }
        if (attackRequested) {
            attackRequested = false;
            handleOvertakeRequest();
        }
        if (!startPlanned) {
            startPlanned = true;
            planStart();
        }
        if (race.isStartPhase()) {
            return;
        }
        for (Car car : race.getCars()) {
            maybeOpen(car, false);
        }
        for (Battle battle : open) {
            decide(battle);
            plan(battle);
        }
    }

    /**
     * Runs once per step after cars move: checks late brakers for lock-ups, gives rulings at
     * corners, ends yields, and finishes battles.
     */
    public void afterMove() {
        checkLockUps();
        ruleCorners();
        releaseYields();
        for (int i = open.size() - 1; i >= 0; i--) {
            Battle battle = open.get(i);
            BattleOutcome outcome = progress(battle);
            if (outcome != null) {
                open.remove(i);
                finish(battle, outcome);
            }
        }
        for (Car car : race.getCars()) {
            LinePlan plan = car.getLinePlan();
            if (plan != null && plan.isDone(car.getDistance()) && !involved(car)) {
                car.setLinePlan(null);
            }
            lastDistance[car.getId()] = car.getDistance();
        }
    }

    /**
     * Gives every car a line into the first corner, taken straight off the grid so the field fans out
     * two and three wide, which makes the start a real chance to gain places:
     * computer drivers pick Inside, Optimal or Outside and sometimes brake late; the player's line comes
     * from the A / S / D keys during the start (Optimal until chosen).
     */
    private void planStart() {
        Corner first = race.getStartCorner();
        if (first == null) {
            return;
        }
        for (Car car : race.getCars()) {
            LineChoice line;
            boolean late;
            if (car.isPlayer()) {
                line = preselected != null ? preselected : LineChoice.OPTIMAL;
                preselected = null;
                late = false;
                startLine = line;
            } else {
                double roll = random.nextDouble();
                line = roll < Tuning.START_INSIDE_SHARE ? LineChoice.INSIDE
                        : roll < Tuning.START_INSIDE_SHARE + Tuning.START_OUTSIDE_SHARE ? LineChoice.OUTSIDE : LineChoice.OPTIMAL;
                late = random.nextDouble() < Tuning.START_LATE_BRAKE_SHARE * (0.5 + car.getAggression());
            }
            LinePlan plan = new LinePlan(line, first, late, true, car.getDistance(), true, race.getTrack());
            plan.markStart();
            car.setLinePlan(plan);
        }
    }

    /**
     * Changes the player's line into the first corner during the start (before the braking point).
     *
     * @param line the line
     * @param late true to brake late (the Overtake boost)
     */
    private void replanStart(LineChoice line, boolean late) {
        Car player = race.getPlayerCar();
        Corner first = race.getStartCorner();
        LinePlan current = player.getLinePlan();
        if (first == null || (current != null && player.getDistance() >= current.getBrakeAt())) {
            return;
        }
        startLine = line;
        LinePlan plan = new LinePlan(line, first, late, true, player.getDistance(), true, race.getTrack());
        plan.markStart();
        player.setLinePlan(plan);
    }

    /**
     * Returns the player's line into the first corner.
     *
     * @return the line
     */
    public LineChoice getStartLine() {
        return startLine;
    }

    /**
     * Fills in the choices that are due: computer drivers choose at once (a defender facing the player
     * waits to see the player's line), and the player's default is used once the choice is locked.
     *
     * @param battle the battle
     */
    private void decide(Battle battle) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        double gap = gapSeconds(a, d);
        if (battle.promptsPlayer() && battle.isPlayerLocked() && battle.getPlayerLine() == null) {
            LineChoice fallback = LineChoicePolicy.playerDefault(battle.isPlayerAttacking(), battle.isOvertake());
            battle.choosePlayerLine(fallback);
            battle.markPlayerDecided();
            if (battle.getPlayerLine() == null) {
                battle.setDefenderChoice(LineChoice.OPTIMAL, false);
            }
        }
        if (battle.getAttackerLine() == null && !a.isPlayer()) {
            LineChoice line = a.getIntentKey() == cornerKey(a, battle.getCorner()) ? a.getIntent()
                    : LineChoicePolicy.attackerLine(a, d, gap, random);
            battle.setAttackerChoice(line, LineChoicePolicy.lateBrake(a, line, random));
            boolean boost = line.leavesLine()
                    && random.nextDouble() < Tuning.AI_OVERTAKE_USE * (0.5 + a.getAggression());
            if (boost && race.tryOvertake(a)) {
                battle.markOvertake();
            }
        }
        boolean attackerKnown = battle.getAttackerLine() != null && (!a.isPlayer() || battle.isPlayerDecided());
        if (battle.getDefenderLine() == null && !d.isPlayer() && attackerKnown) {
            LineChoice line = LineChoicePolicy.defenderLine(d, battle.getAttackerLine(), random);
            battle.setDefenderChoice(line, LineChoicePolicy.lateBrake(d, line, random) && random.nextDouble() < Tuning.AI_DEFEND_LATE_SHARE);
        }
        LineChoice attackLine = battle.getAttackerLine();
        boolean doorShut = attackLine != null && attackLine.leavesLine() && battle.getDefenderLine() == attackLine;
        if (doorShut && !a.isPlayer() && !battle.hasSwitchedSides() && random.nextDouble() < Tuning.AI_SWITCH_SIDES_CHANCE) {
            LinePlan plan = a.getLinePlan();
            if (plan == null || a.getDistance() < plan.getBrakeAt() - Tuning.LANE_SETTLE_BEFORE_BRAKING) {
                LineChoice other = attackLine == LineChoice.INSIDE ? LineChoice.OUTSIDE : LineChoice.INSIDE;
                battle.setAttackerChoice(other, battle.isAttackerLate());
            }
            battle.markSwitchedSides();
        }
        if (battle.promptsPlayer() && !battle.isPlayerLocked()) {
            LinePlan plan = race.getPlayerCar().getLinePlan();
            double lockAt = plan == null ? Double.MAX_VALUE : plan.getBrakeAt() - Tuning.PLAYER_LOCK_BEFORE_BRAKING;
            if (race.getPlayerCar().getDistance() >= lockAt) {
                battle.lockPlayer();
            }
        }
    }

    /**
     * Handles the player's Overtake key: in a fight as the attacker it switches the boost on for that
     * fight; otherwise it opens a fight with the car ahead (if close enough) and switches the boost on.
     * The player then picks a line (A, S or D).
     */
    private void handleOvertakeRequest() {
        Car player = race.getPlayerCar();
        if (race.isStartPhase()) {
            if (race.tryOvertake(player)) {
                replanStart(startLine, true);
            }
            return;
        }
        boolean attacking = playerBattle != null && !playerBattle.isResolved() && playerBattle.isPlayerAttacking();
        if (!attacking && !maybeOpen(player, true)) {
            race.addMessage("No car close enough to overtake (within " + Tuning.ON_DEMAND_ATTACK_GAP + " s).", true);
            return;
        }
        if (playerBattle == null || !playerBattle.isPlayerAttacking() || playerBattle.isResolved()) {
            return;
        }
        if (playerBattle.isOvertake()) {
            return;
        }
        if (!race.tryOvertake(player)) {
            race.addMessage(String.format(Locale.ROOT, "Not enough boost: it needs %.0f%% of the battery.",
                    Tuning.OVERTAKE_COST * 100), true);
            return;
        }
        playerBattle.markOvertake();
    }

    /**
     * Turns the battle's choices into line plans for both cars (and updates them if a choice changed
     * before the car reached the braking point).
     *
     * @param battle the battle
     */
    private void plan(Battle battle) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        if (battle.getAttackerLine() != null && (!a.isPlayer() || battle.isPlayerDecided())) {
            boolean early = battle.getAttackerLine().leavesLine() && hasStraightEdge(a);
            update(a, battle.getAttackerLine(), battle.isAttackerLate(), battle.getCorner(), true, early);
        }
        if (battle.getDefenderLine() != null && (!d.isPlayer() || battle.isPlayerDecided())) {
            update(d, battle.getDefenderLine(), battle.isDefenderLate(), battle.getCorner(), false, false);
        }
    }

    /**
     * Gives a car a new line plan if its choice differs from the plan it has, as long as it has not
     * yet reached the braking point.
     *
     * @param car the car
     * @param line the line
     * @param late true to brake late
     * @param corner the corner
     * @param attacker true for the car behind
     * @param early true to pull out of the tow straight away
     */
    private void update(Car car, LineChoice line, boolean late, Corner corner, boolean attacker, boolean early) {
        LinePlan current = car.getLinePlan();
        boolean same = current != null && current.getChoice() == line && current.isLateBrake() == late
                && current.getCorner() == corner && current.isAttacker() == attacker;
        if (same || car.getYieldTo() != null) {
            return;
        }
        if (current != null && current.getCorner() == corner && car.getDistance() >= current.getBrakeAt()) {
            return;
        }
        car.setLinePlan(new LinePlan(line, corner, late, attacker, car.getDistance(), early, race.getTrack()));
    }

    /**
     * Checks every late-braking car that has just reached its braking point for a lock-up.
     */
    private void checkLockUps() {
        for (Car car : race.getCars()) {
            LinePlan plan = car.getLinePlan();
            if (plan == null || !plan.isLateBrake() || plan.isLockedUp()) {
                continue;
            }
            double before = lastDistance[car.getId()];
            if (before < plan.getBrakeAt() && car.getDistance() >= plan.getBrakeAt()) {
                double wetness = race.getConditions().wetnessAt(car.getDistance());
                if (random.nextDouble() < BattleResolver.lockUpChance(car, wetness)) {
                    plan.lockUp();
                    car.getTire().addWear(Tuning.LOCKUP_TIRE_WEAR);
                    Battle battle = battleOf(car);
                    if (battle != null) {
                        battle.markLockUp();
                    }
                    if (car.isPlayer()) {
                        race.addMessage("Lock-up! You ran wide into Turn " + plan.getCorner().getNumber() + ".", true);
                    }
                }
            }
        }
    }

    /**
     * Gives a ruling for every pair of cars side by side at a corner's turn-in: the stronger claim
     * keeps the corner and the other car gives way. Close calls can end in contact.
     */
    private void ruleCorners() {
        List<Car> cars = race.getCars();
        double lap = race.getTrack().getLapLength();
        for (Car front : cars) {
            if (!racing(front)) {
                continue;
            }
            Corner corner = cornerByApex(front);
            if (corner == null) {
                continue;
            }
            double toApex = race.getTrack().wrap(corner.getApex() - front.getDistance());
            if (toApex > Tuning.TURN_IN_BEFORE_APEX) {
                continue;
            }
            for (Car rear : cars) {
                if (rear == front || !racing(rear)) {
                    continue;
                }
                double gap = race.trackGap(rear, front);
                if (gap >= Tuning.MIN_SEPARATION) {
                    continue;
                }
                int apexLap = (int) Math.floor((front.getDistance() + toApex) / lap);
                long key = ((long) Math.min(rear.getId(), front.getId()) * Tuning.MAX_CARS
                        + Math.max(rear.getId(), front.getId())) * 100000L + apexLap * 100L + corner.getNumber();
                if (!ruled.add(key)) {
                    continue;
                }
                rule(rear, front, corner, gap);
            }
        }
    }

    /**
     * Settles one corner between two cars side by side.
     *
     * @param rear the car whose nose is behind
     * @param front the car whose nose is ahead
     * @param corner the corner
     * @param gap distance from the rear car to the front car, in meters
     */
    private void rule(Car rear, Car front, Corner corner, double gap) {
        double difficulty = race.getTrack().getProfile().getOvertakingDifficulty();
        double claim = BattleResolver.claim(rear, front, corner, gap, difficulty, random.nextGaussian() * Tuning.CLAIM_NOISE);
        Battle battle = battleOf(rear);
        if (battle == null) {
            battle = battleOf(front);
        }
        if (Math.abs(claim) < Tuning.CONTACT_BAND && random.nextDouble() < Tuning.CONTACT_CHANCE) {
            rear.addTimePenalty(Tuning.CONTACT_ATTACKER_LOSS);
            front.addTimePenalty(Tuning.CONTACT_DEFENDER_LOSS);
            if (random.nextDouble() < Tuning.CONTACT_PUNCTURE_CHANCE) {
                (random.nextBoolean() ? rear : front).puncture();
            }
            if (battle != null) {
                battle.markContact();
            } else if (rear.isPlayer() || front.isPlayer()) {
                race.addMessage("Contact with " + (rear.isPlayer() ? front : rear).getDriver().getCode()
                        + " at Turn " + corner.getNumber() + "!", true);
            }
        }
        Car winner = claim > 0 ? rear : front;
        Car loser = winner == rear ? front : rear;
        loser.setYieldTo(winner, race.getTime());
        loser.setLinePlan(null);
        if (winner.getYieldTo() == loser) {
            winner.setYieldTo(null, race.getTime());
        }
    }

    /**
     * Ends a give-way once the car is safely behind the car it lost to (or the other car has gone).
     */
    private void releaseYields() {
        double half = race.getTrack().getLapLength() / 2;
        for (Car car : race.getCars()) {
            Car winner = car.getYieldTo();
            if (winner == null) {
                continue;
            }
            double behind = race.trackGap(car, winner);
            boolean clear = behind >= Tuning.MIN_SEPARATION && behind < half;
            boolean gone = winner.isRetired() || winner.isInPitLane() || winner.isFinished() || car.isInPitLane()
                    || car.isRetired() || car.isFinished();
            boolean wellAhead = behind >= half && race.trackGap(winner, car) > Tuning.MIN_SEPARATION * 2;
            boolean tooLong = race.getTime() - car.getYieldSince() > Tuning.YIELD_MAX_SECONDS;
            if (clear || gone || wellAhead || tooLong) {
                car.setYieldTo(null, race.getTime());
            }
        }
    }

    /**
     * Checks whether a battle is over.
     *
     * @param battle the battle
     * @return the outcome, or null while it goes on
     */
    private BattleOutcome progress(Battle battle) {
        Car a = battle.getAttacker();
        Car d = battle.getDefender();
        if (!racing(a) || !racing(d) || race.getSafetyCar().isActive()) {
            return BattleOutcome.NO_ATTEMPT;
        }
        double half = race.getTrack().getLapLength() / 2;
        double attackerAhead = race.trackGap(d, a);
        LinePlan plan = a.getLinePlan();
        if (attackerAhead >= Tuning.MIN_SEPARATION && attackerAhead < half) {
            if (plan != null && a.getDistance() < plan.getBrakeAt()) {
                battle.markPassedBeforeBraking();
            }
            return BattleOutcome.PASSED;
        }
        double apex = a.getDistance() + race.getTrack().wrap(battle.getCorner().getApex() - a.getDistance());
        boolean pastCorner = plan != null ? plan.isDone(a.getDistance())
                : a.getDistance() > apex - Tuning.MIN_SEPARATION && race.getTrack().wrap(battle.getCorner().getApex() - a.getDistance()) > half;
        boolean timedOut = race.getTime() - battle.getOpenedAt() > Tuning.BATTLE_MAX_SECONDS;
        if (!pastCorner && !timedOut) {
            return null;
        }
        if (battle.hadContact()) {
            return BattleOutcome.CONTACT;
        }
        if (battle.hadLockUp()) {
            return BattleOutcome.LOCKED_UP;
        }
        LineChoice line = battle.getAttackerLine();
        return line == null || line == LineChoice.STAY_BEHIND ? BattleOutcome.NO_ATTEMPT : BattleOutcome.HELD;
    }

    /**
     * Records a finished battle and sends the radio message when it matters to the player.
     *
     * @param battle the battle
     * @param outcome how it ended
     */
    private void finish(Battle battle, BattleOutcome outcome) {
        LineChoice line = battle.getAttackerLine();
        if (line != null && line.leavesLine()) {
            attempts++;
        }
        if (outcome == BattleOutcome.PASSED) {
            passes++;
            if (battle.getAttacker().isPlayer()) {
                lastPlayerPassTime = race.getTime();
            }
        }
        battle.finish(outcome, BattleResolver.describe(battle, outcome));
        boolean aboutPlayer = battle.getAttacker().isPlayer() || battle.getDefender().isPlayer();
        boolean attacked = line != null && line != LineChoice.STAY_BEHIND;
        boolean newsworthy = outcome == BattleOutcome.PASSED || outcome == BattleOutcome.CONTACT
                || outcome == BattleOutcome.LOCKED_UP || (battle.promptsPlayer() && attacked);
        if (aboutPlayer && newsworthy) {
            race.addMessage(battle.getMessage(), true);
        }
    }

    /**
     * Lets a computer driver decide early what it will do at the next corner, so an attack can be
     * shown as a warning before it happens and the battle uses the same decision.
     *
     * @param car the car
     */
    private void updateIntent(Car car) {
        if (car.isPlayer() || !racing(car)) {
            return;
        }
        Car ahead = race.carAheadOnTrack(car);
        if (ahead == null || gapSeconds(car, ahead) > Tuning.INTENT_GAP_SECONDS) {
            return;
        }
        Corner next = nextCorner(car, 0);
        if (next == null) {
            return;
        }
        int key = cornerKey(car, next);
        if (car.getIntentKey() != key) {
            car.setIntent(LineChoicePolicy.attackerLine(car, ahead, gapSeconds(car, ahead), random), key);
        }
    }

    /**
     * Opens a battle for a car and the car ahead if they are close enough before a corner.
     *
     * @param attacker the car behind
     * @param onDemand true when the player asked to attack (a wider gap is allowed and no waiting)
     * @return true if a battle was opened
     */
    private boolean maybeOpen(Car attacker, boolean onDemand) {
        if (race.getSafetyCar().isActive() || !racing(attacker) || attacker.getSpeed() < 1
                || attacker.getYieldTo() != null || involved(attacker)) {
            return false;
        }
        Car defender = race.carAheadOnTrack(attacker);
        if (defender == null || defender.isOffTrack() || defender.isInPitLane() || involved(defender)) {
            return false;
        }
        double gap = gapSeconds(attacker, defender);
        if (gap > (onDemand ? Tuning.ON_DEMAND_ATTACK_GAP : Tuning.BATTLE_GAP_SECONDS)) {
            return false;
        }
        Corner corner = nextCorner(attacker, attacker.getSpeed() * Tuning.BATTLE_MIN_LEAD_SECONDS);
        if (corner == null) {
            return false;
        }
        double toBraking = race.getTrack().wrap(corner.getBrakingPoint() - attacker.getDistance());
        if (!onDemand && toBraking > attacker.getSpeed() * Tuning.BATTLE_OPEN_LEAD_SECONDS) {
            return false;
        }
        int key = cornerKey(attacker, corner);
        if (attacker.getLastBattleKey() == key && !onDemand) {
            return false;
        }
        attacker.setLastBattleKey(key);
        boolean lapped = race.getStandings().indexOf(defender) > race.getStandings().indexOf(attacker);
        if (lapped || defender.isFinished()) {
            giveWay(attacker, defender, corner);
            return true;
        }
        boolean playerInvolved = attacker.isPlayer() || defender.isPlayer();
        boolean chained = race.getTime() - lastPlayerPassTime < Tuning.CHAIN_ATTACK_SECONDS;
        LineChoice aiAttack = null;
        if (!attacker.isPlayer()) {
            aiAttack = attacker.getIntentKey() == key ? attacker.getIntent()
                    : LineChoicePolicy.attackerLine(attacker, defender, gap, random);
        }
        boolean realAttack = attacker.isPlayer() || aiAttack.leavesLine();
        boolean prompt = playerInvolved && realAttack
                && (onDemand || chained || race.getTime() - lastPromptTime >= Tuning.PROMPT_COOLDOWN_SECONDS);
        Battle battle = new Battle(attacker, defender, corner, gap, race.getTime(), prompt);
        if (aiAttack != null) {
            battle.setAttackerChoice(aiAttack, LineChoicePolicy.lateBrake(attacker, aiAttack, random));
            boolean boost = aiAttack.leavesLine()
                    && random.nextDouble() < Tuning.AI_OVERTAKE_USE * (0.5 + attacker.getAggression());
            if (boost && race.tryOvertake(attacker)) {
                battle.markOvertake();
            }
        }
        if (playerInvolved && !prompt) {
            Car player = race.getPlayerCar();
            LineChoice line = LineChoicePolicy.playerDefault(attacker.isPlayer(), false);
            if (attacker.isPlayer()) {
                battle.setAttackerChoice(line, false);
            } else {
                battle.setDefenderChoice(line, false);
            }
            battle.lockPlayer();
        }
        if (prompt) {
            lastPromptTime = race.getTime();
            playerBattle = battle;
            if (preselected != null) {
                battle.choosePlayerLine(preselected);
                preselected = null;
            }
        }
        open.add(battle);
        return true;
    }

    /**
     * Lets a faster car through: a lapped or finished car moves aside and gives way, with no fight.
     *
     * @param attacker the faster car
     * @param defender the car giving way
     * @param corner the coming corner
     */
    private void giveWay(Car attacker, Car defender, Corner corner) {
        Battle battle = new Battle(attacker, defender, corner, 0, race.getTime(), false);
        battle.finish(BattleOutcome.BLUE_FLAG, BattleResolver.describe(battle, BattleOutcome.BLUE_FLAG));
        defender.setLinePlan(new LinePlan(LineChoice.OUTSIDE, corner, false, false, defender.getDistance(), true,
                race.getTrack()));
        defender.setYieldTo(attacker, race.getTime());
        if (attacker.isPlayer() || defender.isPlayer()) {
            race.addMessage(battle.getMessage(), true);
        }
    }

    /**
     * Returns whether a car has a speed edge on the straight right now (DRS, or battery being used),
     * which makes it pull out of the tow early to drag past.
     *
     * @param car the car
     * @return true with a straight-line edge
     */
    public static boolean hasStraightEdge(Car car) {
        return car.isDrsOpen() || car.getDrsZone() >= 0 || car.getErs().isDeploying();
    }

    /**
     * Returns whether a car is racing on the track (not finished, retired or in the pit lane).
     *
     * @param car the car
     * @return true if racing
     */
    private static boolean racing(Car car) {
        return !car.isFinished() && !car.isRetired() && !car.isInPitLane();
    }

    /**
     * Returns the open battle a car is in.
     *
     * @param car the car
     * @return the battle, or null
     */
    private Battle battleOf(Car car) {
        for (Battle b : open) {
            if (b.getAttacker() == car || b.getDefender() == car) {
                return b;
            }
        }
        return null;
    }

    /**
     * Returns whether a car is in an open battle.
     *
     * @param car the car
     * @return true if fighting
     */
    private boolean involved(Car car) {
        return battleOf(car) != null;
    }

    /**
     * Returns the time gap from a car to a car ahead.
     *
     * @param car the car behind
     * @param ahead the car ahead
     * @return seconds
     */
    private double gapSeconds(Car car, Car ahead) {
        return race.trackGap(car, ahead) / Math.max(1.0, car.getSpeed());
    }

    /**
     * Finds the next corner whose braking point is at least some distance ahead of a car.
     *
     * @param car the car
     * @param minAhead smallest distance to the braking point, in meters (time to choose)
     * @return the corner, or null if the track has none
     */
    private Corner nextCorner(Car car, double minAhead) {
        Corner best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Corner corner : race.getCorners()) {
            double d = race.getTrack().wrap(corner.getBrakingPoint() - car.getDistance());
            if (d >= minAhead && d < bestDistance) {
                bestDistance = d;
                best = corner;
            }
        }
        return best;
    }

    /**
     * Finds the corner whose apex is next ahead of a car.
     *
     * @param car the car
     * @return the corner, or null if the track has none
     */
    private Corner cornerByApex(Car car) {
        Corner best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Corner corner : race.getCorners()) {
            double d = race.getTrack().wrap(corner.getApex() - car.getDistance());
            if (d < bestDistance) {
                bestDistance = d;
                best = corner;
            }
        }
        return best;
    }

    /**
     * Returns a key for a corner on a particular lap, as seen from a car.
     *
     * @param car the car
     * @param corner the corner
     * @return lap times 1000 plus corner number
     */
    private int cornerKey(Car car, Corner corner) {
        double atBraking = car.getDistance() + race.getTrack().wrap(corner.getBrakingPoint() - car.getDistance());
        int lap = (int) Math.floor(atBraking / race.getTrack().getLapLength());
        return lap * 1000 + corner.getNumber();
    }

    /**
     * Ends every battle at once (for the safety car): the cars go back to their normal lines.
     */
    public void cancelAll() {
        for (Battle battle : open) {
            battle.getAttacker().setLinePlan(null);
            battle.getDefender().setLinePlan(null);
            battle.finish(BattleOutcome.NO_ATTEMPT, "");
        }
        open.clear();
        if (playerBattle != null && !playerBattle.isResolved()) {
            playerBattle = null;
        }
    }

    /**
     * Applies the player's line choice to the current battle, or remembers it for the next battle.
     *
     * @param line the chosen line
     */
    public void choosePlayerLine(LineChoice line) {
        if (startPlanned && race.isStartPhase()) {
            replanStart(line, race.getPlayerCar().isOvertakeActive());
            return;
        }
        if (playerBattle != null && !playerBattle.isResolved() && !playerBattle.isPlayerLocked()) {
            playerBattle.choosePlayerLine(line);
        } else {
            preselected = line;
        }
    }

    /**
     * Switches late braking on or off for the player's current battle.
     */
    public void togglePlayerLateBrake() {
        if (playerBattle != null && !playerBattle.isResolved()) {
            playerBattle.togglePlayerLateBrake();
        }
    }

    /**
     * Asks for the Overtake boost (the player's Overtake key); handled at the next step.
     */
    public void requestPlayerOvertake() {
        attackRequested = true;
    }

    /**
     * Returns the player's current battle.
     *
     * @return the battle, or null
     */
    public Battle getPlayerBattle() {
        return playerBattle;
    }

    /**
     * Forgets the player's battle once its result has been shown.
     */
    public void clearPlayerBattle() {
        playerBattle = null;
    }

    /**
     * Returns the line the player picked for the next battle.
     *
     * @return the line, or null
     */
    public LineChoice getPreselected() {
        return preselected;
    }

    /**
     * Returns the number of attacks made.
     *
     * @return attacks down the inside or around the outside
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns the number of fights the attacker won.
     *
     * @return passes
     */
    public int getPasses() {
        return passes;
    }
}
