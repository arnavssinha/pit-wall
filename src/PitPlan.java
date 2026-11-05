import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A car's tire strategy for the race: the starting compound and the planned stops in lap order.
 * Stops are removed from the front of the list as they are made (or cancelled).
 */
public final class PitPlan {

    /** Tires fitted on the grid. */
    private Compound startCompound;

    /** Stops still to come, in lap order. */
    private final ArrayList<PitStop> stops = new ArrayList<PitStop>();

    /**
     * Creates a plan with no stops.
     *
     * @param startCompound tires for the start
     */
    public PitPlan(Compound startCompound) {
        this.startCompound = startCompound;
    }

    /**
     * Adds a stop, keeping the list in lap order.
     *
     * @param stop the stop
     */
    public void addStop(PitStop stop) {
        stops.add(stop);
        Collections.sort(stops, (a, b) -> Integer.compare(a.getLap(), b.getLap()));
    }

    /**
     * Returns the next stop still to come.
     *
     * @return the stop, or null if none remain
     */
    public PitStop nextStop() {
        return stops.isEmpty() ? null : stops.get(0);
    }

    /**
     * Removes the next stop (it was made or cancelled).
     */
    public void removeNextStop() {
        if (!stops.isEmpty()) {
            stops.remove(0);
        }
    }

    /**
     * Returns the starting compound.
     *
     * @return the compound
     */
    public Compound getStartCompound() {
        return startCompound;
    }

    /**
     * Changes the starting compound.
     *
     * @param compound the new start compound
     */
    public void setStartCompound(Compound compound) {
        startCompound = compound;
    }

    /**
     * Returns the stops still to come.
     *
     * @return the stops, read-only
     */
    public List<PitStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    /**
     * Returns the compound for a stint: 0 is the start, 1 after the first stop, and so on.
     *
     * @param stint stint number
     * @return the compound
     */
    public Compound stintCompound(int stint) {
        return stint == 0 ? startCompound : stops.get(stint - 1).getCompound();
    }

    /**
     * Sets the compound for a stint.
     *
     * @param stint stint number
     * @param compound the compound
     */
    public void setStintCompound(int stint, Compound compound) {
        if (stint == 0) {
            startCompound = compound;
        } else {
            stops.get(stint - 1).setCompound(compound);
        }
    }

    /**
     * Returns the number of stints.
     *
     * @return stops plus one
     */
    public int stintCount() {
        return stops.size() + 1;
    }

    /**
     * Returns whether the plan uses at least two different dry compounds, as the dry-race rule requires.
     *
     * @return true if the rule is met
     */
    public boolean usesTwoDryCompounds() {
        Compound first = null;
        for (int i = 0; i < stintCount(); i++) {
            Compound c = stintCompound(i);
            if (!c.isDry()) {
                continue;
            }
            if (first == null) {
                first = c;
            } else if (c != first) {
                return true;
            }
        }
        return false;
    }
}
