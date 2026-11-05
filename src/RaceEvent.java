/**
 * Something scheduled to happen at a set race time: rain starting or stopping, a car stopping on
 * track (which brings out the safety car), the safety car coming in, or a mechanical retirement.
 * Events are ordered by time so the queue always hands out the earliest one first.
 */
public final class RaceEvent implements Comparable<RaceEvent> {

    /** The kinds of event. */
    public enum Type {
        /** Rain begins. */
        RAIN_START,
        /** Rain ends. */
        RAIN_STOP,
        /** A car stops on track and the safety car is sent out. */
        SAFETY_CAR,
        /** The safety car comes in and racing resumes. */
        SAFETY_CAR_END,
        /** A car retires with a mechanical failure. */
        RETIREMENT
    }

    /** Race time when it happens, in seconds. */
    private final double time;

    /** What happens. */
    private final Type type;

    /** The car involved, or null. */
    private final Car car;

    /**
     * Creates an event.
     *
     * @param time race time in seconds
     * @param type what happens
     * @param car the car involved, or null
     */
    public RaceEvent(double time, Type type, Car car) {
        this.time = time;
        this.type = type;
        this.car = car;
    }

    /**
     * Orders events by time, earliest first.
     *
     * @param other another event
     * @return negative if this event is earlier
     */
    @Override
    public int compareTo(RaceEvent other) {
        return Double.compare(time, other.time);
    }

    /**
     * Returns when it happens.
     *
     * @return race time in seconds
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns what happens.
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the car involved.
     *
     * @return the car, or null
     */
    public Car getCar() {
        return car;
    }
}
