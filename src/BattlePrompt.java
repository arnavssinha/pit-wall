import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

/**
 * The panel shown while the player is in a fight. Attacking and defending have their own layouts:
 * an attacker chooses Inside, Optimal (sit in the tow, better exit), Outside or Stay behind; a defender
 * chooses Cover inside, Optimal or Cover outside. Each button carries the color of its line drawn on
 * the track (LinePreview), and L switches late braking on or off. Below the buttons the panel lists
 * the factors that decide the fight (gap, straight-line speed and why, grip and tires, battery, the
 * corner, lock-up risk, the rival's line), each colored green when it favors the player and red when
 * it favors the rival. It never shows a success percentage: the player reads the factors.
 */
public final class BattlePrompt extends GCompound {

    /** Serialization id (GCompound is serializable). */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 600;

    /** Panel height in pixels. */
    public static final double HEIGHT = 232;

    /** Width of the countdown bar. */
    private static final double BAR_WIDTH = WIDTH - 24;

    /** Button width. */
    private static final double BUTTON_W = 140;

    /** Number of factor rows. */
    private static final int FACTORS = 6;

    /** Top of the factor rows. */
    private static final double FACTOR_TOP = 128;

    /** Space between factor rows. */
    private static final double FACTOR_STEP = 15;

    /** Speed difference (km/h) below which the straight-line speed counts as even. */
    private static final double EVEN_SPEED = 3;

    /** Grip difference below which grip counts as even. */
    private static final double EVEN_GRIP = 0.01;

    /** Colored frame (red when attacking, blue when defending). */
    private final GRect frame;

    /** Title line. */
    private final GLabel headline;

    /** Line under the title: what kind of corner it is. */
    private final GLabel cornerLine;

    /** The four choice buttons (Inside, Optimal, Outside, Stay behind). */
    private final UiButton[] choices = new UiButton[4];

    /** Late braking switch. */
    private final UiButton late;

    /** The factor rows. */
    private final GLabel[] factors = new GLabel[FACTORS];

    /** Countdown bar for the decision. */
    private final GRect countdown;

    /** Bottom line: time left, the choice, or the result. */
    private final GLabel footer;

    /** The choices in button order. */
    private static final LineChoice[] ORDER = {LineChoice.INSIDE, LineChoice.OPTIMAL, LineChoice.OUTSIDE, LineChoice.STAY_BEHIND};

    /** Keys in button order. */
    private static final String[] KEYS = {"A", "S", "O", "D"};

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
        cornerLine = text(12, 46, Palette.SMALL, Palette.TEXT_DIM);
        for (int i = 0; i < choices.length; i++) {
            choices[i] = new UiButton("", BUTTON_W, 32);
            choices[i].setAccent(Palette.lineColor(ORDER[i]));
            add(choices[i], 12 + i * (BUTTON_W + 6), 56);
        }
        late = new UiButton("L  Late braking: off", 220, 26);
        add(late, 12, 94);
        GLabel hint = text(244, 112, Palette.SMALL, Palette.TEXT_DIM);
        hint.setLabel("Colored lines on the track = your choices. Red dashes = rival.");
        for (int i = 0; i < FACTORS; i++) {
            factors[i] = text(12, FACTOR_TOP + i * FACTOR_STEP, Palette.SMALL, Palette.TEXT);
        }
        countdown = new GRect(12, HEIGHT - 26, BAR_WIDTH, 5);
        countdown.setFilled(true);
        countdown.setFillColor(Palette.DANGER);
        countdown.setColor(Palette.DANGER);
        add(countdown);
        footer = text(12, HEIGHT - 7, Palette.HUD, Palette.PLAYER);
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
     * Shows an open battle: the choices, the factors and the time left.
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
        Color roleColor = attacking ? Palette.DANGER : Palette.LINE_INSIDE;
        if (!frame.getColor().equals(roleColor)) {
            frame.setColor(roleColor);
            countdown.setFillColor(roleColor);
            countdown.setColor(roleColor);
        }
        setText(headline, String.format(Locale.ROOT, "%s  %s  TURN %d", attacking ? "ATTACK" : "DEFEND",
                attacking ? "on " + code : "against " + code, battle.getCorner().getNumber()));
        setText(cornerLine, cornerText(battle, race));
        String[] names = attacking
                ? new String[] {"Inside", "Optimal", "Outside", "Stay behind"}
                : new String[] {"Cover inside", "Optimal", "Cover outside", ""};
        LineChoice chosen = battle.getPlayerLine();
        for (int i = 0; i < choices.length; i++) {
            boolean usable = attacking || ORDER[i] != LineChoice.STAY_BEHIND;
            choices[i].setVisible(usable);
            choices[i].setLabel(usable ? KEYS[i] + "  " + names[i] : "");
            choices[i].setHighlighted(usable && chosen == ORDER[i]);
        }
        late.setLabel("L  Late braking: " + (battle.isPlayerLate() ? "ON" : "off"));
        late.setHighlighted(battle.isPlayerLate());
        showFactors(battle, race, player, rival, attacking);
        double fraction = Math.max(0, Math.min(1, secondsLeft / Tuning.BATTLE_DECISION_SECONDS));
        countdown.setSize(Math.max(1, BAR_WIDTH * (battle.isPlayerDecided() ? 0 : fraction)), 5);
        if (battle.isPlayerLocked() && chosen != null) {
            setText(footer, "Committed: " + describe(chosen, attacking) + (battle.isPlayerLate() ? ", late braking" : ""));
        } else if (battle.isPlayerDecided() && chosen != null) {
            setText(footer, "Line: " + describe(chosen, attacking) + (battle.isPlayerLate() ? ", late braking" : "")
                    + "   (you can still change it)");
        } else {
            LineChoice fallback = LineChoicePolicy.playerDefault(player, battle.getGapSeconds(), attacking);
            setText(footer, String.format(Locale.ROOT, "Choose in %.1f s, or it will be: %s",
                    Math.max(0, secondsLeft), describe(fallback, attacking)));
        }
    }

    /**
     * Fills in the factor rows, colored by who they favor.
     *
     * @param battle the battle
     * @param race the race
     * @param player the player's car
     * @param rival the other car
     * @param attacking true if the player is behind
     */
    private void showFactors(Battle battle, Race race, Car player, Car rival, boolean attacking) {
        String code = rival.getDriver().getCode();
        Car attacker = battle.getAttacker();
        Car defender = battle.getDefender();
        double gapMeters = race.trackGap(attacker, defender);
        double gapSeconds = gapMeters / Math.max(1, attacker.getSpeed());
        boolean alongside = gapMeters < Tuning.MIN_SEPARATION;
        boolean attackerAhead = gapMeters > race.getTrack().getLapLength() / 2;
        String gapText = attackerAhead ? (attacking ? "Gap: you are ahead now" : "Gap: " + code + " is ahead now")
                : alongside
                ? String.format(Locale.ROOT, "Gap: alongside (%.0f%% level)", 100 * (1 - gapMeters / Tuning.MIN_SEPARATION))
                : String.format(Locale.ROOT, "Gap: %.2f s (%.0f m). The attacker must be alongside by turn-in.", gapSeconds, gapMeters);
        boolean closeForAttack = attackerAhead || gapSeconds < Tuning.DEFAULT_ATTACK_GAP;
        row(0, gapText, attacking == closeForAttack ? Palette.GOOD : Palette.DANGER);

        double mySpeed = player.getTopSpeed() * 3.6;
        double theirSpeed = rival.getTopSpeed() * 3.6;
        double diff = mySpeed - theirSpeed;
        String why = reasons(player);
        String theirWhy = reasons(rival);
        row(1, String.format(Locale.ROOT, "Straight speed: you %+.0f km/h%s%s", diff,
                why.isEmpty() ? "" : "  (you: " + why + ")", theirWhy.isEmpty() ? "" : "  (" + code + ": " + theirWhy + ")"),
                tone(diff, EVEN_SPEED));

        double gripDiff = player.getCurrentGrip() - rival.getCurrentGrip();
        row(2, String.format(Locale.ROOT, "Grip: you %.0f%% vs %.0f%%   Tires: %s %.0f%% worn vs %s %.0f%% worn",
                player.getCurrentGrip() * 100, rival.getCurrentGrip() * 100,
                player.getTire().getCompound().getLetter(), player.getTire().getWear() * 100,
                rival.getTire().getCompound().getLetter(), rival.getTire().getWear() * 100),
                tone(gripDiff, EVEN_GRIP));

        double ersDiff = player.getErs().getCharge() - rival.getErs().getCharge();
        row(3, String.format(Locale.ROOT, "Battery: you %.0f%% vs %.0f%%   (X to deploy)", player.getErs().getCharge() * 100,
                rival.getErs().getCharge() * 100), tone(ersDiff, 0.1));

        double wetness = race.getConditions().wetnessAt(battle.getCorner().getBrakingPoint());
        String risk = BattleResolver.riskWord(player, wetness);
        row(4, "Lock-up risk if you brake late: " + risk + "   (worn tires, hot brakes, rain and Push raise it)",
                "low".equals(risk) ? Palette.GOOD : "high".equals(risk) ? Palette.DANGER : Palette.WARNING);

        LineChoice rivalLine = battle.getRivalLine();
        String rivalText;
        if (rivalLine == null) {
            rivalText = code + " has not committed to a line yet";
        } else if (attacking) {
            rivalText = code + " will " + (rivalLine == LineChoice.INSIDE ? "cover the inside"
                    : rivalLine == LineChoice.OUTSIDE ? "cover the outside" : "hold the optimal line")
                    + (battle.isRivalLate() ? " and brake late" : "") + " (red dashes)";
        } else {
            rivalText = code + (rivalLine == LineChoice.STAY_BEHIND ? " is staying behind"
                    : " is lining up the " + rivalLine.getDisplayName().toLowerCase(Locale.ROOT) + " line")
                    + (battle.isRivalLate() ? ", braking late" : "") + " (red dashes)";
        }
        row(5, rivalText, Palette.LINE_RIVAL);
    }

    /**
     * Lists why a car is fast on the straight right now.
     *
     * @param car the car
     * @return words such as "DRS, battery", or empty
     */
    private static String reasons(Car car) {
        StringBuilder text = new StringBuilder();
        if (car.isDrsOpen()) {
            text.append("DRS open");
        } else if (car.getDrsZone() >= 0) {
            text.append("DRS ready");
        }
        if (car.getErs().isDeploying()) {
            text.append(text.length() > 0 ? ", " : "").append("battery");
        }
        if (car.getPushLevel() == PushLevel.PUSH) {
            text.append(text.length() > 0 ? ", " : "").append("push");
        }
        return text.toString();
    }

    /**
     * Describes the corner for the panel: how hard it is to pass at.
     *
     * @param battle the battle
     * @param race the race
     * @return one line of text
     */
    private static String cornerText(Battle battle, Race race) {
        double difficulty = battle.getCorner().getDifficulty();
        double track = race.getTrack().getProfile().getOvertakingDifficulty();
        String kind = difficulty < Tuning.EASY_CORNER ? "heavy braking: a good place to pass"
                : difficulty < Tuning.HARD_CORNER ? "medium braking: possible to pass" : "little braking: hard to pass";
        return String.format(Locale.ROOT, "Turn %d, %s.  %s. Inside wins the corner if alongside; outside must be ahead.",
                battle.getCorner().getNumber(), kind, track < 0.3 ? "Easy track to pass on" : "Hard track to pass on");
    }

    /**
     * Names a choice the way the player sees it.
     *
     * @param line the choice
     * @param attacking true for the attacker's names
     * @return the name
     */
    private static String describe(LineChoice line, boolean attacking) {
        if (attacking) {
            return line.getDisplayName();
        }
        return line == LineChoice.INSIDE ? "Cover inside" : line == LineChoice.OUTSIDE ? "Cover outside" : "Optimal";
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
     * Sets one factor row.
     *
     * @param index the row
     * @param text the text
     * @param color the color
     */
    private void row(int index, String text, Color color) {
        setText(factors[index], text);
        if (!factors[index].getColor().equals(color)) {
            factors[index].setColor(color);
        }
    }

    /**
     * Shows a finished battle's result.
     *
     * @param battle the battle
     */
    public void showResult(Battle battle) {
        countdown.setSize(1, 5);
        setText(footer, battle.getMessage());
    }

    /**
     * Returns the choice under a click, if any.
     *
     * @param x x inside the panel
     * @param y y inside the panel
     * @return the choice, or null
     */
    public LineChoice choiceAt(double x, double y) {
        GObject hit = getElementAt(x, y);
        for (int i = 0; i < choices.length; i++) {
            if (hit != null && (hit == choices[i] || hit.getParent() == choices[i]) && choices[i].isVisible()) {
                return ORDER[i];
            }
        }
        return null;
    }

    /**
     * Returns whether a click hit the late braking switch.
     *
     * @param x x inside the panel
     * @param y y inside the panel
     * @return true on the switch
     */
    public boolean isLateBrakeAt(double x, double y) {
        GObject hit = getElementAt(x, y);
        return hit != null && (hit == late || hit.getParent() == late);
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
