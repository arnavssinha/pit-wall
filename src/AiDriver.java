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
     * Chooses a push level: Balanced normally, Conserve once the tires reach the wear cliff
     * so they can last to the end of the stint.
     *
     * @param car the computer car
     * @return the push level
     */
    public static PushLevel choosePush(Car car) {
        return car.getTire().getWear() >= Tuning.WEAR_CLIFF ? PushLevel.CONSERVE : PushLevel.BALANCED;
    }

    /**
     * Decides whether to deploy battery energy on the straights: only while comfortably charged,
     * so there is energy left for battles.
     *
     * @param car the computer car
     * @return true to deploy
     */
    public static boolean wantsDeploy(Car car) {
        return car.getErs().getCharge() > Tuning.AI_ERS_DEPLOY_ABOVE;
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
