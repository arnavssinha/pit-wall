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

    /** Smallest allowed nose-to-nose distance between two cars, in meters: one car length plus 1 m. */
    public static final double MIN_SEPARATION = CAR_LENGTH + 1.0;

    /**
     * A following car will not close to less than this time gap until overtaking exists (M4).
     * Balance choice.
     */
    public static final double FOLLOW_TIME_GAP = 0.30;

    /** Distance between grid slots in meters. Balance choice close to real staggered grids. */
    public static final double GRID_SLOT_SPACING = 8.0;

    /** Distance from the start line back to pole position, in meters. Balance choice. */
    public static final double GRID_FIRST_SLOT_OFFSET = 10.0;

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

    /**
     * Creates nothing; this class only holds constants.
     */
    private Tuning() {
    }
}
