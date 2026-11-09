/**
 * One car and its driver: where it is, how fast it goes, its tires, boost battery and lap times. Race moves
 * the cars; Car keeps their state and does the small calculations about itself.
 */
public final class Car {

    /** How hard the driver is pushing: faster costs more tire wear. */
    public enum Mode {

        /** Saves the tires. */
        CONSERVE(0.975, 0.6),

        /** Normal race pace. */
        NORMAL(1.0, 1.0),

        /** Attacks the lap. */
        PUSH(1.03, 1.8);

        /** Speed multiplier. */
        final double pace;

        /** Tire wear multiplier. */
        final double wear;

        /** Creates a mode. */
        Mode(double pace, double wear) {
            this.pace = pace;
            this.wear = wear;
        }
    }

    /** Three-letter driver code, such as "VER". */
    public final String code;

    /** Team color as 0xRRGGBB. */
    public final int color;

    /** True for the player's car. */
    public final boolean player;

    /** Small random speed difference between cars, around 1.0. */
    public final double pace;

    /** Distance driven since the start, in meters (lap 2 starts at one lap length). */
    public double distance;

    /** Sideways offset from the centerline, in meters (positive = left). */
    public double lateral;

    /** Where the car is steering sideways to, in meters. */
    public double lateralTarget;

    /** Speed in m/s. */
    public double speed;

    /** Tires fitted. */
    public Compound tire = Compound.MEDIUM;

    /** Tire wear from 0 (new) to 1 (worn out). */
    public double wear;

    /** Boost battery charge from 0 to 1. */
    public double boost = 0.6;

    /** Distance until which the Overtake boost is on. */
    public double boostUntil = -1e9;

    /** Pace mode. */
    public Mode mode = Mode.NORMAL;

    /** Tires to fit at the next pit stop, or null if not boxing. */
    public Compound boxFor;

    /** Seconds left standing in the pit box (0 when not stopped). */
    public double pitTimer;

    /** True while in the pit lane. */
    public boolean inPit;

    /** Number of pit stops made. */
    public int stops;

    /** Car this car is passing, or null. */
    public Car passing;

    /** Laps completed. */
    public int laps;

    /** Race time when the current lap started. */
    public double lapStart;

    /** True once the car has taken the chequered flag. */
    public boolean finished;

    /** Race time at the finish. */
    public double finishTime;

    /** Race time at which this car pulls away from the grid. */
    public double launchAt;

    /** Creates a car. */
    public Car(String code, int color, boolean player, double pace) {
        this.code = code;
        this.color = color;
        this.player = player;
        this.pace = pace;
    }

    /** Returns the tire grip now, from the compound, the track wetness and the wear. */
    public double grip(double wetness) {
        return tire.grip(wetness) * (1 - 0.3 * wear * wear);
    }

    /** Returns whether the Overtake boost is on. */
    public boolean boosting() {
        return distance < boostUntil;
    }
}
