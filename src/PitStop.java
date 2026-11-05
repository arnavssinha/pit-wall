/**
 * One planned pit stop: the lap to stop at and the tires to fit.
 * Stopping "at lap N" means driving into the pit lane at the end of lap N.
 */
public final class PitStop {

    /** The lap at the end of which the car stops. */
    private final int lap;

    /** The compound fitted at this stop. */
    private Compound compound;

    /**
     * Creates a planned stop.
     *
     * @param lap lap at the end of which to stop
     * @param compound tires to fit
     */
    public PitStop(int lap, Compound compound) {
        this.lap = lap;
        this.compound = compound;
    }

    /**
     * Returns the stop lap.
     *
     * @return the lap
     */
    public int getLap() {
        return lap;
    }

    /**
     * Returns the compound to fit.
     *
     * @return the compound
     */
    public Compound getCompound() {
        return compound;
    }

    /**
     * Changes the compound to fit.
     *
     * @param value the new compound
     */
    public void setCompound(Compound value) {
        compound = value;
    }
}
