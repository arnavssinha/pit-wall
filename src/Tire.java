/**
 * One set of tires: its compound, temperature and wear, and the grip that results.
 * Temperature chases a target: working the car hard in corners and under braking raises the
 * target, a cool track lowers it. Grip is the compound's base grip times three factors:
 * temperature (best inside the compound's window), wear (a gentle fall, then a cliff near the end
 * of life) and track wetness (slicks fall off a cliff in the wet; wet tires overheat in the dry).
 */
public final class Tire {

    /** The compound. */
    private final Compound compound;

    /** Temperature in degrees Celsius. */
    private double temperature;

    /** Wear from 0 (new) to 1 (end of life); it can go past 1, which is very slow. */
    private double wear;

    /**
     * Creates a fresh set, warmed by tire blankets.
     *
     * @param compound the compound
     */
    public Tire(Compound compound) {
        this.compound = compound;
        this.temperature = Tuning.TIRE_BLANKET_TEMP;
        this.wear = 0;
    }

    /**
     * Updates temperature and wear for one step.
     * Wear per meter is set so a whole lap at Balanced push, average load and the right temperature
     * uses exactly 1 / lifeLaps of the tire, on a track with wear multiplier 1.0.
     *
     * @param dt step length in seconds
     * @param meters distance driven this step
     * @param load how hard the car is working now (1 on a straight, more in corners and braking)
     * @param meanLoad the track's average load over a lap, so heat and wear are fair across tracks
     * @param lapLength lap length in meters
     * @param push the current push level
     * @param trackWearMultiplier how abrasive the track is (1.0 is neutral)
     * @param ambient mix of track and air temperature in Celsius
     * @param wetness track wetness from 0 to 1
     */
    public void update(double dt, double meters, double load, double meanLoad, double lapLength,
                       PushLevel push, double trackWearMultiplier, double ambient, double wetness) {
        double relativeLoad = load / meanLoad;
        double target = ambient + Tuning.TIRE_HEAT_RISE * push.getHeat() * wetHeating(wetness) * relativeLoad;
        double blend = Math.min(1.0, dt / Tuning.TIRE_TEMP_SECONDS);
        temperature += (target - temperature) * blend;

        double perMeter = 1.0 / (compound.getLifeLaps() * lapLength);
        wear += perMeter * meters * relativeLoad * push.getWearFactor() * trackWearMultiplier * overheatWear();
    }

    /**
     * Extra heating or cooling from the track surface: wet tires on a drying track overheat,
     * slicks on a wet track run cold.
     *
     * @param wetness track wetness from 0 to 1
     * @return heat multiplier
     */
    private double wetHeating(double wetness) {
        if (compound.isDry()) {
            return 1.0 - Tuning.SLICK_WET_COOLING * wetness;
        }
        double dryness = Math.max(0, compound.getIdealWetness() - wetness);
        return 1.0 + Tuning.WET_TIRE_OVERHEAT * dryness;
    }

    /**
     * Overheated tires wear faster: each degree above the window adds a little.
     *
     * @return wear multiplier, 1 or more
     */
    private double overheatWear() {
        double over = temperature - compound.getWindowHigh();
        return over <= 0 ? 1.0 : 1.0 + over / Tuning.OVERHEAT_WEAR_DEGREES;
    }

    /**
     * Returns the grip multiplier this tire gives right now.
     *
     * @param wetness track wetness from 0 to 1
     * @return grip relative to a fresh Soft in its window on a dry track
     */
    public double grip(double wetness) {
        return compound.getDryGrip() * temperatureFactor() * wearFactor() * wetnessFactor(wetness);
    }

    /**
     * Grip lost to being outside the temperature window: none inside it, then a bell-shaped
     * fall-off the further outside it is.
     *
     * @return factor from Tuning.MIN_TEMP_GRIP to 1
     */
    public double temperatureFactor() {
        double outside = 0;
        if (temperature < compound.getWindowLow()) {
            outside = compound.getWindowLow() - temperature;
        } else if (temperature > compound.getWindowHigh()) {
            outside = temperature - compound.getWindowHigh();
        }
        double ratio = outside / Tuning.TEMP_FALLOFF_DEGREES;
        return Math.max(Tuning.MIN_TEMP_GRIP, 1.0 - (1.0 - Tuning.MIN_TEMP_GRIP) * ratio * ratio);
    }

    /**
     * Grip lost to wear: a slow fall until the cliff, then a steep one, and worse still past the end of life.
     *
     * @return factor from Tuning.MIN_WEAR_GRIP to 1
     */
    public double wearFactor() {
        double cliff = Tuning.WEAR_CLIFF;
        double atCliff = 1.0 - Tuning.WEAR_GRIP_BEFORE_CLIFF;
        double atEnd = atCliff - Tuning.WEAR_GRIP_IN_CLIFF;
        double factor;
        if (wear <= cliff) {
            factor = 1.0 - Tuning.WEAR_GRIP_BEFORE_CLIFF * wear / cliff;
        } else if (wear <= 1.0) {
            factor = atCliff - Tuning.WEAR_GRIP_IN_CLIFF * (wear - cliff) / (1.0 - cliff);
        } else {
            factor = atEnd - Tuning.WEAR_GRIP_PAST_END * (wear - 1.0);
        }
        return Math.max(Tuning.MIN_WEAR_GRIP, factor);
    }

    /**
     * Grip lost because the tire does not suit the track's wetness.
     * Slicks lose a little in damp conditions and then fall off a cliff. Intermediates and wets
     * are best at their ideal wetness and lose grip steadily away from it.
     *
     * @param wetness track wetness from 0 to 1
     * @return factor from 0 to 1
     */
    public double wetnessFactor(double wetness) {
        if (compound.isDry()) {
            double edge = Tuning.SLICK_WET_CLIFF;
            if (wetness <= edge) {
                return 1.0 - Tuning.SLICK_DAMP_LOSS * wetness / edge;
            }
            double past = Math.min(1.0, (wetness - edge) / (1.0 - edge));
            return (1.0 - Tuning.SLICK_DAMP_LOSS) - Tuning.SLICK_CLIFF_LOSS * Math.sqrt(past);
        }
        double away = Math.abs(wetness - compound.getIdealWetness());
        return 1.0 - Tuning.WET_TIRE_MISMATCH_LOSS * away;
    }

    /**
     * Returns the compound.
     *
     * @return the compound
     */
    public Compound getCompound() {
        return compound;
    }

    /**
     * Returns the temperature.
     *
     * @return degrees Celsius
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns the wear.
     *
     * @return wear from 0 (new) upward; 1 is end of life
     */
    public double getWear() {
        return wear;
    }
}
