/**
 * The everyday decisions of a computer driver: how hard to push and when to spend the battery.
 * Fight lines are chosen by LineChoicePolicy; pit stops come from PitStrategyPlanner.
 * Every driver has its own battery style and each straight is a fresh decision, so the field does
 * not all deploy at the same places.
 */
public final class AiDriver {

    /**
     * Not instantiated; all methods are static.
     */
    private AiDriver() {
    }

    /**
     * Chooses a push level: Push while fighting another car, Conserve once the tires reach the wear
     * cliff so they last the stint, and otherwise mostly Balanced with spells of Push and Conserve that
     * change every couple of kilometers, so drivers' pace varies through the race.
     *
     * @param car the computer car
     * @return the push level
     */
    public static PushLevel choosePush(Car car) {
        if (isFighting(car)) {
            return PushLevel.PUSH;
        }
        if (car.getTire().getWear() >= Tuning.WEAR_CLIFF) {
            return PushLevel.CONSERVE;
        }
        int stretch = (int) (car.getDistance() / Tuning.AI_PUSH_STRETCH_METERS);
        long mix = car.getId() * 2654435761L ^ stretch * 40503L;
        double roll = Math.floorMod(mix, 1000) / 1000.0;
        if (roll < Tuning.AI_RANDOM_PUSH_SHARE) {
            return PushLevel.PUSH;
        }
        if (roll < Tuning.AI_RANDOM_PUSH_SHARE + Tuning.AI_RANDOM_CONSERVE_SHARE) {
            return PushLevel.CONSERVE;
        }
        return PushLevel.BALANCED;
    }

    /**
     * Decides whether to deploy battery energy on the straights. The player's car only uses charge
     * above a reserve, so the Overtake boost stays available. A computer driver in a fight spends whatever
     * it has. Otherwise each driver, on each 500 m stretch of each lap, decides afresh: drivers with a
     * free-spending style deploy more often and down to a lower charge.
     *
     * @param car the computer car
     * @param lapDistance where the car is on the lap, in meters
     * @return true to deploy
     */
    public static boolean wantsDeploy(Car car, double lapDistance) {
        double charge = car.getErs().getCharge();
        if (car.isPlayer()) {
            return charge > Tuning.PLAYER_ERS_RESERVE + Tuning.OVERTAKE_COST;
        }
        if (isFighting(car)) {
            return charge > Tuning.AI_FIGHT_ERS_ABOVE;
        }
        double needed = Tuning.AI_ERS_DEPLOY_ABOVE - Tuning.AI_ERS_STYLE_RANGE * (car.getErsStyle() - 0.5);
        int stretch = (int) (lapDistance / Tuning.AI_ERS_STRETCH_METERS);
        long mix = car.getId() * 73856093L ^ car.getLapsCompleted() * 19349663L ^ stretch * 83492791L;
        double roll = Math.floorMod(mix, 1000) / 1000.0;
        return charge > needed && roll < Tuning.AI_ERS_USE_SHARE + Tuning.AI_ERS_STYLE_RANGE * (car.getErsStyle() - 0.5);
    }

    /**
     * Decides whether to spend battery on a fight.
     *
     * @param car the computer car
     * @return true if it has charge to spare
     */
    public static boolean usesErsInBattle(Car car) {
        return car.getErs().getCharge() > Tuning.AI_BATTLE_ERS_ABOVE;
    }

    /**
     * Returns whether the car is in a fight: it has a fight line for the coming corner (attacking or
     * defending) other than staying behind. A start line into the first corner is not a fight.
     *
     * @param car the car
     * @return true while fighting
     */
    public static boolean isFighting(Car car) {
        LinePlan plan = car.getLinePlan();
        return plan != null && plan.getChoice() != LineChoice.STAY_BEHIND && !plan.isStart();
    }
}
