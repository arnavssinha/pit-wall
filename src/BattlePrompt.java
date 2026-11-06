import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

/**
 * The one panel for every decision the player makes on track, kept short and in the same place:
 * <ul>
 * <li>Attack: a car ahead is in range. One big SHIFT OVERTAKE button (battery boost, DRS on a DRS
 * straight, late braking). After pressing it, the three lines appear: A Inside, S Optimal, D Outside.</li>
 * <li>Defend: a car behind is attacking. The three lines appear at once: A Cover inside, S Optimal,
 * D Cover outside.</li>
 * <li>Start: the five red lights, SHIFT to launch when they go out, then the line into Turn 1.</li>
 * </ul>
 * Each line button has the color of its line drawn on the track. Three short facts sit under the
 * buttons (speed, grip, battery and DRS), green when they favor the player and red when not.
 */
public final class BattlePrompt extends GCompound {

    /** Serialization id (GCompound is serializable). */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 600;

    /** Panel height in pixels. */
    public static final double HEIGHT = 150;

    /** Width of the countdown bar. */
    private static final double BAR_WIDTH = WIDTH - 24;

    /** Width of a line button. */
    private static final double BUTTON_W = 186;

    /** Number of start lights. */
    public static final int LIGHTS = 5;

    /** Speed difference (km/h) below which speed counts as even. */
    private static final double EVEN_SPEED = 3;

    /** Grip difference below which grip counts as even. */
    private static final double EVEN_GRIP = 0.01;

    /** The line choices in button order. */
    private static final LineChoice[] ORDER = {LineChoice.INSIDE, LineChoice.OPTIMAL, LineChoice.OUTSIDE};

    /** Keys in button order. */
    private static final String[] KEYS = {"A", "S", "D"};

    /** Frame, colored by the kind of decision. */
    private final GRect frame;

    /** Title line. */
    private final GLabel headline;

    /** The three line buttons. */
    private final UiButton[] lines = new UiButton[3];

    /** The Overtake button. */
    private final UiButton overtake;

    /** What the Overtake button does. */
    private final GLabel overtakeHint;

    /** The start lights. */
    private final GRect[] lights = new GRect[LIGHTS];

    /** The three facts under the buttons. */
    private final GLabel[] facts = new GLabel[3];

    /** Countdown bar for the decision. */
    private final GRect countdown;

    /** Bottom line: what happens next, or the result. */
    private final GLabel footer;

    /**
     * Builds the panel.
     */
    public BattlePrompt() {
        frame = new GRect(0, 0, WIDTH, HEIGHT);
        frame.setFilled(true);
        frame.setFillColor(Palette.PANEL);
        frame.setColor(Palette.DANGER);
        add(frame);
        headline = text(12, 26, Palette.HEADLINE.deriveFont(20f), Palette.TEXT);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = new UiButton("", BUTTON_W, 34);
            lines[i].setAccent(Palette.lineColor(ORDER[i]));
            add(lines[i], 12 + i * (BUTTON_W + 7), 38);
        }
        overtake = new UiButton("SHIFT   OVERTAKE", 250, 34);
        overtake.setAccent(Palette.PLAYER);
        add(overtake, 12, 38);
        overtakeHint = text(274, 60, Palette.SMALL, Palette.TEXT_DIM);
        for (int i = 0; i < LIGHTS; i++) {
            lights[i] = new GRect(WIDTH - 12 - (LIGHTS - i) * 34, 8, 28, 22);
            lights[i].setFilled(true);
            add(lights[i]);
        }
        for (int i = 0; i < facts.length; i++) {
            facts[i] = text(12 + i * 196, 96, Palette.HUD, Palette.TEXT);
        }
        countdown = new GRect(12, HEIGHT - 32, BAR_WIDTH, 4);
        countdown.setFilled(true);
        add(countdown);
        footer = text(12, HEIGHT - 10, Palette.HUD, Palette.PLAYER);
    }

    /**
     * Adds a label.
     *
     * @param x left edge
     * @param y baseline
     * @param font font
     * @param color color
     * @return the label
     */
    private GLabel text(double x, double y, Font font, Color color) {
        GLabel l = new GLabel("", x, y);
        l.setFont(font);
        l.setColor(color);
        add(l);
        return l;
    }

    /**
     * Shows an open fight.
     *
     * @param battle the player's battle
     * @param race the race
     * @param secondsLeft real seconds left to choose
     */
    public void showOpen(Battle battle, Race race, double secondsLeft) {
        Car player = race.getPlayerCar();
        Car rival = battle.getRival();
        boolean attacking = battle.isPlayerAttacking();
        String code = rival.getDriver().getCode();
        double gap = race.trackGap(battle.getAttacker(), battle.getDefender()) / Math.max(1, battle.getAttacker().getSpeed());
        boolean ahead = race.trackGap(battle.getAttacker(), battle.getDefender()) > race.getTrack().getLapLength() / 2;
        setFrame(attacking ? Palette.DANGER : Palette.LINE_INSIDE);
        showLights(-1);
        LineChoice chosen = battle.getPlayerLine();
        boolean choosingLine = !attacking || battle.isOvertake();
        if (attacking && !battle.isOvertake()) {
            setText(headline, String.format(Locale.ROOT, "ATTACK   %s  %.2f s ahead   Turn %d", code, ahead ? 0 : gap,
                    battle.getCorner().getNumber()));
        } else if (attacking) {
            setText(headline, "OVERTAKE ON   vs " + code + "   choose your line");
        } else {
            LineChoice their = battle.getAttackerLine();
            String how = their == null || their == LineChoice.STAY_BEHIND ? "" : "  (going " + their.getDisplayName().toLowerCase(Locale.ROOT) + ")";
            setText(headline, "DEFEND   " + code + " is attacking" + how);
        }
        String[] names = attacking ? new String[] {"Inside", "Optimal", "Outside"}
                : new String[] {"Cover inside", "Optimal", "Cover outside"};
        for (int i = 0; i < lines.length; i++) {
            show(lines[i], choosingLine);
            lines[i].setLabel(KEYS[i] + "   " + names[i]);
            lines[i].setHighlighted(chosen == ORDER[i]);
        }
        show(overtake, attacking && !battle.isOvertake());
        boolean canBoost = player.getErs().getCharge() >= Tuning.OVERTAKE_COST;
        overtake.setEnabled(canBoost);
        show(overtakeHint, attacking && !battle.isOvertake());
        setText(overtakeHint, canBoost ? "boost + DRS + late braking   (or A / S / D without boost)"
                : String.format(Locale.ROOT, "needs %.0f%% boost   (A / S / D: line without boost)", Tuning.OVERTAKE_COST * 100));
        showFacts(player, rival);
        double fraction = Math.max(0, Math.min(1, secondsLeft / Tuning.BATTLE_DECISION_SECONDS));
        countdown.setSize(Math.max(1, BAR_WIDTH * (battle.isPlayerDecided() ? 0 : fraction)), 4);
        if (battle.isPlayerDecided() && chosen != null) {
            setText(footer, "Line: " + names[indexOf(chosen)] + (battle.isOvertake() ? "   with Overtake" : "")
                    + (battle.isPlayerLocked() ? "   (committed)" : "   (change with A / S / D)"));
        } else {
            LineChoice fallback = LineChoicePolicy.playerDefault(attacking, battle.isOvertake());
            String next = fallback == LineChoice.STAY_BEHIND ? "you follow" : names[indexOf(fallback)];
            setText(footer, String.format(Locale.ROOT, "%.1f s to choose, otherwise: %s", Math.max(0, secondsLeft), next));
        }
    }

    /**
     * Shows the start: the lights, then the launch and the line into the first corner.
     *
     * @param race the race
     * @param lightsOn how many red lights are on (0 to 5), or -1 once they have gone out
     * @param launched true once the player's car has launched
     * @param line the player's line into the first corner
     */
    public void showStart(Race race, int lightsOn, boolean launched, LineChoice line) {
        setFrame(Palette.PLAYER);
        showLights(lightsOn);
        Corner first = race.getStartCorner();
        int turn = first == null ? 1 : first.getNumber();
        Car player = race.getPlayerCar();
        if (lightsOn >= 0) {
            setText(headline, "START   lights...");
        } else {
            setText(headline, launched ? "START   into Turn " + turn : "START   GO!  Press SHIFT to launch");
        }
        String[] names = {"Inside", "Optimal", "Outside"};
        for (int i = 0; i < lines.length; i++) {
            show(lines[i], true);
            lines[i].setLabel(KEYS[i] + "   " + names[i]);
            lines[i].setHighlighted(line == ORDER[i]);
        }
        show(overtake, false);
        show(overtakeHint, false);
        showFacts(player, null);
        countdown.setSize(1, 4);
        if (lightsOn >= 0) {
            setText(footer, "Press SHIFT the moment the lights go out. Too early is a jump start.");
        } else if (!launched) {
            setText(footer, "SHIFT now!");
        } else if (player.isOvertakeActive()) {
            setText(footer, "Overtake on: boost and late braking into Turn " + turn + ".  A / S / D: line");
        } else {
            setText(footer, "A / S / D: line into Turn " + turn + ".   SHIFT: Overtake (boost + late braking)");
        }
    }

    /**
     * Shows the three facts: straight-line speed, grip, and battery with DRS.
     *
     * @param player the player's car
     * @param rival the other car, or null (start)
     */
    private void showFacts(Car player, Car rival) {
        if (rival == null) {
            fact(0, String.format(Locale.ROOT, "Boost %.0f%%", player.getErs().getCharge() * 100),
                    player.getErs().getCharge() >= Tuning.OVERTAKE_COST ? Palette.GOOD : Palette.TEXT_DIM);
            fact(1, "Grip " + Math.round(player.getCurrentGrip() * 100) + "%", Palette.TEXT);
            fact(2, "Tires: " + player.getTire().getCompound().getDisplayName(), Palette.compoundColor(player.getTire().getCompound()));
            return;
        }
        double speedDiff = (player.getTopSpeed() - rival.getTopSpeed()) * 3.6;
        String speed = Math.abs(speedDiff) < 1 ? "Speed even" : String.format(Locale.ROOT, "Speed %+.0f km/h", speedDiff);
        fact(0, speed, tone(speedDiff, EVEN_SPEED));
        double gripDiff = player.getCurrentGrip() - rival.getCurrentGrip();
        fact(1, String.format(Locale.ROOT, "Grip %d%% vs %d%%", Math.round(player.getCurrentGrip() * 100),
                Math.round(rival.getCurrentGrip() * 100)), tone(gripDiff, EVEN_GRIP));
        String drs = player.isDrsOpen() ? "   DRS open" : "";
        boolean ready = player.getErs().getCharge() >= Tuning.OVERTAKE_COST;
        fact(2, String.format(Locale.ROOT, "Boost %.0f%%%s", player.getErs().getCharge() * 100, drs),
                player.isDrsOpen() ? Palette.DRS : ready ? Palette.GOOD : Palette.TEXT_DIM);
    }

    /**
     * Sets one fact.
     *
     * @param index which fact
     * @param text the text
     * @param color the color
     */
    private void fact(int index, String text, Color color) {
        setText(facts[index], text);
        if (!facts[index].getColor().equals(color)) {
            facts[index].setColor(color);
        }
    }

    /**
     * Lights the start lights: the first ones red, the rest dark; all hidden once they are out.
     *
     * @param lightsOn number of lights on, or -1 to hide them
     */
    private void showLights(int lightsOn) {
        for (int i = 0; i < LIGHTS; i++) {
            show(lights[i], lightsOn >= 0);
            Color c = i < lightsOn ? Palette.DANGER : Palette.GAUGE_BACK;
            if (!lights[i].getFillColor().equals(c)) {
                lights[i].setFillColor(c);
                lights[i].setColor(c);
            }
        }
    }

    /**
     * Shows a finished fight's result.
     *
     * @param battle the battle
     */
    public void showResult(Battle battle) {
        countdown.setSize(1, 4);
        for (UiButton b : lines) {
            show(b, false);
        }
        show(overtake, false);
        show(overtakeHint, false);
        setText(footer, battle.getMessage());
    }

    /**
     * Returns the line under a click, if any.
     *
     * @param x x inside the panel
     * @param y y inside the panel
     * @return the choice, or null
     */
    public LineChoice choiceAt(double x, double y) {
        GObject hit = getElementAt(x, y);
        for (int i = 0; i < lines.length; i++) {
            if (hit != null && (hit == lines[i] || hit.getParent() == lines[i]) && lines[i].isVisible()) {
                return ORDER[i];
            }
        }
        return null;
    }

    /**
     * Returns whether a click hit the Overtake button.
     *
     * @param x x inside the panel
     * @param y y inside the panel
     * @return true on the button
     */
    public boolean isOvertakeAt(double x, double y) {
        GObject hit = getElementAt(x, y);
        return overtake.isVisible() && hit != null && (hit == overtake || hit.getParent() == overtake);
    }

    /**
     * Returns the button position of a line.
     *
     * @param line the line
     * @return 0, 1 or 2
     */
    private static int indexOf(LineChoice line) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == line) {
                return i;
            }
        }
        return 1;
    }

    /**
     * Picks green, red or neutral for a difference that favors the player when positive.
     *
     * @param difference the player's value minus the rival's
     * @param even differences smaller than this count as even
     * @return the color
     */
    private static Color tone(double difference, double even) {
        if (difference > even) {
            return Palette.GOOD;
        }
        if (difference < -even) {
            return Palette.DANGER;
        }
        return Palette.TEXT;
    }

    /**
     * Sets the frame color.
     *
     * @param color the color
     */
    private void setFrame(Color color) {
        if (!frame.getColor().equals(color)) {
            frame.setColor(color);
            countdown.setFillColor(color);
            countdown.setColor(color);
        }
    }

    /**
     * Shows or hides an object only if that changes it.
     *
     * @param object the object
     * @param visible true to show
     */
    private static void show(GObject object, boolean visible) {
        if (object.isVisible() != visible) {
            object.setVisible(visible);
        }
    }

    /**
     * Replaces a label's text only if it changed.
     *
     * @param label the label
     * @param text the new text
     */
    private static void setText(GLabel label, String text) {
        if (!text.equals(label.getLabel())) {
            label.setLabel(text);
        }
    }
}
