import java.util.Random;

/** The decisions of the computer drivers: pace mode, pit stops, attacking and defending. */
public final class Ai {

    /** Not used: all methods are static. */
    private Ai() {
    }

    /** Picks a pace mode now and then: mostly Normal, sometimes Push or Conserve, Conserve on worn tires. */
    public static void chooseMode(Car car, Random random) {
        if (car.wear > 0.7) {
            car.mode = Car.Mode.CONSERVE;
            return;
        }
        double roll = random.nextDouble();
        car.mode = roll < 0.2 ? Car.Mode.PUSH : roll < 0.35 ? Car.Mode.CONSERVE : Car.Mode.NORMAL;
    }

    /**
     * Decides whether to box this lap and for which tires: rain tires when the track is wet, slicks when it
     * dries, fresh tires when they are worn and there is enough race left.
     */
    public static Compound pitChoice(Car car, double wetness, int lapsLeft) {
        if (wetness > 0.35 && !car.tire.isRain()) {
            return wetness > 0.75 ? Compound.WET : Compound.INTER;
        }
        if (wetness < 0.15 && car.tire.isRain()) {
            return lapsLeft > 4 ? Compound.MEDIUM : Compound.SOFT;
        }
        if (car.wear > 0.75 && lapsLeft > 2) {
            return lapsLeft > 5 ? Compound.HARD : Compound.SOFT;
        }
        return null;
    }

    /** Decides whether a computer attacker goes for it, on which line and with boost. */
    public static void chooseAttack(Fight fight, Random random) {
        if (random.nextDouble() < 0.6) {
            double roll = random.nextDouble();
            fight.attackLine = roll < 0.6 ? Fight.Line.INSIDE : roll < 0.8 ? Fight.Line.OUTSIDE : Fight.Line.OPTIMAL;
            fight.boost = fight.attacker.boost >= Race.BOOST_COST && random.nextDouble() < 0.6;
        }
    }

    /** Decides which line a computer defender covers: half the time it reads the attack correctly. */
    public static void chooseDefence(Fight fight, Random random) {
        boolean readsIt = fight.attackLine != null && random.nextDouble() < 0.5;
        fight.defendLine = readsIt ? fight.attackLine : Fight.Line.OPTIMAL;
    }
}
