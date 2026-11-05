/**
 * Every tunable number in the simulation, in one place.
 * Each constant says where its value came from: a physical fact, a rule of the
 * sport, or a balance choice made so the game plays well. Keeping them here means
 * the rest of the code never hides a magic number.
 */
public final class Tuning {

    /** Number of laps in every race. The brief fixes the sprint at 10 laps. */
    public static final int RACE_LAPS = 10;

    /**
     * Creates nothing; this class only holds constants.
     */
    private Tuning() {
    }
}
