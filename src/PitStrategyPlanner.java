import java.util.Random;

/**
 * Pit strategy for computer drivers, and the automatic calls on the player's own plan.
 * At the start each computer driver gets one of the three strategy presets. During the race the
 * planner calls each planned stop on its lap, never stops on the last lap, and calls an early
 * emergency stop when tires are about to run out. The player's planned stops are called the same
 * way (with a radio message), and the player can cancel or move them.
 */
public final class PitStrategyPlanner {

    /**
     * Not instantiated; all methods are static.
     */
    private PitStrategyPlanner() {
    }

    /**
     * Picks a strategy for a computer driver: one-stop early, one-stop late or two-stop, using the
     * shares in Tuning.
     *
     * @param random the race's random generator
     * @return a fresh plan
     */
    public static PitPlan choosePlan(Random random) {
        double roll = random.nextDouble();
        if (roll < Tuning.AI_SHARE_ONE_STOP_EARLY) {
            return StrategyPreset.ONE_STOP_EARLY.makePlan();
        }
        if (roll < Tuning.AI_SHARE_ONE_STOP_EARLY + Tuning.AI_SHARE_ONE_STOP_LATE) {
            return StrategyPreset.ONE_STOP_LATE.makePlan();
        }
        return StrategyPreset.TWO_STOP.makePlan();
    }

    /**
     * Calls a stop for a car when its plan says so, or early when its tires are nearly gone.
     * Stops planned for the last lap or later are dropped, because the race ends first.
     *
     * @param car the car
     * @param race the race
     */
    public static void callStops(Car car, Race race) {
        if (car.isInPitLane() || car.getPitRequest() > 0) {
            return;
        }
        int raceLaps = race.getConfig().getLaps();
        int currentLap = car.getLapsCompleted() + 1;
        if (!car.isPlayer() && currentLap < raceLaps && weatherCall(car, race.getConditions().averageWetness())) {
            return;
        }
        PitPlan plan = car.getPlan();
        PitStop next = plan.nextStop();
        while (next != null && next.getLap() >= raceLaps) {
            plan.removeNextStop();
            next = plan.nextStop();
        }
        if (currentLap >= raceLaps) {
            return;
        }
        if (next != null && currentLap >= next.getLap()) {
            car.requestPit(1, next.getCompound());
            if (car.isPlayer()) {
                race.addMessage("Box this lap for " + next.getCompound().getDisplayName()
                        + "s, as planned. B to cancel.", true);
            }
            return;
        }
        if (!car.isPlayer() && car.getTire().getWear() >= Tuning.AI_EMERGENCY_WEAR) {
            Compound fresh = next != null ? next.getCompound() : otherDryCompound(car.getTire().getCompound());
            car.requestPit(1, fresh);
        }
    }

    /**
     * Calls a stop for a computer driver whose tires do not suit the track's wetness: slicks on a
     * wet track, Intermediates in heavy rain, or rain tires on a drying track.
     *
     * @param car the computer car
     * @param wetness average track wetness
     * @return true if a stop was called
     */
    private static boolean weatherCall(Car car, double wetness) {
        Compound now = car.getTire().getCompound();
        Compound wanted = null;
        if (now.isDry() && wetness > Tuning.SLICK_WET_CLIFF) {
            wanted = wetness > Tuning.AI_WET_WETNESS ? Compound.WET : Compound.INTERMEDIATE;
        } else if (now == Compound.INTERMEDIATE && wetness > Tuning.AI_WET_WETNESS) {
            wanted = Compound.WET;
        } else if (!now.isDry() && wetness < Tuning.AI_DRY_WETNESS) {
            wanted = Compound.MEDIUM;
        }
        if (wanted == null) {
            return false;
        }
        car.requestPit(1, wanted);
        return true;
    }

    /**
     * Picks a different dry compound, so an unplanned stop still satisfies the two-compound rule.
     *
     * @param current the compound now fitted
     * @return Medium, or Hard if the current tire is a Medium
     */
    private static Compound otherDryCompound(Compound current) {
        return current == Compound.MEDIUM ? Compound.HARD : Compound.MEDIUM;
    }
}
