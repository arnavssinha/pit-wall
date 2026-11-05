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

    /** Index into TIME_SPEEDS used at race start (2x, chosen by the project owner). */
    public static final int DEFAULT_TIME_SPEED_INDEX = 1;

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

    /** Track width in meters. Real F1 circuits are 12 to 15 m wide; 12 m is a drawing choice. */
    public static final double TRACK_WIDTH = 12.0;

    /** Car length in meters, close to a 2026 F1 car. Used for spacing and drawing. */
    public static final double CAR_LENGTH = 5.4;

    /** Car width in meters, close to a 2026 F1 car. Used for drawing. */
    public static final double CAR_WIDTH = 1.9;

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
    public static final double FOLLOW_TIME_GAP = 0.30;

    /**
     * Distance between grid slots in meters. Balance choice: real staggered grids put a car every
     * 8 m, but slots must be at least MIN_SEPARATION apart here.
     */
    public static final double GRID_SLOT_SPACING = 24.0;

    /** Distance from the start line back to pole position, in meters. Balance choice. */
    public static final double GRID_FIRST_SLOT_OFFSET = 14.0;

    /** Sideways offset of grid slots from the centerline, alternating left and right, in meters. */
    public static final double GRID_LATERAL_OFFSET = 2.5;

    /** Rate at which a car moves back to the racing line after the start, in meters per second. */
    public static final double LATERAL_RETURN_SPEED = 0.6;

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
    public static final double SLIPSTREAM_MAX_BONUS = 0.025;

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
    public static final double TIRE_TEMP_SECONDS = 12.0;

    /** How much a wet track cools slick tires at full wetness (fraction of heating removed). */
    public static final double SLICK_WET_COOLING = 0.4;

    /** Extra heating of rain tires per unit of track that is drier than they want. */
    public static final double WET_TIRE_OVERHEAT = 1.0;

    /** Degrees above the window that double the wear rate. Balance choice. */
    public static final double OVERHEAT_WEAR_DEGREES = 30.0;

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
    public static final double BRAKE_HEATING = 0.22;

    /** Base fraction of the gap to air temperature lost per second. Balance choice. */
    public static final double BRAKE_COOLING_BASE = 0.03;

    /** Extra cooling fraction per second for each m/s of speed. Balance choice. */
    public static final double BRAKE_COOLING_PER_SPEED = 0.0016;

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
    public static final double ERS_HARVEST_PER_G_SECOND = 0.012;

    /** Charge spent per second of deploying: a full battery lasts 20 s. Balance choice. */
    public static final double ERS_DEPLOY_PER_SECOND = 0.05;

    /** Speed gain while deploying, as a fraction. Balance choice. */
    public static final double ERS_SPEED_BOOST = 0.03;

    /** Computer drivers deploy on straights while the battery is above this. Balance choice. */
    public static final double AI_ERS_DEPLOY_ABOVE = 0.45;

    /**
     * Creates nothing; this class only holds constants.
     */
    private Tuning() {
    }
}
