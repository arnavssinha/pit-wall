/**
 * The everyday decisions of a computer driver: how hard to push and when to spend the battery.
 * Battle stances are chosen by DefaultStancePolicy; pit stops arrive with the pit planner (M5).
 */
public final class AiDriver {

    /**
     * Not instantiated; all methods are static.
     */
    private AiDriver() {
    }

    /**
     * Chooses a push level: Push while fighting another car wheel to wheel, Conserve once the tires
     * reach the wear cliff so they last the stint, and Balanced otherwise.
     *
     * @param car the computer car
     * @return the push level
     */
    public static PushLevel choosePush(Car car) {
        if (car.getPassPartner() != null) {
            return PushLevel.PUSH;
        }
        return car.getTire().getWear() >= Tuning.WEAR_CLIFF ? PushLevel.CONSERVE : PushLevel.BALANCED;
    }

    /**
     * Decides whether to deploy battery energy on the straights: whenever there is some charge while
     * fighting wheel to wheel, otherwise only while comfortably charged so energy is left for battles.
     *
     * @param car the computer car
     * @return true to deploy
     */
    public static boolean wantsDeploy(Car car) {
        boolean fighting = car.getPassPartner() != null && car.isPassAttacker() && !car.isPassAborting();
        double needed = fighting ? Tuning.AI_FIGHT_ERS_ABOVE : Tuning.AI_ERS_DEPLOY_ABOVE;
        return car.getErs().getCharge() > needed;
    }

    /**
     * Decides whether to spend battery energy in a battle.
     *
     * @param car the computer car
     * @return true to use ERS in this battle
     */
    public static boolean usesErsInBattle(Car car) {
        return car.getErs().getCharge() > Tuning.AI_BATTLE_ERS_ABOVE;
    }
}
