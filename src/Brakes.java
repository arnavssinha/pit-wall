/**
 * The brakes' temperature, which rises with hard braking and falls on the straights.
 * Past a threshold the brakes fade: the car cannot brake as hard, so it has to brake earlier
 * and loses time in every braking zone until they cool down.
 */
public final class Brakes {

    /** Temperature in degrees Celsius. */
    private double temperature;

    /**
     * Creates brakes at their starting temperature.
     */
    public Brakes() {
        this.temperature = Tuning.BRAKE_START_TEMP;
    }

    /**
     * Updates the temperature for one step.
     * Heating follows braking power (deceleration times speed). Cooling grows with the gap to air
     * temperature and with speed, because faster air flow carries more heat away.
     *
     * @param dt step length in seconds
     * @param speed speed in m/s
     * @param deceleration braking deceleration in m/s squared (0 when not braking)
     * @param heat push-level heat multiplier
     * @param airTemp air temperature in Celsius
     */
    public void update(double dt, double speed, double deceleration, double heat, double airTemp) {
        double heating = Tuning.BRAKE_HEATING * deceleration * speed * heat;
        double coolingRate = Tuning.BRAKE_COOLING_BASE + Tuning.BRAKE_COOLING_PER_SPEED * speed;
        double cooling = (temperature - airTemp) * coolingRate;
        temperature += (heating - cooling) * dt;
    }

    /**
     * Adds a burst of heat, for example from a late-braking attack.
     *
     * @param degrees degrees Celsius to add
     */
    public void addHeat(double degrees) {
        temperature += degrees;
    }

    /**
     * Returns how much of the car's normal stopping grip the brakes still deliver.
     *
     * @return 1 when fine, lower when fading
     */
    public double fadeFactor() {
        double over = temperature - Tuning.BRAKE_FADE_TEMP;
        if (over <= 0) {
            return 1.0;
        }
        return 1.0 - Math.min(Tuning.BRAKE_MAX_FADE, Tuning.BRAKE_MAX_FADE * over / Tuning.BRAKE_FADE_SPAN);
    }

    /**
     * Returns the temperature.
     *
     * @return degrees Celsius
     */
    public double getTemperature() {
        return temperature;
    }
}
