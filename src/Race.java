import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * The race: ten cars on one track. Each call to step() moves the race on by 1/60 of a second: weather,
 * computer decisions, every car's speed and position, pit stops, laps and overtakes.
 */
public final class Race {

    /** Laps in the race. */
    public static final int LAPS = 10;

    /** Length of one simulation step in seconds. */
    public static final double STEP = 1.0 / 60;

    /** Battery spent by one Overtake boost. */
    public static final double BOOST_COST = 0.2;

    /** Cars never get closer than this (nose to nose) unless side by side, in meters. */
    private static final double MIN_GAP = 22;

    /** Cars at least this far apart sideways can be side by side, in meters. */
    private static final double SIDE_BY_SIDE = 9;

    /** Sideways position of the Inside and Outside lanes, in meters from the racing line side. */
    private static final double LANE = 16;

    /** Acceleration from low speed, in m/s squared. */
    private static final double ACCELERATION = 13;

    /** Sideways steering speed, in m/s. */
    private static final double STEERING = 12;

    /** Extra speed from DRS. */
    private static final double DRS_BONUS = 0.04;

    /** Extra speed from the Overtake boost. */
    private static final double BOOST_BONUS = 0.03;

    /** Distance the Overtake boost lasts, in meters. */
    private static final double BOOST_METERS = 500;

    /** Boost battery recharge per second (50% more in Push mode). */
    private static final double BOOST_RECHARGE = 0.012;

    /** A fight can start within this many seconds of the car ahead. */
    private static final double FIGHT_RANGE = 1.0;

    /** The player may press Overtake within this many seconds of the car ahead. */
    private static final double OVERTAKE_RANGE = 1.5;

    /** Where the pit lane starts, in meters before the line. */
    private static final double PIT_ENTRY = 250;

    /** Where the pit lane ends, in meters after the line. */
    private static final double PIT_EXIT = 250;

    /** Where the pit box is, in meters after the line. */
    private static final double PIT_BOX = 20;

    /** Sideways position of the pit lane (outside the track on the right). */
    public static final double PIT_LANE = -46;

    /** Pit lane speed limit in m/s (80 km/h). */
    private static final double PIT_SPEED = 22;

    /** Seconds standing in the pit box. */
    private static final double PIT_STOP_SECONDS = 2.5;

    /** The ten drivers' codes, two per team. */
    private static final String[] DRIVERS = {"VER", "HAD", "NOR", "PIA", "LEC", "HAM", "RUS", "ANT", "ALO", "STR"};

    /** Team colors (Red Bull, McLaren, Ferrari, Mercedes, Aston Martin); driver i drives for team i / 2. */
    private static final int[] TEAM_COLORS = {0x3671C6, 0xFF8000, 0xE8002D, 0x27F4D2, 0x229971};

    /** The track. */
    public final Track track;

    /** All cars, in grid order. */
    public final ArrayList<Car> cars = new ArrayList<Car>();

    /** The player's car. */
    public final Car player;

    /** Lap times: lapTimes[car][lap] in seconds. */
    public final double[][] lapTimes = new double[DRIVERS.length][LAPS];

    /** Open fights (overtaking attempts). */
    private final ArrayList<Fight> fights = new ArrayList<Fight>();

    /** Weather changes still to come: {race time, new rain intensity}, earliest first. */
    private final PriorityQueue<double[]> weather = new PriorityQueue<double[]>(Comparator.comparingDouble(e -> e[0]));

    /** Radio messages, newest last. */
    private final ArrayDeque<String> radio = new ArrayDeque<String>();

    /** When each kind of engineer message was last sent, so the radio does not repeat itself. */
    private final HashMap<String, Double> lastSaid = new HashMap<String, Double>();

    /** Random numbers (a seed gives the same race again). */
    private final Random random;

    /** The player's current fight, or null. */
    private Fight playerFight;

    /** Race time in seconds. */
    private double time;

    /** How hard it is raining now, 0 to 1. */
    private double rain;

    /** How wet the track is, 0 to 1 (follows the rain slowly). */
    private double wetness;

    /** Sets up a race: the grid in random order, a random player car and random weather. */
    public Race(Track track, long seed) {
        this.track = track;
        this.random = new Random(seed);
        ArrayList<Integer> grid = new ArrayList<Integer>();
        for (int i = 0; i < DRIVERS.length; i++) {
            grid.add(i);
        }
        Collections.shuffle(grid, random);
        int playerSlot = random.nextInt(grid.size());
        for (int i = 0; i < grid.size(); i++) {
            int driver = grid.get(i);
            Car car = new Car(DRIVERS[driver], TEAM_COLORS[driver / 2], i == playerSlot, 0.995 + random.nextDouble() * 0.01);
            car.distance = -8 - i * 24;
            car.lateral = (i % 2 == 0 ? 8 : -8);
            car.lateralTarget = car.lateral;
            car.launchAt = 0.1 + random.nextDouble() * 0.3;
            cars.add(car);
        }
        player = cars.get(playerSlot);
        if (random.nextDouble() < 0.5) {
            double start = (2 + random.nextDouble() * 4) * 75;
            weather.add(new double[] {start, 0.5 + random.nextDouble() * 0.5});
            weather.add(new double[] {start + (1.5 + random.nextDouble() * 2) * 75, 0});
        }
    }

    /** Moves the race on by one step. */
    public void step() {
        time += STEP;
        updateWeather();
        openFights();
        for (Car car : cars) {
            if (!car.player && Math.floor(car.distance / 2000) != Math.floor((car.distance - car.speed * STEP) / 2000)) {
                Ai.chooseMode(car, random);
                if (car.boxFor == null) {
                    car.boxFor = Ai.pitChoice(car, wetness, LAPS - car.laps);
                }
            }
            moveCar(car);
        }
        updateFights();
        talkToPlayer();
    }

    /** Applies due weather changes and lets the track wet or dry toward the rain level. */
    private void updateWeather() {
        while (!weather.isEmpty() && weather.peek()[0] <= time) {
            rain = weather.poll()[1];
            say(rain > 0 ? "Rain is starting!" : "Rain has stopped, the track will dry.");
        }
        double seconds = rain > wetness ? 40 : 150;
        wetness += (rain - wetness) * STEP / seconds;
    }

    /**
     * Moves one car: works out its target speed, speeds up or brakes toward it, keeps its distance from the
     * car ahead, steers sideways, handles the pit lane and counts laps.
     */
    private void moveCar(Car car) {
        double before = car.distance;
        double target = track.speedAt(car.distance);
        if (!track.isStraight(car.distance)) {
            target *= Math.sqrt(car.grip(wetness));
        }
        target *= car.pace * car.mode.pace;
        if (car.boosting() && track.isStraight(car.distance)) {
            target *= 1 + BOOST_BONUS;
        }
        if (hasDrs(car)) {
            target *= 1 + DRS_BONUS;
        }
        if (car.passing != null) {
            target *= 1.06;
        } else if (isBeingPassed(car)) {
            target *= 0.97;
        }
        if (car.finished) {
            target *= 0.8;
        }
        if (car.inPit) {
            target = Math.min(target, PIT_SPEED);
        }
        if (time < car.launchAt || car.pitTimer > 0) {
            target = 0;
        }
        if (car.speed < target) {
            double ratio = car.speed / Track.TOP_SPEED;
            car.speed = Math.min(target, car.speed + ACCELERATION * (1 - ratio * ratio) * STEP);
        } else {
            car.speed = Math.max(target, car.speed - Track.BRAKING * STEP);
        }
        double move = car.speed * STEP;
        Car ahead = carAhead(car, true);
        if (ahead != null && ahead != car.passing) {
            double room = gap(car, ahead) - MIN_GAP;
            if (move > room) {
                move = Math.max(0, room);
                car.speed = Math.min(car.speed, ahead.speed);
            }
        }
        car.distance += move;
        steer(car);
        car.wear = Math.min(1, car.wear + car.tire.getWearPerLap() * car.mode.wear * move / track.length());
        if (car.boosting()) {
            car.boost = Math.max(0, car.boost - 0.02 * STEP);
        } else {
            car.boost = Math.min(1, car.boost + BOOST_RECHARGE * (car.mode == Car.Mode.PUSH ? 1.5 : 1) * STEP);
        }
        pitLane(car, before);
        if (Math.floor(car.distance / track.length()) > car.laps && !car.finished) {
            lapTimes[cars.indexOf(car)][car.laps] = time - car.lapStart;
            car.laps++;
            car.lapStart = time;
            if (car.laps == LAPS) {
                car.finished = true;
                car.finishTime = time;
            }
        }
    }

    /**
     * Steers a car toward its sideways target: the racing line normally, or its lane in a fight, or the pit
     * lane. Steering is limited so cars move across smoothly.
     */
    private void steer(Car car) {
        boolean inFight = car.passing != null || fightOf(car) != null;
        if (!inFight && !car.inPit) {
            car.lateralTarget = track.lineAt(car.distance);
        }
        double maxStep = Math.min(STEERING, 0.3 * car.speed) * STEP;
        double toGo = car.lateralTarget - car.lateral;
        car.lateral += Math.max(-maxStep, Math.min(maxStep, toGo));
    }

    /** Runs the pit stop: entering the lane, stopping in the box to change tires, and leaving. */
    private void pitLane(Car car, double before) {
        double lap = track.length();
        if (car.boxFor != null && !car.inPit && crossed(before, car.distance, lap - PIT_ENTRY) && !car.finished) {
            car.inPit = true;
            car.lateralTarget = PIT_LANE;
        }
        if (car.inPit && car.boxFor != null && crossed(before, car.distance, PIT_BOX)) {
            car.pitTimer = PIT_STOP_SECONDS;
            car.tire = car.boxFor;
            car.wear = 0;
            car.boxFor = null;
            car.stops++;
            if (car.player) {
                say("Tires changed: " + car.tire.getName() + ". Go go go!");
            }
        }
        car.pitTimer = Math.max(0, car.pitTimer - STEP);
        if (car.inPit && car.boxFor == null && crossed(before, car.distance, PIT_EXIT)) {
            car.inPit = false;
        }
    }

    /** Returns whether a car passed a point on the lap during the last step. */
    private boolean crossed(double before, double after, double point) {
        double lap = track.length();
        return Math.floor((before - point) / lap) < Math.floor((after - point) / lap);
    }

    /** Opens a fight for every car that is within range of the car ahead before a corner. */
    private void openFights() {
        for (Car car : cars) {
            Car ahead = carAhead(car, false);
            if (ahead == null || fightOf(car) != null || fightOf(ahead) != null || car.passing != null
                    || car.inPit || ahead.inPit || car.finished || ahead.finished) {
                continue;
            }
            double toBrake = track.toNextBrakePoint(car.distance);
            if (gapSeconds(car, ahead) < FIGHT_RANGE && toBrake > 150 && toBrake < 600) {
                startFight(car, ahead, toBrake);
            }
        }
    }

    /** Opens one fight and lets the computer side(s) choose. */
    private Fight startFight(Car attacker, Car defender, double toBrake) {
        Fight fight = new Fight(attacker, defender, attacker.distance + toBrake);
        if (!attacker.player) {
            Ai.chooseAttack(fight, random);
            if (fight.boost) {
                useBoost(attacker);
            }
        }
        fights.add(fight);
        if (attacker.player || defender.player) {
            playerFight = fight;
        }
        return fight;
    }

    /**
     * Moves fights on: cars move to their chosen lanes, and at the braking point the pass is decided. A
     * passing car stops passing once it is clear ahead.
     */
    private void updateFights() {
        for (int i = fights.size() - 1; i >= 0; i--) {
            Fight fight = fights.get(i);
            fight.attacker.lateralTarget = laneFor(fight, fight.attackLine, fight.attacker);
            fight.defender.lateralTarget = laneFor(fight, fight.defendLine, fight.defender);
            if (fight.attacker.distance >= fight.decideAt || fight.attacker.inPit || fight.defender.inPit) {
                decide(fight);
                fights.remove(i);
            }
        }
        for (Car car : cars) {
            if (car.passing != null && (gap(car.passing, car) > MIN_GAP || car.passing.inPit)) {
                car.passing = null;
            }
        }
    }

    /** Decides a fight at the braking point: no attack, a pass, or a failed attempt. */
    private void decide(Fight fight) {
        Car a = fight.attacker;
        Car d = fight.defender;
        if (!d.player) {
            Ai.chooseDefence(fight, random);
        }
        if (fight.attackLine == null || a.inPit || d.inPit) {
            return;
        }
        String result;
        if (random.nextDouble() < fight.chance(gapSeconds(a, d), hasDrs(a), wetness)) {
            a.passing = d;
            a.lateralTarget = d.lateral > a.lateral ? d.lateral - LANE : d.lateral + LANE;
            result = a.code + " passes " + d.code + " on the " + fight.attackLine.name().toLowerCase() + "!";
        } else {
            a.speed *= 0.95;
            result = d.code + " holds off " + a.code;
        }
        if (a.player || d.player) {
            say(result);
        }
    }

    /** Returns the sideways position of a line in a fight (null or Optimal = the racing line). */
    public double laneFor(Fight fight, Fight.Line line, Car car) {
        int side = cornerSide(fight.decideAt);
        if (line == Fight.Line.INSIDE) {
            return side * LANE;
        }
        if (line == Fight.Line.OUTSIDE) {
            return -side * LANE;
        }
        return track.lineAt(car.distance);
    }

    /** Returns which way the corner after a braking point turns. */
    private int cornerSide(double brakeAt) {
        double turn = Track.angle(track.heading(brakeAt + 150) - track.heading(brakeAt));
        return turn >= 0 ? 1 : -1;
    }

    /** Switches on a car's Overtake boost if it has the battery for it. */
    private boolean useBoost(Car car) {
        if (car.boosting()) {
            return true;
        }
        if (car.boost < BOOST_COST) {
            return false;
        }
        car.boost -= BOOST_COST;
        car.boostUntil = car.distance + BOOST_METERS;
        return true;
    }

    /**
     * The player pressed Overtake: switch on the boost and attack the car ahead (opening a fight if there is
     * none yet). Then the player picks a line.
     */
    public void playerOvertake() {
        Car ahead = carAhead(player, false);
        if (playerFight == null && ahead != null && gapSeconds(player, ahead) < OVERTAKE_RANGE && !ahead.inPit) {
            startFight(player, ahead, Math.max(150, track.toNextBrakePoint(player.distance)));
        }
        if (playerFight != null && playerFight.attacker != player) {
            say("You are defending: cover a line with A/S/D.");
            return;
        }
        if (playerFight == null) {
            say("No car close enough to overtake.");
            return;
        }
        if (!useBoost(player)) {
            say("Not enough boost for an Overtake.");
            return;
        }
        playerFight.boost = true;
        if (playerFight.attackLine == null) {
            playerFight.attackLine = Fight.Line.INSIDE;
        }
    }

    /** The player picked a line: to attack on (if attacking) or to cover (if defending). */
    public void playerLine(Fight.Line line) {
        if (playerFight == null) {
            return;
        }
        if (playerFight.attacker == player) {
            playerFight.attackLine = line;
        } else {
            playerFight.defendLine = line;
        }
    }

    /** Sends engineer messages about tires and rain, each kind at most once every 40 seconds. */
    private void talkToPlayer() {
        if (player.wear > 0.7 && player.boxFor == null) {
            remind("tires", "Tires are going off. Box with keys 1-5?");
        }
        if (!weather.isEmpty() && weather.peek()[1] > 0 && weather.peek()[0] - time < 75) {
            remind("rainSoon", "Rain expected in about a lap.");
        }
        if (wetness > 0.35 && !player.tire.isRain()) {
            remind("wet", "Track is wet: box for Intermediates (key 4).");
        }
        if (wetness < 0.15 && player.tire.isRain() && rain == 0) {
            remind("dry", "Track is drying: box for slicks (keys 1-3).");
        }
    }

    /** Adds a radio message, keeping only the last four. */
    public void say(String text) {
        radio.addLast(text);
        if (radio.size() > 4) {
            radio.removeFirst();
        }
    }

    /** Adds an engineer reminder unless the same kind was said in the last 40 seconds. */
    private void remind(String kind, String text) {
        Double last = lastSaid.get(kind);
        if (last == null || time - last > 40) {
            lastSaid.put(kind, time);
            say(text);
        }
    }

    /** Finds the nearest car ahead on the road. */
    private Car carAhead(Car car, boolean sameLane) {
        Car best = null;
        for (Car other : cars) {
            boolean inWay = Math.abs(other.lateral - car.lateral) < SIDE_BY_SIDE;
            if (other != car && gap(car, other) > 0 && (!sameLane || inWay) && other.inPit == car.inPit
                    && (best == null || gap(car, other) < gap(car, best))) {
                best = other;
            }
        }
        return best;
    }

    /** Returns how far ahead another car is along the lap (0 up to one lap). */
    private double gap(Car car, Car other) {
        return track.wrap(other.distance - car.distance);
    }

    /** Returns the gap to a car ahead in seconds. */
    public double gapSeconds(Car car, Car other) {
        return gap(car, other) / Math.max(10, car.speed);
    }

    /** Returns whether a car has DRS open: from lap 2, on a straight, within a second of the car ahead, dry track. */
    public boolean hasDrs(Car car) {
        Car ahead = carAhead(car, false);
        return car.laps >= 1 && wetness < 0.3 && track.isStraight(car.distance) && ahead != null
                && gapSeconds(car, ahead) < 1.0 && !car.inPit;
    }

    /** Returns whether another car is passing this one. */
    private boolean isBeingPassed(Car car) {
        for (Car other : cars) {
            if (other.passing == car) {
                return true;
            }
        }
        return false;
    }

    /** Returns the open fight a car is in. */
    private Fight fightOf(Car car) {
        for (Fight f : fights) {
            if (f.attacker == car || f.defender == car) {
                return f;
            }
        }
        return null;
    }

    /** Returns the cars in race order: finished cars by finishing time, then by distance driven. */
    public List<Car> standings() {
        ArrayList<Car> order = new ArrayList<Car>(cars);
        order.sort((a, b) -> {
            if (a.finished && b.finished) {
                return Double.compare(a.finishTime, b.finishTime);
            }
            if (a.finished != b.finished) {
                return a.finished ? -1 : 1;
            }
            return Double.compare(b.distance, a.distance);
        });
        return order;
    }

    /** Returns a car's best lap time from the lap time table, or 0 before its first lap. */
    public double bestLap(Car car) {
        double best = 0;
        for (double lap : lapTimes[cars.indexOf(car)]) {
            if (lap > 0 && (best == 0 || lap < best)) {
                best = lap;
            }
        }
        return best;
    }

    /** Returns the player's open fight, if the player is in one. */
    public Fight getPlayerFight() {
        if (playerFight != null && !fights.contains(playerFight)) {
            playerFight = null;
        }
        return playerFight;
    }

    /** Returns the radio messages, newest last. */
    public ArrayDeque<String> getRadio() {
        return radio;
    }

    /** Returns the race time. */
    public double getTime() {
        return time;
    }

    /** Returns how wet the track is. */
    public double getWetness() {
        return wetness;
    }

    /** Returns how hard it is raining. */
    public double getRain() {
        return rain;
    }

    /** Returns whether every car has finished. */
    public boolean isOver() {
        for (Car car : cars) {
            if (!car.finished) {
                return false;
            }
        }
        return true;
    }
}
