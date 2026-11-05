import java.util.HashMap;

/**
 * Tunes each track's grip so a lone car's lap matches the track's real typical race lap.
 * One clean lap is simulated with the same car physics the race uses; the grip multiplier is
 * then found by bisection (more grip always means a faster lap, so halving the search range
 * each time converges). Results are cached per track because the Strategy Lab builds many races.
 */
public final class LapCalibrator {

    /** Calibrated grip scale for each track id, so each track is solved only once. */
    private static final HashMap<String, Double> CACHE = new HashMap<String, Double>();

    /** Number of bisection rounds; each halves the search range, so 40 is far more than enough. */
    private static final int ITERATIONS = 40;

    /**
     * Not instantiated; all methods are static.
     */
    private LapCalibrator() {
    }

    /**
     * Returns the grip scale that makes a clean lap match the track's target lap time.
     * If even the highest allowed grip is too slow (or the lowest too fast), the nearest
     * bound is returned and the calibration test will report the miss.
     *
     * @param track the track
     * @return grip multiplier between Tuning.CALIBRATION_MIN_GRIP and Tuning.CALIBRATION_MAX_GRIP
     */
    public static synchronized double gripScaleFor(Track track) {
        String id = track.getProfile().getId();
        Double cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        double target = track.getProfile().getMedianRaceLapSeconds();
        double low = Tuning.CALIBRATION_MIN_GRIP;
        double high = Tuning.CALIBRATION_MAX_GRIP;
        double result;
        if (cleanLapSeconds(track, high) > target) {
            result = high;
        } else if (cleanLapSeconds(track, low) < target) {
            result = low;
        } else {
            for (int i = 0; i < ITERATIONS; i++) {
                double mid = (low + high) / 2;
                if (cleanLapSeconds(track, mid) > target) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            result = (low + high) / 2;
        }
        CACHE.put(id, result);
        return result;
    }

    /**
     * Measures the average tire load per meter over a clean lap at the reference condition
     * (Balanced, fresh Medium, no ERS). Tire wear and heat are divided by this number, so on every
     * track a lap at average effort costs exactly 1 / lifeLaps of a tire.
     *
     * @param track the track
     * @param gripScale the track's calibrated grip scale
     * @return mean load per meter, 1 or more
     */
    public static double meanLoadPerMeter(Track track, double gripScale) {
        double grip = gripScale * Compound.MEDIUM.getDryGrip();
        SpeedProfile profile = new SpeedProfile(track, grip, Tuning.TOP_SPEED);
        Driver tester = new Driver(0, "CAL", "Calibration car", new Team("Calibration", "#FFFFFF"));
        Car car = new Car(0, tester, 1, 1.0, 0.0, 0.0, Compound.MEDIUM, false);
        double lap = track.getLapLength();
        double dt = Tuning.SIM_STEP_SECONDS;
        double weighted = 0;
        double meters = 0;
        while (car.getDistance() < 2 * lap) {
            car.approachSpeed(profile.speedAt(car.getDistance()), grip, dt);
            double move = car.getSpeed() * dt;
            car.advance(move, dt);
            if (car.getDistance() >= lap) {
                double curvature = track.getSample(track.indexAt(car.getDistance())).getCurvature();
                double lateral = car.getSpeed() * car.getSpeed() * Math.abs(curvature);
                weighted += CarPhysics.load(lateral, car.getDeceleration()) * move;
                meters += move;
            }
        }
        return weighted / meters;
    }

    /**
     * Simulates a lone car with a flying start and returns its second lap time.
     * The first lap only brings the car up to racing speed. The car runs at the brief's reference
     * condition: Balanced push on fresh Medium tires in their temperature window, no ERS.
     *
     * @param track the track
     * @param gripScale grip multiplier to test
     * @return lap time in seconds
     */
    public static double cleanLapSeconds(Track track, double gripScale) {
        double grip = gripScale * Compound.MEDIUM.getDryGrip();
        SpeedProfile profile = new SpeedProfile(track, grip, Tuning.TOP_SPEED);
        Driver tester = new Driver(0, "CAL", "Calibration car", new Team("Calibration", "#FFFFFF"));
        Car car = new Car(0, tester, 1, 1.0, 0.0, 0.0, Compound.MEDIUM, false);
        double lap = track.getLapLength();
        double dt = Tuning.SIM_STEP_SECONDS;
        double time = 0;
        double lapStart = 0;
        boolean timing = false;
        while (true) {
            car.approachSpeed(profile.speedAt(car.getDistance()), grip, dt);
            double before = car.getDistance();
            double move = car.getSpeed() * dt;
            car.advance(move, dt);
            if (!timing && car.getDistance() >= lap) {
                lapStart = time + dt * (lap - before) / move;
                timing = true;
            } else if (timing && car.getDistance() >= 2 * lap) {
                return time + dt * (2 * lap - before) / move - lapStart;
            }
            time += dt;
        }
    }
}
