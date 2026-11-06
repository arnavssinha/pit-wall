/**
 * Every tunable number in the simulation, in one place.
 * Each constant says where its value came from: a physical fact, a rule of the
 * sport, or a balance choice made so the game plays well. Keeping them here means
 * the rest of the code never hides a magic number.
 */
public final class Tuning {

    /** Number of laps in every race. The brief fixes the sprint at 10 laps. */
    public static final int RACE_LAPS = 10;

    /** Fewest cars allowed in a race (brief, Section 3.3). */
    public static final int MIN_CARS = 10;

    /** Most cars allowed: the 2026 grid has 22 cars. */
    public static final int MAX_CARS = 22;

    /** Default field size: a full 2026 grid. */
    public static final int DEFAULT_CARS = 22;

    /** Length of one simulation step in seconds (brief, Section 4: fixed 1/60 s). */
    public static final double SIM_STEP_SECONDS = 1.0 / 60.0;

    /** Playback speeds the player can choose. Physics is unchanged; more steps run per frame. */
    public static final int[] TIME_SPEEDS = {1, 2, 4, 8};

    /** Index into TIME_SPEEDS used at race start (4x, for a faster, more engaging pace). */
    public static final int DEFAULT_TIME_SPEED_INDEX = 2;

    /** Gravity in meters per second squared (physics). */
    public static final double GRAVITY = 9.81;

    /**
     * Tire-road friction coefficient before downforce. Racing slicks reach roughly 1.5 to 1.8;
     * 1.6 is a balance choice inside that range. The per-track calibration scales it.
     */
    public static final double BASE_GRIP_MU = 1.6;

    /**
     * Downforce, expressed as extra grip per (speed squared), in 1 per meter. Balance choice:
     * with BASE_GRIP_MU this gives about 5 g of cornering at 300 km/h, close to a modern F1 car.
     */
    public static final double DOWNFORCE_PER_SPEED_SQUARED = 0.0030;

    /**
     * Straight-line speed cap in meters per second (about 345 km/h). Balance choice near
     * real low-downforce top speeds.
     */
    public static final double TOP_SPEED = 96.0;

    /** Peak forward acceleration at low speed, m/s squared (about 1.2 g). Balance choice. */
    public static final double MAX_ACCELERATION = 12.0;

    /**
     * Safety margin on braking when planning the speed profile. Cars plan for 95 percent of
     * their real braking so they never arrive at a corner too fast. Balance choice.
     */
    public static final double BRAKE_PLANNING_MARGIN = 0.95;

    /** Track samples are placed this many meters apart along the centerline (brief, Section 4). */
    public static final double SAMPLE_SPACING = 5.0;

    /** Samples averaged when smoothing curvature, to remove jitter from the source points. */
    public static final int CURVATURE_SMOOTHING_WINDOW = 7;

    /** Curvature (1 per meter) above which a sample counts as part of a corner, for kerbs. */
    public static final double CORNER_CURVATURE = 1.0 / 220.0;

    /** Tightest corner radius the drawn track may have, in meters, so the wide drawn track never folds. Style choice. */
    public static final double MIN_CORNER_RADIUS = 40.0;

    /**
     * Tightest corner radius used for corner speeds, in meters. The real layout keeps its tight corners
     * for speed, but a traced picture can draw a hairpin tighter than the real one, so it is capped here.
     */
    public static final double MIN_SPEED_RADIUS = 30.0;

    /** Most smoothing passes used to round off corners tighter than MIN_CORNER_RADIUS. */
    public static final int CORNER_EASING_MAX_PASSES = 4000;

    /** Neighbouring samples on each side eased together with a too-tight one. */
    public static final int CORNER_EASING_REACH = 2;

    /**
     * Track width in meters of the game world. The game world is stylized: the lap keeps its real
     * length, but the track, the cars and every sideways distance are about four times their real
     * size (a real track is 12 to 15 m wide and a car is 1.9 m wide), so the cars are easy to see
     * and the picture is drawn at one scale in every direction. Style choice.
     */
    public static final double TRACK_WIDTH = 56.0;

    /** Car length in game-world meters (stylized, see TRACK_WIDTH). Used for drawing. */
    public static final double CAR_LENGTH = 20.0;

    /** Car width in game-world meters (stylized, see TRACK_WIDTH). Used for drawing and side-by-side room. */
    public static final double CAR_WIDTH = 7.2;

    /**
     * Smallest allowed nose-to-nose distance between two cars, in meters. Balance choice made for
     * the picture: the screen draws cars larger than life on a shrunk layout (see TrackDrawing),
     * and 22 m keeps those drawings from ever overlapping. A test checks this stays true.
     */
    public static final double MIN_SEPARATION = 22.0;

    /**
     * A following car will not close to less than this time gap until overtaking exists (M4).
     * Balance choice.
     */
    public static final double FOLLOW_TIME_GAP = 0.10;

    /**
     * Distance between grid slots in meters. Balance choice: real staggered grids put a car every
     * 8 m, but slots must be at least MIN_SEPARATION apart here.
     */
    public static final double GRID_SLOT_SPACING = 24.0;

    /** Distance from the start line back to pole position, in meters. Balance choice. */
    public static final double GRID_FIRST_SLOT_OFFSET = 14.0;

    /** Sideways offset of grid slots from the centerline, alternating left and right, in meters. */
    public static final double GRID_LATERAL_OFFSET = 8.5;

    /** Rate at which a car moves sideways (off the grid, into and out of a pass), in meters per second. */
    public static final double LATERAL_SPEED = 9.0;

    /**
     * Random per-car pace spread for one race, as a fraction (plus or minus). Balance choice:
     * stands in for car performance until team pace is computed from data in M8. It is drawn
     * fresh each race and is not a rating of any real driver.
     */
    public static final double PACE_NOISE = 0.004;

    /** Speed fraction cars use on the cool-down lap after taking the chequered flag. */
    public static final double COOL_DOWN_PACE = 0.85;

    /** Spacing of timing loops in meters. Gaps between cars are measured at these loops. */
    public static final double TIMING_LOOP_SPACING = 50.0;

    /** Lowest grip scale the lap-time calibration may choose. */
    public static final double CALIBRATION_MIN_GRIP = 0.4;

    /** Highest grip scale the lap-time calibration may choose. */
    public static final double CALIBRATION_MAX_GRIP = 2.5;

    /** Allowed calibration error as a fraction of the target lap (brief: plus or minus 3 percent). */
    public static final double CALIBRATION_TOLERANCE = 0.03;

    /** Random spread of qualifying laps around each car's pace, in seconds (standard deviation). Balance choice. */
    public static final double QUALIFYING_NOISE_SECONDS = 0.15;

    /** A car within this time gap behind another on a straight gets a slipstream (brief: about 1 s). */
    public static final double SLIPSTREAM_GAP_SECONDS = 1.0;

    /** Largest slipstream speed bonus, right behind the car ahead. Balance choice. */
    public static final double SLIPSTREAM_MAX_BONUS = 0.05;

    /** A battle opens when the car ahead is within this time gap (brief: about 1.0 s). */
    public static final double BATTLE_GAP_SECONDS = 1.0;

    /** A battle opens this many seconds (at current speed) before the corner's braking point. */
    public static final double BATTLE_OPEN_LEAD_SECONDS = 6.0;

    /** Simulation speed while the player decides (slow enough to read, quick enough to keep the flow). */
    public static final double BATTLE_SLOWDOWN = 0.35;

    /** Real seconds the player has to choose a stance (brief: about 4). */
    public static final double BATTLE_DECISION_SECONDS = 4.0;

    /** Real seconds for time to ease back to normal speed after a battle. */
    public static final double BATTLE_EASE_BACK_SECONDS = 1.2;

    /** Real seconds the battle result stays on the prompt. */
    public static final double BATTLE_RESULT_SECONDS = 2.5;

    /** Race seconds after a player prompt before another prompt may appear. */
    public static final double PROMPT_COOLDOWN_SECONDS = 20.0;

    /** A computer driver decides its stance for the next corner once within this gap of the car ahead. */
    public static final double INTENT_GAP_SECONDS = 2.0;

    /** Corners need at least this braking speed drop, in m/s, to host battles. */
    public static final double CORNER_MIN_SPEED_DROP = 8.0;

    /** A braking speed drop this large, in m/s, makes a corner as easy to pass at as it gets. */
    public static final double CORNER_EASY_SPEED_DROP = 60.0;

    /** Constant term of the pass score. Balance choice. */
    public static final double PASS_BASE_SCORE = -0.5;

    /** Pass score per unit of speed-scale advantage (0.01 = 1 percent faster gives 0.4). Balance choice. */
    public static final double PASS_PACE_WEIGHT = 40.0;

    /** Pass score per unit of grip advantage. Balance choice. */
    public static final double PASS_TIRE_WEIGHT = 15.0;

    /** Pass score for being right behind rather than at the edge of the battle gap. Balance choice. */
    public static final double PASS_CLOSENESS_WEIGHT = 1.0;

    /** Pass score bonus for choosing Attack. Balance choice. */
    public static final double PASS_ATTACK_BONUS = 1.0;

    /** Pass score bonus when the attacker spends ERS. Balance choice. */
    public static final double PASS_ERS_BONUS = 0.7;

    /** Pass score lost when the defender spends ERS. Balance choice. */
    public static final double PASS_ERS_DEFENCE = 0.5;

    /** Pass score lost when the defender chooses Defend. Balance choice. */
    public static final double PASS_DEFEND_BONUS = 1.6;

    /** Pass score lost per unit of track overtaking difficulty. Balance choice. */
    public static final double PASS_TRACK_WEIGHT = 2.5;

    /** Pass score lost per unit of corner difficulty. Balance choice. */
    public static final double PASS_CORNER_WEIGHT = 1.5;

    /** Steepness k of the logistic curve that turns the pass score into a probability. Balance choice. */
    public static final double PASS_LOGISTIC_K = 1.4;

    /** Base chance an Attack goes wrong. Balance choice. */
    public static final double RISK_BASE = 0.04;

    /** Extra risk per unit of tire wear. */
    public static final double RISK_PER_WEAR = 0.06;

    /** Brake temperature above which hot brakes add risk, in Celsius. */
    public static final double RISK_BRAKE_TEMP = 900.0;

    /** Degrees above RISK_BRAKE_TEMP that add one whole unit of risk. */
    public static final double RISK_BRAKE_SPAN = 1500.0;

    /** Extra risk at a fully wet track. */
    public static final double RISK_PER_WETNESS = 0.25;

    /** Extra risk per unit of aggression above the middle (0.5). */
    public static final double RISK_PER_AGGRESSION = 0.1;

    /** Highest attack risk. */
    public static final double RISK_MAX = 0.6;

    /** Share of incidents that are lock-ups. */
    public static final double INCIDENT_LOCKUP_SHARE = 0.5;

    /** Share of incidents that are running wide (the rest are contact). */
    public static final double INCIDENT_RUN_WIDE_SHARE = 0.35;

    /** Time lost by a lock-up, in seconds. */
    public static final double LOCKUP_TIME_LOSS = 0.4;

    /** Time lost by running wide, in seconds. */
    public static final double RUN_WIDE_TIME_LOSS = 1.2;

    /** Time lost by the attacker in contact, in seconds. */
    public static final double CONTACT_ATTACKER_LOSS = 1.5;

    /** Time lost by the defender in contact, in seconds. */
    public static final double CONTACT_DEFENDER_LOSS = 0.8;

    /** Time a defender loses by taking the defensive line, attacked or not, in seconds. */
    public static final double DEFEND_TIME_COST = 0.15;

    /** Time an attacker loses when an attempted move fails cleanly, in seconds. */
    public static final double FAILED_ATTACK_TIME_COST = 0.1;

    /** Brake heat added by a late-braking attack, in Celsius. */
    public static final double ATTACK_BRAKE_HEAT = 120.0;

    /** Tire wear added by attacking. */
    public static final double ATTACK_TIRE_WEAR = 0.01;

    /** Tire wear added by defending. */
    public static final double DEFEND_TIRE_WEAR = 0.004;

    /** Battery charge spent when ERS is used in a battle. */
    public static final double ERS_BATTLE_COST = 0.08;

    /** Computer drivers use ERS in a battle while above this charge. */
    public static final double AI_BATTLE_ERS_ABOVE = 0.3;

    /** Speed fraction a car drives at while serving a time loss. */
    public static final double PENALTY_SPEED_FRACTION = 0.7;

    /** Sideways offset of the attacker while passing, in meters. */
    public static final double PASS_LATERAL = 12.0;

    /** Sideways offset of the defender while being passed, in meters (to the other side). */
    public static final double DEFENDER_LATERAL = 5.0;

    /** Sideways distance at which two cars no longer touch (car width plus a little), in meters. */
    public static final double LATERAL_CLEARANCE = 8.6;

    /** Extra speed fraction for the attacker during a pass. */
    public static final double PASS_SPEED_BONUS = 0.04;

    /** The attacker closes on the defender at least this fast during a pass, in m/s. */
    public static final double PASS_MIN_CLOSING = 8.0;

    /** Speed fraction the defender gives up while being passed. */
    public static final double PASS_DEFENDER_SLOW = 0.04;

    /** Lowest aggression drawn for a computer driver (narrow band, brief Section 3.11). */
    public static final double AI_AGGRESSION_MIN = 0.4;

    /** Highest aggression drawn for a computer driver. */
    public static final double AI_AGGRESSION_MAX = 0.6;

    /** Under this gap, default policies attack. */
    public static final double DEFAULT_ATTACK_GAP = 0.6;

    /** Under this gap, default policies defend. */
    public static final double DEFAULT_DEFEND_GAP = 0.5;

    /** Attack chance for a computer driver is its aggression times this, plus the bonuses below. */
    public static final double AI_ATTACK_SCALE = 0.5;

    /** Extra attack chance for a computer driver right behind. */
    public static final double AI_CLOSE_ATTACK_BONUS = 0.25;

    /** Extra attack chance per unit of tire-wear advantage. */
    public static final double AI_TIRE_EDGE_WEIGHT = 1.5;

    /** Attack chance removed when the attacker's tires are past the cliff. */
    public static final double AI_WORN_TIRE_CAUTION = 0.3;

    /** Chance a computer defender correctly reads a coming attack. */
    public static final double AI_READ_ACCURACY = 0.7;

    /** Chance a computer defender defends when no attack is coming. */
    public static final double AI_FALSE_DEFEND = 0.15;

    /** Pit lane speed limit in m/s: 80 km/h, the limit at Monza and most circuits (sporting rule). */
    public static final double PIT_SPEED_LIMIT = 80.0 / 3.6;

    /** Sideways distance from the track centerline to the pit lane, in meters. Drawing choice. */
    public static final double PIT_LANE_OFFSET = 42.0;

    /** Sideways speed when moving into or out of the pit lane, in m/s. */
    public static final double PIT_LATERAL_SPEED = 20.0;

    /** Extra seconds of room a car leaving the pits keeps when judging whether it can merge. */
    public static final double MERGE_MARGIN_SECONDS = 0.5;

    /** Average time the crew needs to change four tires, in seconds (real stops take about 2 to 3 s). */
    public static final double CREW_TIME_MEAN = 2.4;

    /** Random spread of crew time, in seconds (standard deviation). Balance choice. */
    public static final double CREW_TIME_SPREAD = 0.3;

    /** Fastest possible crew time, in seconds. */
    public static final double CREW_TIME_MIN = 1.9;

    /** Dry-race rule: a car must use two different dry compounds or be disqualified (brief, default on). */
    public static final boolean TWO_COMPOUND_RULE = true;

    /** Share of computer drivers on the one-stop-early strategy. Balance choice. */
    public static final double AI_SHARE_ONE_STOP_EARLY = 0.35;

    /** Share of computer drivers on the one-stop-late strategy (the rest two-stop). Balance choice. */
    public static final double AI_SHARE_ONE_STOP_LATE = 0.4;

    /** Computer drivers make an unplanned stop at this tire wear. Balance choice. */
    public static final double AI_EMERGENCY_WEAR = 0.97;

    /** Seconds for rain to build to its peak, and to fade away again. */
    public static final double RAIN_RAMP_SECONDS = 40.0;

    /** Coldest air temperature a forecast may give, in Celsius. */
    public static final double AIR_TEMP_MIN = 5.0;

    /** Hottest air temperature a forecast may give, in Celsius. */
    public static final double AIR_TEMP_MAX = 42.0;

    /** Coldest track temperature a forecast may give, in Celsius. */
    public static final double TRACK_TEMP_MIN = 10.0;

    /** Hottest track temperature a forecast may give, in Celsius. */
    public static final double TRACK_TEMP_MAX = 60.0;

    /** Earliest lap rain may start. Balance choice. */
    public static final int RAIN_FIRST_LAP = 2;

    /** Shortest spell of rain, in laps. Balance choice. */
    public static final double RAIN_MIN_LAPS = 1.5;

    /** Longest spell of rain, in laps. Balance choice. */
    public static final double RAIN_MAX_LAPS = 4.0;

    /** Lightest peak rain intensity. Balance choice. */
    public static final double RAIN_MIN_PEAK = 0.35;

    /** Lowest chance the forecast shows when rain really is coming. */
    public static final double FORECAST_RAIN_LOW = 0.55;

    /** Highest chance the forecast shows when rain really is coming. */
    public static final double FORECAST_RAIN_HIGH = 0.9;

    /** Highest chance the forecast shows when no rain is coming (a false alarm). */
    public static final double FORECAST_FALSE_ALARM = 0.4;

    /** Above this shown chance, the forecast also names a lap. */
    public static final double FORECAST_SHOW_LAP_ABOVE = 0.25;

    /** Forecast lap error (standard deviation, in laps) on a fully changeable track. */
    public static final double FORECAST_LAP_ERROR = 3.0;

    /** Size of a wetness grid cell, in meters. */
    public static final double WETNESS_CELL_METERS = 150.0;

    /** How much cells' drying rates differ from average, as a fraction (shade, drainage). Balance choice. */
    public static final double DRYING_VARIATION = 0.4;

    /** Seconds of full-intensity rain to soak a dry track. Balance choice. */
    public static final double WETTING_SECONDS = 90.0;

    /** Seconds for a soaked cell to dry at the neutral track temperature. Balance choice. */
    public static final double DRYING_SECONDS = 300.0;

    /** Rain lighter than this lets the track dry. */
    public static final double DRIZZLE = 0.05;

    /** Every this many samples is checked when averaging wetness over the lap. */
    public static final int WETNESS_AVERAGE_STRIDE = 10;

    /** Chance of a mechanical retirement per car per race. Balance choice. */
    public static final double RETIREMENT_CHANCE = 0.01;

    /** Earliest point for a scheduled incident, as a fraction of the race. */
    public static final double INCIDENT_FIRST_FRACTION = 0.15;

    /** Latest point for a scheduled incident, as a fraction of the race. */
    public static final double INCIDENT_LAST_FRACTION = 0.8;

    /** Seconds after a car stops before the safety car is out. */
    public static final double SC_DEPLOY_DELAY = 3.0;

    /** Seconds the safety car stays out. Balance choice (about two slow laps). */
    public static final double SC_DURATION_SECONDS = 150.0;

    /** Speed fraction under the safety car. Balance choice. */
    public static final double SC_PACE = 0.6;

    /** Speed fraction for a car catching up to the queue under the safety car. */
    public static final double SC_CATCH_UP_PACE = 0.8;

    /** Under the safety car, a car further than this behind the car ahead may catch up, in meters. */
    public static final double SC_BUNCH_GAP = 60.0;

    /** How far off the track a retired car parks, in meters from the centerline. */
    public static final double OFF_TRACK_OFFSET = 36.0;

    /** Computer drivers on slicks fit Wets (not Intermediates) above this wetness. */
    public static final double AI_WET_WETNESS = 0.7;

    /** Computer drivers on rain tires go back to slicks below this wetness. */
    public static final double AI_DRY_WETNESS = 0.2;

    /** Seconds between the engineer's checks. */
    public static final double RADIO_CHECK_SECONDS = 0.5;

    /** Seconds before the engineer repeats a topic. */
    public static final double RADIO_TOPIC_COOLDOWN = 60.0;

    /** Degrees outside the tire window before the engineer mentions it. */
    public static final double RADIO_TEMP_MARGIN = 5.0;

    /** The engineer warns about a rival closing within this gap, in seconds. */
    public static final double RADIO_RIVAL_GAP = 1.0;

    /** The engineer warns about rain this many laps before it starts. */
    public static final int RADIO_RAIN_WARN_LAPS = 2;

    /** Races the Strategy Lab runs by default (brief: 200). Rounded down to whole scenarios of 7 strategies. */
    public static final int LAB_DEFAULT_RACES = 200;

    /** Target time for a default Strategy Lab run, in seconds, on a computer with 4 or more cores. */
    public static final double LAB_TARGET_SECONDS = 60.0;

    /** Real seconds between the end of the race and the results screen. */
    public static final double RESULTS_DELAY_SECONDS = 4.0;

    /** When the player skips to the results, the rest of the race is run this far ahead at most, in race seconds. */
    public static final double SKIP_TO_RESULTS_SECONDS = 600.0;

    /** Closest the racing line comes to the track edge, in meters (half a car plus a little). */
    public static final double RACING_LINE_EDGE_MARGIN = 4.5;

    /** Smoothing passes at each stride when finding the racing line. */
    public static final int RACING_LINE_PASSES = 300;

    /**
     * First (widest) sample stride of the racing line search. The search smooths every 32nd point
     * first, then every 16th, and so on down to every point, so long sweeps settle quickly and the
     * line uses the whole width of the track (wide on entry, apex, wide on exit).
     */
    public static final int RACING_LINE_COARSEST_STRIDE = 32;

    /** A car within this sideways distance of the racing line locks onto it, in meters. */
    public static final double LINE_LOCK_TOLERANCE = 0.6;

    /** Seconds to wait before retrying an incident whose car was busy. */
    public static final double INCIDENT_RETRY_SECONDS = 2.0;

    /** Shortest flat-out straight that gets a DRS zone, in meters. Balance choice. */
    public static final double DRS_MIN_STRAIGHT = 400.0;

    /** How far into a straight its DRS zone starts, in meters. */
    public static final double DRS_START_AFTER = 60.0;

    /** How far before a DRS zone its detection point is, in meters. */
    public static final double DRS_DETECTION_BEFORE = 250.0;

    /** A car less than this many seconds behind another at a detection point may use DRS (the real rule). */
    public static final double DRS_GAP_SECONDS = 1.0;

    /** Extra top speed with the DRS flap open, as a fraction. Balance choice (real DRS is worth about 10 to 15 km/h). */
    public static final double DRS_SPEED_BOOST = 0.04;

    /** First race lap on which DRS is allowed (the real rule: from the second lap). */
    public static final int DRS_FIRST_LAP = 2;

    /** DRS is switched off when the average track wetness is above this. */
    public static final double DRS_MAX_WETNESS = 0.2;

    /** Shortest time between any two engineer messages, in race seconds. */
    public static final double RADIO_MIN_GAP_SECONDS = 12.0;

    /** Computer cars move each planned stop up to this many laps earlier or later, at random. */
    public static final int AI_STOP_LAP_SPREAD = 1;

    /** Distance between neighbouring teams' pit boxes along the pit lane, in meters. */
    public static final double PIT_BOX_SPACING = 22.0;

    /** Middle of the row of pit boxes, in meters after the start/finish line. Leaves room to steer in after the pit entry. */
    public static final double PIT_BOXES_CENTER = 20.0;

    /** How far a car pulls sideways out of the fast lane into its box, in meters. */
    public static final double PIT_BOX_SHIFT = 12.0;

    /** How far before its box a car starts turning in, in meters. */
    public static final double PIT_BOX_TURN_IN = 60.0;

    /** A corner whose braking starts less than this many meters after the last apex is part of the same corner. */
    public static final double CORNER_MERGE_METERS = 50.0;

    /** Seconds a car leaving the pits waits for a safe gap before it squeezes into any space. */
    public static final double MERGE_PATIENCE_SECONDS = 4.0;

    /** Slack on the minimum gap when rejoining, so a car exactly one gap behind another still counts as clear, in meters. */
    public static final double MERGE_GAP_TOLERANCE = 1.0;

    /**
     * A car that has lost a side-by-side fight (passed, or an attacker backing out) runs at least this
     * much slower than its rival until it is clear behind, in m/s, so no pass stays side by side for long.
     */
    public static final double PASS_YIELD_SPEED = 5.0;

    /**
     * Braking is stronger than cornering grip by this factor: a car brakes in a straight line with the
     * whole tire, and an F1 car decelerates at up to about 5 g. Balance choice.
     */
    public static final double BRAKING_GRIP_FACTOR = 1.5;

    /** Extra deceleration from air drag and engine braking when lifting, in m/s squared. Balance choice. */
    public static final double DRAG_DECELERATION = 4.0;

    /**
     * How far from the centerline the corner-speed line may go, in meters: a real track is about
     * 12 to 15 m wide, so a car's center can be about 5 m either side. See Track.speedLine.
     */
    public static final double SPEED_LINE_HALF_WIDTH = 5.0;

    /** Sideways position of the Inside and Outside lanes, in meters from the centerline. */
    public static final double LANE_OFFSET = 14.0;

    /** Distance over which a car moves across into its fight lane, in meters. */
    public static final double LANE_CHANGE_METERS = 160.0;

    /** An attacker is settled in its lane this many meters before the braking point. */
    public static final double LANE_SETTLE_BEFORE_BRAKING = 20.0;

    /** A defender covering a side is settled this many meters before the braking point (it moves first). */
    public static final double COVER_SETTLE_BEFORE_BRAKING = 80.0;

    /** Distance after the apex over which a car blends back to the racing line, in meters. */
    public static final double CORNER_EXIT_METERS = 120.0;

    /** How far toward the outside edge a car that locked up runs wide, in meters from the centerline. */
    public static final double RUN_WIDE_OFFSET = 22.0;

    /** How much later a late-braking car brakes, in meters. Balance choice. */
    public static final double LATE_BRAKE_METERS = 25.0;

    /** Speed lost through the corner after a lock-up, as a fraction. Balance choice. */
    public static final double LOCKUP_SPEED_LOSS = 0.15;

    /** Extra tire wear from a lock-up (a flat spot). */
    public static final double LOCKUP_TIRE_WEAR = 0.01;

    /** Speed lost on the way into the corner on the Outside line (the long way round), as a fraction. */
    public static final double OUTSIDE_ENTRY_LOSS = 0.03;

    /** Speed lost on the exit after taking the Inside line (a tight, slow exit), as a fraction. */
    public static final double INSIDE_EXIT_LOSS = 0.06;

    /** Speed of a car with a punctured tire, as a fraction of normal. Balance choice. */
    public static final double PUNCTURE_SPEED_FRACTION = 0.55;

    /** Sideways speed while following a fight line, in m/s (the steering limit usually applies first). */
    public static final double PLAN_LATERAL_SPEED = 30.0;

    /** How strongly a driver's own line offset is pulled back toward the racing line each second. */
    public static final double LINE_BIAS_PULL_BACK = 0.15;

    /** Typical change of a driver's own line offset each second, in meters. */
    public static final double LINE_BIAS_STEP = 1.2;

    /** Largest own line offset after the first lap, in meters. */
    public static final double LINE_BIAS_RANGE = 3.0;

    /** Largest own line offset on the first lap (the field spreads out after the start), in meters. */
    public static final double LINE_BIAS_START_RANGE = 7.0;

    /** Shortest start reaction time, in seconds. */
    public static final double LAUNCH_DELAY_MIN = 0.05;

    /** Spread of start reaction times on top of the shortest, in seconds. Balance choice. */
    public static final double LAUNCH_DELAY_SPREAD = 0.35;

    /** Within this many seconds of a car ahead, a car loses grip in its dirty air through corners. */
    public static final double DIRTY_AIR_SECONDS = 1.0;

    /** Most corner speed lost right behind another car in its dirty air, as a fraction. Balance choice. */
    public static final double DIRTY_AIR_LOSS = 0.04;

    /** A car that chose Stay behind keeps this many seconds back from the car ahead. */
    public static final double STAY_BEHIND_GAP_SECONDS = 0.6;

    /** How much a computer driver's battery style moves its deploy threshold and how often it deploys. */
    public static final double AI_ERS_STYLE_RANGE = 0.3;

    /** Computer drivers decide afresh whether to deploy on each stretch of this length, in meters. */
    public static final double AI_ERS_STRETCH_METERS = 500.0;

    /** Share of stretches on which an average computer driver deploys. */
    public static final double AI_ERS_USE_SHARE = 0.6;

    /** Corner claim: weight of how far alongside the car behind is. */
    public static final double CLAIM_OVERLAP_WEIGHT = 2.5;

    /** Corner claim: how far alongside (0 nose to tail, 1 level) counts as an even claim before other factors. */
    public static final double CLAIM_LEVEL_POINT = 0.45;

    /** Corner claim: weight of being on the inside. */
    public static final double CLAIM_INSIDE_WEIGHT = 0.9;

    /** Corner claim: weight of the grip difference. */
    public static final double CLAIM_GRIP_WEIGHT = 6.0;

    /** Corner claim: bonus for braking late. */
    public static final double CLAIM_LATE_BRAKE_WEIGHT = 0.4;

    /** Corner claim: penalty for a car that locked up. */
    public static final double CLAIM_LOCKUP_WEIGHT = 3.0;

    /** Corner claim: how much a hard-to-pass track favors the car ahead. */
    public static final double CLAIM_TRACK_WEIGHT = 1.0;

    /** Corner claim: size of the random part (standard deviation). */
    public static final double CLAIM_NOISE = 0.3;

    /** Claims closer than this to even can end in contact. */
    public static final double CONTACT_BAND = 0.15;

    /** Chance of contact when a claim is that close. */
    public static final double CONTACT_CHANCE = 0.35;

    /** Chance that contact punctures a tire. */
    public static final double CONTACT_PUNCTURE_CHANCE = 0.2;

    /** Extra lock-up chance when pushing. */
    public static final double RISK_WHEN_PUSHING = 0.03;

    /** Lock-up chance from which the battle panel says the risk is medium. */
    public static final double RISK_WORD_MEDIUM = 0.12;

    /** Lock-up chance from which the battle panel says the risk is high. */
    public static final double RISK_WORD_HIGH = 0.25;

    /** AI attack edge: value of DRS. */
    public static final double AI_EDGE_DRS = 0.25;

    /** AI attack edge: value of having more battery charge (per full battery). */
    public static final double AI_EDGE_ERS = 0.3;

    /** AI attack edge: value of a grip advantage (per unit of grip). */
    public static final double AI_EDGE_GRIP = 4.0;

    /** AI attack edge: value of being close (per second inside the battle gap). */
    public static final double AI_EDGE_CLOSE = 0.3;

    /** AI attack chance before aggression and edge. */
    public static final double AI_ATTACK_BASE = 0.1;

    /** Chance a computer driver going down the inside brakes late (scaled by aggression). */
    public static final double AI_LATE_BRAKE_INSIDE = 0.45;

    /** Chance a computer driver on another line brakes late (scaled by aggression). */
    public static final double AI_LATE_BRAKE_OTHER = 0.15;

    /** Chance a computer defender covers the outside against an attack around the outside. */
    public static final double AI_COVER_OUTSIDE = 0.35;

    /** Share of computer defenders' late-braking decisions that they act on. */
    public static final double AI_DEFEND_LATE_SHARE = 0.5;

    /** Chance a computer attacker switches sides when the defender covers its side. */
    public static final double AI_SWITCH_SIDES_CHANCE = 0.6;

    /** The player's choice locks this many meters before their braking point. */
    public static final double PLAYER_LOCK_BEFORE_BRAKING = 60.0;

    /** Two cars side by side this many meters before the apex get a ruling on who has the corner. */
    public static final double TURN_IN_BEFORE_APEX = 60.0;

    /** Gap within which the player may launch an attack with the attack key, in seconds. */
    public static final double ON_DEMAND_ATTACK_GAP = 1.5;

    /** A battle needs at least this many seconds before the braking point, so there is time to choose. */
    public static final double BATTLE_MIN_LEAD_SECONDS = 1.0;

    /** After a pass, the player is offered the next car at once if it is within reach this many seconds later. */
    public static final double CHAIN_ATTACK_SECONDS = 6.0;

    /** A battle that has not settled after this many seconds is closed. */
    public static final double BATTLE_MAX_SECONDS = 30.0;

    /** Corners with a difficulty below this are described to the player as good places to pass. */
    public static final double EASY_CORNER = 0.3;

    /** Corners with a difficulty from this up are described to the player as hard places to pass. */
    public static final double HARD_CORNER = 0.65;

    /** A car giving way never slows below this fraction of its own pace (so a queue can never stall). */
    public static final double YIELD_FLOOR_FRACTION = 0.6;

    /** A car stops giving way after this many seconds even if still level. */
    public static final double YIELD_MAX_SECONDS = 6.0;

    /** Corner claim: how much a car behind that is not attacking holds back. */
    public static final double CLAIM_NO_ATTACK_PENALTY = 1.5;

    /** A car that is not attacking but finds itself level with the car ahead drops back this much slower, in m/s. */
    public static final double OVERLAP_DROP_SPEED = 2.0;

    /** Most passes of the side-by-side check per step (more are needed when three cars are squeezed). */
    public static final int SIDE_BY_SIDE_PASSES = 4;

    /** Smallest zoom (as a share of the follow zoom) used to frame the player's fight and its corner. */
    public static final double FIGHT_MIN_ZOOM = 0.35;

    /** Tire wear above which a tire can puncture. Balance choice. */
    public static final double PUNCTURE_WEAR_START = 0.95;

    /** Wear beyond the start at which the puncture chance reaches its base rate. */
    public static final double PUNCTURE_WEAR_SPAN = 0.05;

    /** Puncture chance per second at the base rate (it keeps rising with more wear). Balance choice. */
    public static final double PUNCTURE_CHANCE_PER_SECOND = 0.01;

    /** Cars within this distance of the followed car show their tire-condition bar, in meters. */
    public static final double DETAIL_RADIUS_METERS = 300.0;

    /** Steepest sideways movement per meter driven (about 17 degrees), so cars steer instead of sliding. */
    public static final double MAX_LATERAL_RATE = 0.3;

    /** Sideways gap that counts as alongside for two cars in the pit lane, in meters (still wider than a car). */
    public static final double PIT_LATERAL_CLEARANCE = 7.6;

    /** Steepest sideways movement per meter in the pit lane, whose entry and exit roads turn off sharply. */
    public static final double PIT_MAX_LATERAL_RATE = 0.7;

    /** Sideways speed a nearly stopped car may still creep at (to park or leave a pit box), in m/s. */
    public static final double CREEP_LATERAL_SPEED = 1.5;

    /** How far ahead, in meters, the car drawing looks to work out which way it points. */
    public static final double HEADING_PROBE_METERS = 6.0;

    /** Speed fraction a failed attacker gives up while dropping back behind the defender. */
    public static final double ABORT_SLOWDOWN = 0.12;

    /** How far an attacker pulls out of the slipstream, in meters from the centerline. */
    public static final double PULL_OUT_LATERAL = 15.0;

    /** How far a covering defender moves toward the inside, in meters from the centerline. */
    public static final double COVER_LATERAL = 12.0;

    /** Sideways speed of a defender moving across to cover, in m/s. */
    public static final double COVER_SPEED = 11.0;

    /** Pass score added per unit of overlap (from -1, a gap behind, to +1, a gap ahead). Balance choice. */
    public static final double PASS_OVERLAP_WEIGHT = 1.5;

    /** Pass score bonus for going around the outside (smaller than Attack's). Balance choice. */
    public static final double PASS_OUTSIDE_BONUS = 0.5;

    /** Share of the corner difficulty an outside move still suffers. */
    public static final double OUTSIDE_CORNER_SHARE = 0.5;

    /** Share of the defender's cover bonus that still applies against an outside move. */
    public static final double OUTSIDE_DEFEND_SHARE = 0.35;

    /** Share of the normal attack risk taken by an outside move. */
    public static final double OUTSIDE_RISK_SHARE = 0.4;

    /** Chance a computer attacker goes around the outside instead of the inside. */
    public static final double AI_OUTSIDE_CHANCE = 0.25;

    /** Computer drivers fighting wheel to wheel deploy ERS while above this charge. */
    public static final double AI_FIGHT_ERS_ABOVE = 0.1;

    /** An attacker pulls out of the slipstream once this close behind, in seconds. */
    public static final double PULL_OUT_GAP_SECONDS = 0.4;

    /** An attacker pulls out at the latest this long before the braking point, in seconds. */
    public static final double PULL_OUT_BEFORE_BRAKING_SECONDS = 1.2;


    /** Largest swing of a car's form (its pace from moment to moment) above or below its race pace. */
    public static final double FORM_RANGE = 0.008;

    /** Random change in form per second (standard deviation). Balance choice. */
    public static final double FORM_STEP = 0.0015;

    /** Share of the form that fades back toward zero each second, so form wanders but does not drift away. */
    public static final double FORM_PULL_BACK = 0.1;

    /** Camera zoom multiplier while the player is in a battle. */
    public static final double BATTLE_ZOOM = 1.35;

    /** Cars either side in road order checked when keeping side-by-side room. */
    public static final int SIDE_BY_SIDE_NEIGHBOURS = 3;

    /** Shortest tire life, as a share of the reference race's, however short the race. */
    public static final double MIN_TIRE_LIFE_SCALE = 0.3;

    /** Races shorter than this many laps do not have the two-compound rule. */
    public static final int COMPOUND_RULE_MIN_LAPS = 5;

    /** Share of computer drivers that do not stop in a short race. Balance choice. */
    public static final double AI_SHARE_NO_STOP = 0.8;

    /** Grip levels are rounded to this step when choosing a speed profile, so few profiles are built. */
    public static final double PROFILE_GRIP_STEP = 0.004;

    /** A point counts as straight when its corner limit is at least this fraction of top speed. */
    public static final double STRAIGHT_FRACTION = 0.97;

    /** Track temperature in Celsius used until weather is simulated (M6). Balance choice. */
    public static final double NEUTRAL_TRACK_TEMP = 35.0;

    /** Air temperature in Celsius used until weather is simulated (M6). Balance choice. */
    public static final double NEUTRAL_AIR_TEMP = 25.0;

    /** Share of track (versus air) temperature in the temperature tires settle toward. Balance choice. */
    public static final double TRACK_TEMP_SHARE = 0.6;

    /** Tire temperature when fitted, from tire blankets (real blankets run near 70 C). */
    public static final double TIRE_BLANKET_TEMP = 70.0;

    /**
     * Degrees a tire runs above ambient at Balanced push under average load. Balance choice: puts a
     * Medium in the middle of its window (about 100 C) in neutral conditions.
     */
    public static final double TIRE_HEAT_RISE = 63.0;

    /** Time in seconds for tire temperature to close most of the gap to its target. Balance choice. */
    public static final double TIRE_TEMP_SECONDS = 7.0;

    /** How much a wet track cools slick tires at full wetness (fraction of heating removed). */
    public static final double SLICK_WET_COOLING = 0.4;

    /** Extra heating of rain tires per unit of track that is drier than they want. */
    public static final double WET_TIRE_OVERHEAT = 1.0;

    /** Degrees above the window that double the wear rate. Balance choice. */
    public static final double OVERHEAT_WEAR_DEGREES = 45.0;

    /** Degrees outside the window at which grip has fallen to MIN_TEMP_GRIP. Balance choice. */
    public static final double TEMP_FALLOFF_DEGREES = 35.0;

    /** Lowest grip factor from being out of the temperature window. */
    public static final double MIN_TEMP_GRIP = 0.85;

    /** Wear at which the grip cliff begins. */
    public static final double WEAR_CLIFF = 0.8;

    /** Grip lost between new and the start of the cliff. */
    public static final double WEAR_GRIP_BEFORE_CLIFF = 0.04;

    /** Grip lost across the cliff, from WEAR_CLIFF to end of life. */
    public static final double WEAR_GRIP_IN_CLIFF = 0.10;

    /** Grip lost per unit of wear past the end of life. */
    public static final double WEAR_GRIP_PAST_END = 0.5;

    /** Lowest grip factor from wear. */
    public static final double MIN_WEAR_GRIP = 0.65;

    /** Wetness where slicks fall off the cliff. Balance choice. */
    public static final double SLICK_WET_CLIFF = 0.3;

    /** Grip slicks lose between dry and the cliff. */
    public static final double SLICK_DAMP_LOSS = 0.08;

    /** Further grip slicks lose from the cliff to a soaked track. */
    public static final double SLICK_CLIFF_LOSS = 0.47;

    /** Grip a rain tire loses per unit of wetness away from its ideal. */
    public static final double WET_TIRE_MISMATCH_LOSS = 0.25;

    /** Brake temperature at the start, in Celsius. */
    public static final double BRAKE_START_TEMP = 300.0;

    /** Degrees per second gained per unit of braking power (deceleration times speed). Balance choice. */
    public static final double BRAKE_HEATING = 0.27;

    /** Base fraction of the gap to air temperature lost per second. Balance choice. */
    public static final double BRAKE_COOLING_BASE = 0.045;

    /** Extra cooling fraction per second for each m/s of speed. Balance choice. */
    public static final double BRAKE_COOLING_PER_SPEED = 0.0024;

    /** Brake temperature where fade starts, in Celsius. Carbon brakes work up to about 1000 C. */
    public static final double BRAKE_FADE_TEMP = 1000.0;

    /** Degrees above the fade temperature where fade reaches its maximum. */
    public static final double BRAKE_FADE_SPAN = 300.0;

    /** Most stopping grip that fade can remove. */
    public static final double BRAKE_MAX_FADE = 0.08;

    /** Bottom of the brakes' good working range, for the gauge. */
    public static final double BRAKE_GOOD_LOW = 400.0;

    /** Battery charge at the start. */
    public static final double ERS_START_CHARGE = 0.9;

    /** Charge gained per second of 1 g braking. Balance choice: about a quarter battery per lap. */
    public static final double ERS_HARVEST_PER_G_SECOND = 0.02;

    /** Charge spent per second of deploying: a full battery lasts 10 s. Balance choice. */
    public static final double ERS_DEPLOY_PER_SECOND = 0.10;

    /** Speed gain while deploying, as a fraction: big enough to see the car surge. Balance choice. */
    public static final double ERS_SPEED_BOOST = 0.08;

    /** Computer drivers deploy on straights while the battery is above this. Balance choice. */
    public static final double AI_ERS_DEPLOY_ABOVE = 0.45;

    /**
     * Creates nothing; this class only holds constants.
     */
    private Tuning() {
    }
}
