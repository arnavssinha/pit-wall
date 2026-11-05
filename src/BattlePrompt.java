import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

/**
 * The battle prompt: who you are fighting and for which corner, the gap, tire grip and battery on
 * both sides, a hint of the risk, the stance buttons and a countdown bar. After the battle it shows
 * the one-line result for a moment.
 */
public final class BattlePrompt extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 520;

    /** Panel height in pixels. */
    public static final double HEIGHT = 150;

    /** Width of the full countdown bar in pixels. */
    private static final double BAR_WIDTH = WIDTH - 24;

    /** Headline: corner and rival. */
    private final GLabel headline;

    /** Situation: gap, grip, battery, risk. */
    private final GLabel details;

    /** Default stance hint, or the result after the battle. */
    private final GLabel footer;

    /** Inside attack button. */
    private final UiButton attack;

    /** Outside attack button. */
    private final UiButton outside;

    /** Defend button. */
    private final UiButton defend;

    /** Hold button. */
    private final UiButton hold;

    /** Countdown bar. */
    private final GRect countdown;

    /**
     * Builds the prompt (hidden until shown by the race screen).
     */
    public BattlePrompt() {
        GRect panel = new GRect(0, 0, WIDTH, HEIGHT);
        panel.setFilled(true);
        panel.setFillColor(Palette.PANEL);
        panel.setColor(Palette.DANGER);
        add(panel);
        headline = text(12, 26, Palette.HEADLINE.deriveFont(20f), Palette.TEXT);
        details = text(12, 50, Palette.SMALL, Palette.TEXT_DIM);
        attack = button("A  Inside", 12);
        outside = button("O  Outside", 12 + 126);
        defend = button("S  Defend", 12 + 252);
        hold = button("D  Hold", 12 + 378);
        countdown = new GRect(12, 106, BAR_WIDTH, 6);
        countdown.setFilled(true);
        countdown.setFillColor(Palette.DANGER);
        countdown.setColor(Palette.DANGER);
        add(countdown);
        footer = text(12, 136, Palette.HUD, Palette.PLAYER);
    }

    /**
     * Creates and adds a label.
     *
     * @param x x position
     * @param y baseline y
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
     * Creates and adds a stance button.
     *
     * @param text the label
     * @param x x position
     * @return the button
     */
    private UiButton button(String text, double x) {
        UiButton b = new UiButton(text, 118, 34);
        add(b, x, 62);
        return b;
    }

    /**
     * Refreshes the prompt for an open battle.
     *
     * @param battle the player's battle
     * @param race the race
     * @param secondsLeft real seconds left to choose
     */
    public void showOpen(Battle battle, Race race, double secondsLeft) {
        Car player = race.getPlayerCar();
        Car rival = battle.getRival();
        boolean attacking = battle.getAttacker() == player;
        String code = rival.getDriver().getCode();
        headline.setLabel(String.format(Locale.ROOT, "TURN %d  %s %s", battle.getCorner().getNumber(),
                attacking ? "ATTACKING" : "DEFENDING AGAINST", code));
        double risk = BattleResolver.attackRisk(battle.getAttacker(),
                race.getConditions().wetnessAt(battle.getCorner().getBrakingPoint()), Stance.ATTACK);
        String riskText = risk < Tuning.RISK_BASE * 2 ? "low" : risk < Tuning.RISK_BASE * 4 ? "medium" : "high";
        details.setLabel(String.format(Locale.ROOT,
                "Gap %.2f s   Grip you %.0f%% / %s %.0f%%   ERS you %.0f%% / %s %.0f%%   %s risk %s",
                battle.getGapSeconds(), player.getCurrentGrip() * 100, code, rival.getCurrentGrip() * 100,
                player.getErs().getCharge() * 100, code, rival.getErs().getCharge() * 100,
                attacking ? "Your" : "Their", riskText));
        attack.setEnabled(attacking);
        outside.setEnabled(attacking);
        defend.setEnabled(!attacking);
        Stance chosen = battle.getPlayerStance();
        attack.setHighlighted(chosen == Stance.ATTACK);
        outside.setHighlighted(chosen == Stance.OUTSIDE);
        defend.setHighlighted(chosen == Stance.DEFEND);
        hold.setHighlighted(chosen == Stance.HOLD);
        double fraction = Math.max(0, Math.min(1, secondsLeft / Tuning.BATTLE_DECISION_SECONDS));
        countdown.setSize(Math.max(1, BAR_WIDTH * fraction), 6);
        if (chosen != null) {
            footer.setLabel("Locked in: " + chosen.getDisplayName() + "   (X toggles ERS for the move)");
        } else {
            Stance fallback = DefaultStancePolicy.playerDefault(player, battle.getGapSeconds(), attacking);
            String warning = !attacking && rival.getIntent() == Stance.ATTACK ? code + " is lining up a move!   " : "";
            footer.setLabel(String.format(Locale.ROOT, "%sChoose in %.1f s, or it will be: %s", warning,
                    Math.max(0, secondsLeft), fallback.getDisplayName()));
        }
    }

    /**
     * Shows the result after the battle.
     *
     * @param battle the resolved battle
     */
    public void showResult(Battle battle) {
        countdown.setSize(1, 6);
        footer.setLabel(battle.getMessage());
    }

    /**
     * Works out which stance button, if any, is under a point.
     *
     * @param x x in this compound's coordinates
     * @param y y in this compound's coordinates
     * @return the stance of an enabled button, or null
     */
    public Stance stanceAt(double x, double y) {
        GObject hit = getElementAt(x, y);
        if (hit == null) {
            return null;
        }
        if (hit == attack || hit.getParent() == attack) {
            return attack.isEnabled() ? Stance.ATTACK : null;
        }
        if (hit == outside || hit.getParent() == outside) {
            return outside.isEnabled() ? Stance.OUTSIDE : null;
        }
        if (hit == defend || hit.getParent() == defend) {
            return defend.isEnabled() ? Stance.DEFEND : null;
        }
        if (hit == hold || hit.getParent() == hold) {
            return Stance.HOLD;
        }
        return null;
    }
}
