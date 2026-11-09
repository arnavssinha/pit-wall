/**
 * One overtaking attempt: the car behind (attacker) tries to pass the car ahead (defender) at the next
 * braking point. Each side picks a line; the pass then succeeds or fails by chance.
 */
public final class Fight {

    /** The three lines into a corner. */
    public enum Line {

        /** The inside of the corner: shortest way in. */
        INSIDE,

        /** The normal racing line. */
        OPTIMAL,

        /** The outside: the long way round. */
        OUTSIDE
    }

    /** The car behind. */
    public final Car attacker;

    /** The car ahead. */
    public final Car defender;

    /** Attacker distance at which the fight is decided (the braking point). */
    public final double decideAt;

    /** The attacker's line, or null if it is not attacking (just following). */
    public Line attackLine;

    /** The line the defender covers. */
    public Line defendLine = Line.OPTIMAL;

    /** True if the attacker uses the Overtake boost. */
    public boolean boost;

    /** Opens a fight. */
    public Fight(Car attacker, Car defender, double decideAt) {
        this.attacker = attacker;
        this.defender = defender;
        this.decideAt = decideAt;
    }

    /**
     * Works out the chance that the pass works. Every term is something the player can see: pace, tire grip,
     * the boost, DRS, the gap, and whether the defender covered the chosen line.
     */
    public double chance(double gapSeconds, boolean drs, double wetness) {
        double chance = 0.35;
        chance += 8 * (attacker.pace * attacker.mode.pace - defender.pace * defender.mode.pace);
        chance += 3 * (attacker.grip(wetness) - defender.grip(wetness));
        chance += boost ? 0.2 : 0;
        chance += drs ? 0.1 : 0;
        chance -= 0.3 * gapSeconds;
        if (attackLine == defendLine) {
            chance -= 0.3;
        }
        if (attackLine == Line.OUTSIDE) {
            chance -= 0.05;
        } else if (attackLine == Line.OPTIMAL) {
            chance -= 0.1;
        }
        return Math.max(0.05, Math.min(0.9, chance));
    }
}
