/**
 * The energy recovery system: a battery that charges under braking and can be spent for extra
 * speed on the straights. Deploying is a choice: spend it now to attack or defend, or save it.
 * When the battery runs empty the boost stops, which is why running out matters.
 */
public final class Ers {

    /** Charge from 0 (empty) to 1 (full). */
    private double charge;

    /** True if energy was spent during the last step. */
    private boolean deploying;

    /**
     * Creates a battery at its starting charge.
     */
    public Ers() {
        this.charge = Tuning.ERS_START_CHARGE;
    }

    /**
     * Charges from braking and, if wanted and possible, spends energy for this step.
     *
     * @param dt step length in seconds
     * @param deceleration braking deceleration in m/s squared
     * @param wantDeploy whether the driver wants to deploy
     * @param onStraight whether the car is on a straight, the only place deploying helps
     */
    public void update(double dt, double deceleration, boolean wantDeploy, boolean onStraight) {
        charge += Tuning.ERS_HARVEST_PER_G_SECOND * (deceleration / Tuning.GRAVITY) * dt;
        deploying = wantDeploy && onStraight && charge > 0;
        if (deploying) {
            charge -= Tuning.ERS_DEPLOY_PER_SECOND * dt;
        }
        charge = Math.max(0, Math.min(1, charge));
    }

    /**
     * Spends a lump of charge, for example on a move in a battle.
     *
     * @param amount charge to spend, from 0 to 1
     */
    public void spend(double amount) {
        charge = Math.max(0, charge - amount);
    }

    /**
     * Returns the speed multiplier from deploying.
     *
     * @return more than 1 while deploying, otherwise 1
     */
    public double boost() {
        return deploying ? 1.0 + Tuning.ERS_SPEED_BOOST : 1.0;
    }

    /**
     * Returns the charge.
     *
     * @return charge from 0 to 1
     */
    public double getCharge() {
        return charge;
    }

    /**
     * Returns whether energy was spent last step.
     *
     * @return true while deploying
     */
    public boolean isDeploying() {
        return deploying;
    }
}
