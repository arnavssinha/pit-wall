/**
 * The handful of physics formulas the speed model uses, kept together so they are easy to explain.
 * The model treats a car as a point that can accelerate, brake and corner up to a grip limit.
 * Downforce makes grip grow with speed, which is why F1 cars take fast corners flat out.
 */
public final class CarPhysics {

    /**
     * Not instantiated; all methods are static.
     */
    private CarPhysics() {
    }

    /**
     * Total grip acceleration available at a speed, in m/s squared.
     * Grip is friction times (gravity plus downforce), and downforce grows with speed squared.
     *
     * @param speed speed in m/s
     * @param gripScale multiplier on the friction coefficient (track calibration, later tires and weather)
     * @return the grip-limited acceleration in m/s squared
     */
    public static double gripAcceleration(double speed, double gripScale) {
        double mu = Tuning.BASE_GRIP_MU * gripScale;
        return mu * (Tuning.GRAVITY + Tuning.DOWNFORCE_PER_SPEED_SQUARED * speed * speed);
    }

    /**
     * Fastest speed a car can take a bend of the given curvature.
     * Cornering needs speed squared times curvature of sideways acceleration. Setting that equal
     * to the grip limit and solving for speed gives
     * v = sqrt(mu g / (curvature minus mu times downforce)). If the bend is gentle enough that
     * downforce alone holds the car, the answer is the straight-line top speed.
     *
     * @param curvature curvature in 1 per meter (sign ignored)
     * @param gripScale multiplier on the friction coefficient
     * @param topSpeed the car's straight-line top speed in m/s
     * @return the corner speed limit in m/s
     */
    public static double cornerSpeedLimit(double curvature, double gripScale, double topSpeed) {
        double mu = Tuning.BASE_GRIP_MU * gripScale;
        double denominator = Math.abs(curvature) - mu * Tuning.DOWNFORCE_PER_SPEED_SQUARED;
        if (denominator <= 0) {
            return topSpeed;
        }
        return Math.min(topSpeed, Math.sqrt(mu * Tuning.GRAVITY / denominator));
    }

    /**
     * Forward acceleration available at a speed. It is strongest when slow and falls to zero
     * at top speed, a simple stand-in for engine power fading against air resistance.
     *
     * @param speed speed in m/s
     * @param topSpeed top speed in m/s
     * @return acceleration in m/s squared, never negative
     */
    public static double acceleration(double speed, double topSpeed) {
        double ratio = speed / topSpeed;
        return Math.max(0.0, Tuning.MAX_ACCELERATION * (1.0 - ratio * ratio));
    }

    /**
     * How hard the tires are working, as a load number: 1 when cruising straight, plus the
     * cornering and braking acceleration measured in g. Tire heat and wear follow this number.
     *
     * @param lateral cornering acceleration in m/s squared
     * @param braking braking deceleration in m/s squared
     * @return load, 1 or more
     */
    public static double load(double lateral, double braking) {
        return 1.0 + (lateral + braking) / Tuning.GRAVITY;
    }

    /**
     * Braking deceleration available at a speed, limited by grip (so downforce helps braking too).
     *
     * @param speed speed in m/s
     * @param gripScale multiplier on the friction coefficient
     * @return deceleration in m/s squared, as a positive number
     */
    public static double braking(double speed, double gripScale) {
        return gripAcceleration(speed, gripScale) * Tuning.BRAKING_GRIP_FACTOR + Tuning.DRAG_DECELERATION;
    }
}
