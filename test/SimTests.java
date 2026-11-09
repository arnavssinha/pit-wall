/**
 * Checks the simulation without a window. Run with test.sh; it prints each result and exits with 1 if any fail.
 */
public final class SimTests {

    /** Number of failed checks. */
    private static int failures;

    /** Not used: all methods are static. */
    private SimTests() {
    }

    /** Runs every test. */
    public static void main(String[] args) throws Exception {
        Track track = Track.load("data/interlagos.csv");
        check("lap is about 4.3 km", track.length() > 4000 && track.length() < 4600);
        boolean sawRain = false;
        for (long seed = 1; seed <= 20; seed++) {
            Race race = new Race(track, seed);
            boolean rainTires = false;
            int steps = 0;
            while (!race.isOver() && steps < 60 * 60 * 30) {
                race.step();
                steps++;
                for (Car car : race.cars) {
                    rainTires |= car.tire.isRain();
                }
            }
            check("seed " + seed + ": all ten cars finish", race.isOver() && race.cars.size() == 10);
            double best = race.bestLap(race.standings().get(0));
            check("seed " + seed + ": winner's best lap is 70-80 s (" + Math.round(best) + ")", best > 70 && best < 80);
            if (rainTires) {
                sawRain = true;
            }
        }
        check("in some race it rains and cars fit rain tires", sawRain);
        testPitStop(track);
        testChance(track);
        testTires();
        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " TEST(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** The player boxes for Softs: after one stop the car has new Softs. */
    private static void testPitStop(Track track) {
        Race race = new Race(track, 7);
        race.player.boxFor = Compound.SOFT;
        for (int i = 0; i < 60 * 200 && race.player.stops == 0; i++) {
            race.step();
        }
        check("pit stop fits the chosen tires", race.player.stops == 1 && race.player.tire == Compound.SOFT);
        check("pit stop resets wear", race.player.wear < 0.05);
    }

    /** The overtake chance stays in range, the boost helps, and covering the line hurts the attacker. */
    private static void testChance(Track track) {
        Race race = new Race(track, 3);
        Fight fight = new Fight(race.cars.get(1), race.cars.get(0), 0);
        fight.attackLine = Fight.Line.INSIDE;
        fight.defendLine = Fight.Line.OUTSIDE;
        double open = fight.chance(0.5, false, 0);
        fight.boost = true;
        double boosted = fight.chance(0.5, false, 0);
        fight.defendLine = Fight.Line.INSIDE;
        double covered = fight.chance(0.5, false, 0);
        check("chance is between 5% and 90%", open >= 0.05 && open <= 0.9 && covered >= 0.05);
        check("boost raises the chance", boosted > open);
        check("covering the line lowers the chance", covered < boosted);
    }

    /** Rain tires win on a wet track, slicks on a dry one, and the computer picks the right ones. */
    private static void testTires() {
        check("Inters beat Softs when wet", Compound.INTER.grip(0.6) > Compound.SOFT.grip(0.6));
        check("Softs beat Inters when dry", Compound.SOFT.grip(0) > Compound.INTER.grip(0));
        Car car = new Car("TST", 0, false, 1);
        check("computer boxes for Inters on a wet track", Ai.pitChoice(car, 0.5, 5) == Compound.INTER);
        car.tire = Compound.INTER;
        check("computer boxes for slicks when it dries", Ai.pitChoice(car, 0.05, 5) != null
                && !Ai.pitChoice(car, 0.05, 5).isRain());
    }

    /** Prints one result. */
    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) {
            failures++;
        }
    }
}
