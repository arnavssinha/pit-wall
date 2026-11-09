import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.List;
import java.util.Locale;

/**
 * Everything drawn on top of the track: the timing tower, the car gauges, the radio, the fight
 * prompt, the start lights, the rain and the finishing order. Labels are made once and only their
 * text changes each frame.
 */
public final class Hud extends GCompound {

    /** Font for most text. */
    private static final String FONT = "Monospaced-bold-14";

    /** Panel background. */
    private static final Color PANEL = new Color(15, 15, 20, 210);

    /** The race shown. */
    private final Race race;

    /** Window size. */
    private final double width;

    /** Window height. */
    private final double height;

    /** Blue-grey layer over everything while it rains. */
    private final GRect rainLayer;

    /** Timing tower rows, one per position. */
    private final GLabel[] towerRows;

    /** Highlight behind the player's row in the tower. */
    private final GRect towerHighlight;

    /** Gauge lines at the bottom: lap and pace, tires, boost. */
    private final GLabel[] gauges = new GLabel[3];

    /** Radio messages, newest at the bottom. */
    private final GLabel[] radioLines = new GLabel[4];

    /** Prompt panel for fights and the start. */
    private final GRect promptBox;

    /** Prompt text lines. */
    private final GLabel[] promptLines = new GLabel[4];

    /** The three line choices A/S/D, in the colors of the dots on the track. */
    private final GLabel[] choices = new GLabel[3];

    /** The five start lights. */
    private final GOval[] lights = new GOval[5];

    /** Big centered message (pause, finish). */
    private final GLabel banner;

    /** Builds the HUD for a race and a window size. */
    public Hud(Race race, double width, double height) {
        this.race = race;
        this.width = width;
        this.height = height;
        rainLayer = rect(0, 0, width, height, new Color(60, 80, 110, 0));
        add(rect(10, 10, 210, 30 + 22 * race.cars.size(), PANEL));
        add(label("POS DRIVER     GAP  T", 20, 32, Color.LIGHT_GRAY));
        towerHighlight = rect(12, 0, 206, 22, new Color(255, 230, 0, 70));
        towerRows = new GLabel[race.cars.size()];
        for (int i = 0; i < towerRows.length; i++) {
            towerRows[i] = label("", 20, 56 + 22 * i, Color.WHITE);
        }
        add(rect(10, height - 108, width - 20, 98, PANEL));
        for (int i = 0; i < gauges.length; i++) {
            gauges[i] = label("", 24, height - 84 + 22 * i, Color.WHITE);
        }
        add(label("SHIFT Overtake  A/S/D Line  Q/W/E Pace  1-5 Box for tires  B Cancel box  P Pause  T Speed  R Restart",
                24, height - 18, new Color(0x9AA0A6)));
        add(rect(width - 480, 10, 470, 100, PANEL));
        for (int i = 0; i < radioLines.length; i++) {
            radioLines[i] = label("", width - 470, 32 + 22 * i, new Color(0x8FD3FF));
        }
        promptBox = rect(width / 2 - 260, height - 250, 520, 126, PANEL);
        for (int i = 0; i < promptLines.length; i++) {
            promptLines[i] = label("", width / 2 - 244, height - 222 + 26 * i, Color.WHITE);
        }
        String[] names = {"A Inside", "S Optimal", "D Outside"};
        for (int i = 0; i < choices.length; i++) {
            choices[i] = label(names[i], width / 2 - 244 + 160 * i, height - 196, TrackView.LINE_COLORS[i]);
        }
        for (int i = 0; i < lights.length; i++) {
            lights[i] = new GOval(width / 2 - 150 + i * 62, 90, 48, 48);
            lights[i].setFilled(true);
            add(lights[i]);
        }
        banner = label("", 0, height / 2 - 120, Color.WHITE);
        banner.setFont("SansSerif-bold-36");
    }

    /** Adds a filled rectangle. */
    private GRect rect(double x, double y, double w, double h, Color color) {
        GRect r = new GRect(x, y, w, h);
        r.setFilled(true);
        r.setFillColor(color);
        r.setColor(color);
        add(r);
        return r;
    }

    /** Adds a label. */
    private GLabel label(String text, double x, double y, Color color) {
        GLabel l = new GLabel(text, x, y);
        l.setFont(FONT);
        l.setColor(color);
        add(l);
        return l;
    }

    /** Updates everything from the race. lightsOn is 0-5 before the start, or -1 once racing. */
    public void update(int lightsOn, boolean paused, int timeSpeed) {
        rainLayer.setFillColor(new Color(60, 80, 110, (int) (110 * race.getRain())));
        updateTower();
        updateGauges(timeSpeed);
        int i = 0;
        for (String message : race.getRadio()) {
            radioLines[i++].setLabel(message);
        }
        updatePrompt(lightsOn);
        for (int k = 0; k < lights.length; k++) {
            lights[k].setVisible(lightsOn >= 0);
            lights[k].setFillColor(k < lightsOn ? Color.RED : new Color(0x330000));
        }
        Car p = race.player;
        String text = paused ? "PAUSED" : "";
        if (p.finished) {
            text = "FINISHED P" + (race.standings().indexOf(p) + 1) + "  -  R to race again";
        }
        banner.setLabel(text);
        banner.setLocation((width - banner.getWidth()) / 2, height / 2 - 120);
    }

    /** Fills the timing tower: position, driver, gap to the leader and tire letter. */
    private void updateTower() {
        List<Car> order = race.standings();
        Car leader = order.get(0);
        for (int i = 0; i < order.size(); i++) {
            Car car = order.get(i);
            String gap = i == 0 ? "LEADER" : car.finished ? String.format(Locale.ROOT, "+%.1f", car.finishTime - leader.finishTime)
                    : String.format(Locale.ROOT, "+%.1f", (leader.distance - car.distance) / 58);
            String pit = car.inPit ? " PIT" : "";
            towerRows[i].setLabel(String.format(Locale.ROOT, "%2d  %s%s %8s  %c", i + 1, car.code, pit.isEmpty() ? "    " : pit,
                    gap, car.tire.getLetter()));
            towerRows[i].setColor(new Color(car.color).brighter());
            if (car.player) {
                towerHighlight.setLocation(12, 40 + 22 * i);
            }
        }
    }

    /** Fills the three gauge lines about the player's car. */
    private void updateGauges(int timeSpeed) {
        Car p = race.player;
        int lap = Math.min(Race.LAPS, p.laps + 1);
        double bestLap = race.bestLap(p);
        String best = bestLap == 0 ? "--" : String.format(Locale.ROOT, "%.2f", bestLap);
        gauges[0].setLabel(String.format(Locale.ROOT, "LAP %d/%d   %3d km/h   Pace: %s   Best: %s   Time x%d",
                lap, Race.LAPS, Math.round(p.speed * 3.6), p.mode, best, timeSpeed));
        String box = p.boxFor == null ? "" : "   BOXING for " + p.boxFor.getName();
        gauges[1].setLabel(String.format(Locale.ROOT, "Tires: %s %d%% worn%s   Track: %s",
                p.tire.getName(), Math.round(p.wear * 100), box, race.getWetness() < 0.15 ? "dry" : race.getWetness() < 0.5 ? "damp" : "wet"));
        String status = p.boosting() ? "   OVERTAKE ON" : race.hasDrs(p) ? "   DRS OPEN" : "";
        gauges[2].setLabel(String.format(Locale.ROOT, "Boost: %s %d%%%s", bar(p.boost), Math.round(p.boost * 100), status));
    }

    /** Returns a ten-block text bar for a value from 0 to 1. */
    private static String bar(double value) {
        int full = (int) Math.round(value * 10);
        return "[" + "#".repeat(full) + ".".repeat(10 - full) + "]";
    }

    /** Shows the start or fight prompt, or hides it. */
    private void updatePrompt(int lightsOn) {
        Fight fight = race.getPlayerFight();
        String[] text = {"", "", "", ""};
        if (lightsOn >= 0) {
            text[0] = "START: press SHIFT when the lights go out!";
            text[1] = "Too early and you lose time.";
        } else if (fight != null && fight.attacker == race.player) {
            text[0] = "ATTACK " + fight.defender.code + "  -  braking in "
                    + Math.round(fight.decideAt - race.player.distance) + " m";
            text[2] = fight.attackLine == null ? "Pick a line to attack, or stay behind."
                    : String.format(Locale.ROOT, "Chance: %d%%", Math.round(100 * fight.chance(
                    race.gapSeconds(race.player, fight.defender), race.hasDrs(race.player), race.getWetness())));
            text[3] = fight.boost ? "Overtake boost ON" : "SHIFT: Overtake boost (" + Math.round(race.player.boost * 100) + "%)";
        } else if (fight != null) {
            text[0] = "DEFEND from " + fight.attacker.code + "  -  cover a line!";
            text[2] = "If you cover the line they pick, they will probably fail.";
        }
        boolean show = !text[0].isEmpty();
        promptBox.setVisible(show);
        Fight.Line chosen = fight == null ? null : fight.attacker == race.player ? fight.attackLine : fight.defendLine;
        for (int i = 0; i < choices.length; i++) {
            choices[i].setVisible(fight != null && lightsOn < 0);
            choices[i].setFont(chosen == Fight.Line.values()[i] ? "Monospaced-bold-18" : FONT);
        }
        for (int i = 0; i < promptLines.length; i++) {
            promptLines[i].setLabel(text[i]);
        }
    }
}
