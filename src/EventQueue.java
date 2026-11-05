import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The race's timetable of scheduled events, kept in a PriorityQueue so the earliest event is
 * always at the front, no matter in what order events were added.
 */
public final class EventQueue {

    /** Events waiting to happen, earliest first. */
    private final PriorityQueue<RaceEvent> queue = new PriorityQueue<RaceEvent>();

    /**
     * Creates an empty queue.
     */
    public EventQueue() {
        super();
    }

    /**
     * Adds an event.
     *
     * @param event the event
     */
    public void schedule(RaceEvent event) {
        queue.add(event);
    }

    /**
     * Removes and returns every event due by a given time, earliest first.
     *
     * @param time the current race time in seconds
     * @return the due events (empty if none)
     */
    public List<RaceEvent> takeDue(double time) {
        List<RaceEvent> due = new ArrayList<RaceEvent>();
        while (!queue.isEmpty() && queue.peek().getTime() <= time) {
            due.add(queue.poll());
        }
        return due;
    }

    /**
     * Returns the next event without removing it.
     *
     * @return the earliest event, or null if none
     */
    public RaceEvent peek() {
        return queue.peek();
    }

    /**
     * Returns how many events are waiting.
     *
     * @return the count
     */
    public int size() {
        return queue.size();
    }
}
