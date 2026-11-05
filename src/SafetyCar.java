/**
 * The safety car. While it is out, everyone drives slowly, overtaking is not allowed, and cars that
 * are far behind the car in front may go a little faster so the field bunches up. Because everyone
 * is slow, a pit stop costs less time than usual, which is the gamble the player faces.
 */
public final class SafetyCar {

    /** True while the safety car is on track. */
    private boolean active;

    /** Number of times it has been sent out. */
    private int deployments;

    /**
     * Creates a safety car in the pit.
     */
    public SafetyCar() {
        super();
    }

    /**
     * Sends the safety car out.
     */
    public void deploy() {
        active = true;
        deployments++;
    }

    /**
     * Brings the safety car in.
     */
    public void end() {
        active = false;
    }

    /**
     * Returns the speed fraction a car may use under the safety car.
     *
     * @param leader true for the car leading the race (it follows the safety car directly)
     * @param gapAhead distance to the car ahead in meters
     * @return fraction of the car's normal target speed, or 1 when the safety car is in
     */
    public double paceFactor(boolean leader, double gapAhead) {
        if (!active) {
            return 1.0;
        }
        if (!leader && gapAhead > Tuning.SC_BUNCH_GAP) {
            return Tuning.SC_CATCH_UP_PACE;
        }
        return Tuning.SC_PACE;
    }

    /**
     * Returns whether the safety car is out.
     *
     * @return true while active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns how many times it has been out.
     *
     * @return deployments
     */
    public int getDeployments() {
        return deployments;
    }
}
