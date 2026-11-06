import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

/**
 * The text around the edges of the race screen: track name and clock at the top, the player's
 * assignment and speed at the bottom, a controls hint, and the chequered-flag banner at the end.
 */
public final class HudPanel extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Lowest pixel row used by the top bar; the world view is framed below it. */
    public static final double TOP_BAR_BOTTOM = 50;

    /** Height reserved for the bottom panel, in pixels; the world view is framed above it. */
    public static final double BOTTOM_PANEL_HEIGHT = 100;

    /** The race being shown. */
    private final transient Race race;

    /** Track name, race clock, time speed and pause state. */
    private final GLabel topLine;

    /** "You are driving..." assignment line. */
    private final GLabel assignment;

    /** Speed and lap times of the car being followed. */
    private final GLabel carLine;

    /** The player's pit plan and any pit call. */
    private final GLabel pitLine;

    /** Banner background shown when the race ends. */
    private final GRect bannerBox;

    /** First banner line. */
    private final GLabel bannerTitle;

    /** Second banner line. */
    private final GLabel bannerDetail;

    /** Screen width in pixels, for centering. */
    private final double width;

    /** True once the chequered-flag banner is on screen. */
    private boolean bannerShown;

    /**
     * Builds the HUD for a canvas of the given size.
     *
     * @param race the race
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     */
    public HudPanel(Race race, double width, double height) {
        this.race = race;
        this.width = width;
        double left = TimingTower.WIDTH + 24;

        GRect topBox = new GRect(left, 8, 560, 34);
        topBox.setFilled(true);
        topBox.setFillColor(Palette.PANEL);
        topBox.setColor(Palette.PANEL_EDGE);
        add(topBox);
        topLine = text("", left + 12, 31, Palette.HUD, Palette.TEXT);

        GRect bottomBox = new GRect(left, height - 92, 640, 84);
        bottomBox.setFilled(true);
        bottomBox.setFillColor(Palette.PANEL);
        bottomBox.setColor(Palette.PANEL_EDGE);
        add(bottomBox);
        Car player = race.getPlayerCar();
        Driver d = player.getDriver();
        assignment = text(String.format(Locale.ROOT, "You are driving #%d %s for %s, starting P%d.",
                d.getNumber(), d.getName(), d.getTeam().getName(), player.getGridPosition()),
                left + 12, height - 70, Palette.HUD, Palette.PLAYER);
        carLine = text("", left + 12, height - 51, Palette.MONO, Palette.TEXT);
        pitLine = text("", left + 12, height - 33, Palette.MONO, Palette.TEXT);
        text("QWE push  X ERS  A/S/O/D line  L late brake  C attack  B/N box  1-5 tires  Space  T speed  Z  F/G  Esc",
                left + 12, height - 14, Palette.SMALL, Palette.TEXT_DIM);

        bannerBox = new GRect(0, 0, 520, 84);
        bannerBox.setFilled(true);
        bannerBox.setFillColor(Palette.PANEL);
        bannerBox.setColor(Palette.PLAYER);
        bannerTitle = new GLabel("");
        bannerTitle.setFont(Palette.HEADLINE);
        bannerTitle.setColor(Palette.TEXT);
        bannerDetail = new GLabel("");
        bannerDetail.setFont(Palette.HUD);
        bannerDetail.setColor(Palette.PLAYER);
    }

    /**
     * Creates and adds a label.
     *
     * @param s text
     * @param x x position
     * @param y baseline y position
     * @param font font
     * @param color color
     * @return the label
     */
    private GLabel text(String s, double x, double y, Font font, Color color) {
        GLabel label = new GLabel(s, x, y);
        label.setFont(font);
        label.setColor(color);
        add(label);
        return label;
    }

    /**
     * Refreshes all text.
     *
     * @param followed the car the camera is following
     * @param timeSpeed current playback speed multiplier
     * @param paused whether the race is paused
     */
    public void update(Car followed, int timeSpeed, boolean paused) {
        String state = paused ? "PAUSED" : timeSpeed + "x";
        if (race.getSafetyCar().isActive()) {
            state += "   SAFETY CAR";
        }
        setText(topLine, String.format(Locale.ROOT, "%s   %s   %s   %s",
                race.getTrack().getProfile().getName().toUpperCase(Locale.ROOT),
                clock(race.getTime()), state, "SEED " + race.getConfig().getSeed()));

        String who = followed == race.getPlayerCar() ? "YOU" : "FOLLOWING " + followed.getDriver().getCode();
        int position = race.getStandings().indexOf(followed) + 1;
        setText(carLine, String.format(Locale.ROOT, "%-14s P%-3d %3.0f km/h   last %s   best %s",
                who, position, followed.getSpeed() * 3.6,
                lapText(followed.getLastLapTime()), lapText(followed.getBestLapTime())));

        setText(pitLine, pitText(race.getPlayerCar()));

        boolean over = race.getStandings().get(0).isFinished();
        if (over && !bannerShown) {
            showBanner();
        }
    }

    /**
     * Describes the player's pit situation: a stop under way, a pit call, or the next planned stop.
     *
     * @param player the player's car
     * @return one line of text
     */
    private static String pitText(Car player) {
        switch (player.getPitPhase()) {
            case IN_LANE:
            case STOPPED:
            case LEAVING:
                return "PIT STOP in progress: " + player.getRequestedCompound().getDisplayName() + "s";
            case MERGING:
                return "Leaving the pits: waiting for a gap to rejoin";
            default:
                break;
        }
        if (player.getPitRequest() == 1) {
            return "BOX THIS LAP for " + player.getRequestedCompound().getDisplayName() + "s   (B cancels, 1-5 tires)";
        }
        if (player.getPitRequest() == 2) {
            return "BOX NEXT LAP for " + player.getRequestedCompound().getDisplayName() + "s   (B cancels, 1-5 tires)";
        }
        PitStop next = player.getPlan().nextStop();
        String plan = next == null ? "no more stops planned" : "plan: stop at end of lap " + next.getLap()
                + " for " + next.getCompound().getDisplayName() + "s";
        return String.format(Locale.ROOT, "Stops %d, %s   (B box now)", player.getStopsMade(), plan);
    }

    /**
     * Shows the chequered-flag banner with the winner and the player's result so far.
     */
    private void showBanner() {
        Car winner = race.getStandings().get(0);
        Car player = race.getPlayerCar();
        bannerTitle.setLabel("CHEQUERED FLAG - " + winner.getDriver().getName() + " wins");
        String result = player.isFinished() ? "You finished P" + player.getFinishPosition() : "You are finishing your last lap";
        bannerDetail.setLabel(result + ".  Enter: results once you finish.");
        double x = (width - bannerBox.getWidth()) / 2 + TimingTower.WIDTH / 2;
        bannerBox.setLocation(x, 60);
        bannerTitle.setLocation(x + 16, 98);
        bannerDetail.setLocation(x + 16, 126);
        add(bannerBox);
        add(bannerTitle);
        add(bannerDetail);
        bannerShown = true;
    }

    /**
     * Updates the result line once the player has finished.
     */
    public void refreshBanner() {
        Car player = race.getPlayerCar();
        if (bannerShown && player.isFinished()) {
            setText(bannerDetail, "You finished P" + player.getFinishPosition() + ".  Press Enter for the results.");
        }
    }

    /**
     * Formats race time as minutes:seconds.tenths.
     *
     * @param seconds race time
     * @return text such as "3:07.4"
     */
    private static String clock(double seconds) {
        int minutes = (int) (seconds / 60);
        return String.format(Locale.ROOT, "%d:%04.1f", minutes, seconds - minutes * 60);
    }

    /**
     * Formats a lap time, or dashes if there is none yet.
     *
     * @param seconds lap time, 0 if none
     * @return text such as "1:25.123"
     */
    private static String lapText(double seconds) {
        if (seconds <= 0) {
            return "-:--.---";
        }
        int minutes = (int) (seconds / 60);
        return String.format(Locale.ROOT, "%d:%06.3f", minutes, seconds - minutes * 60);
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
