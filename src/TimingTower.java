import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.List;
import java.util.Locale;

/**
 * The timing tower: lap counter on top, then one row per car in race order with position,
 * team color, driver code and gap. Rows are built once; each frame only their text and colors
 * change, and a label's text is only replaced when it actually changed.
 */
public final class TimingTower extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 236;

    /** Height of one row in pixels. */
    private static final double ROW_HEIGHT = 25;

    /** Height of the header area in pixels. */
    private static final double HEADER = 44;

    /** The race being shown. */
    private final transient Race race;

    /** Lap counter text, such as "LAP 3 / 10". */
    private final GLabel lapLabel;

    /** Highlight behind the player's row. */
    private final GRect playerRow;

    /** Position number per row. */
    private final GLabel[] positionLabels;

    /** Team color stripe per row. */
    private final GRect[] colorBars;

    /** Driver code per row. */
    private final GLabel[] codeLabels;

    /** Gap text per row. */
    private final GLabel[] gapLabels;

    /** Tire compound circle per row. */
    private final GOval[] tireBadges;

    /** Tire compound letter per row. */
    private final GLabel[] tireLetters;

    /**
     * Builds the tower with one row per car.
     *
     * @param race the race
     */
    public TimingTower(Race race) {
        this.race = race;
        int rows = race.getCars().size();
        GRect panel = new GRect(0, 0, WIDTH, HEADER + rows * ROW_HEIGHT + 8);
        panel.setFilled(true);
        panel.setFillColor(Palette.PANEL);
        panel.setColor(Palette.PANEL_EDGE);
        add(panel);

        lapLabel = new GLabel("", 12, 29);
        lapLabel.setFont(Palette.HEADLINE.deriveFont(20f));
        lapLabel.setColor(Palette.TEXT);
        add(lapLabel);

        playerRow = new GRect(2, 0, WIDTH - 4, ROW_HEIGHT);
        playerRow.setFilled(true);
        playerRow.setFillColor(Palette.PLAYER_ROW);
        playerRow.setColor(Palette.PLAYER);
        add(playerRow);

        positionLabels = new GLabel[rows];
        colorBars = new GRect[rows];
        codeLabels = new GLabel[rows];
        gapLabels = new GLabel[rows];
        tireBadges = new GOval[rows];
        tireLetters = new GLabel[rows];
        for (int i = 0; i < rows; i++) {
            double baseline = HEADER + i * ROW_HEIGHT + 17;
            positionLabels[i] = label(String.valueOf(i + 1), 12, baseline, Palette.TEXT_DIM);
            colorBars[i] = new GRect(40, baseline - 13, 4, 16);
            colorBars[i].setFilled(true);
            add(colorBars[i]);
            codeLabels[i] = label("", 52, baseline, Palette.TEXT);
            gapLabels[i] = label("", 112, baseline, Palette.TEXT_DIM);
            tireBadges[i] = new GOval(WIDTH - 30, baseline - 13, 17, 17);
            tireBadges[i].setFilled(true);
            add(tireBadges[i]);
            tireLetters[i] = label("", WIDTH - 26, baseline, Palette.CAR_OUTLINE);
        }
    }

    /**
     * Creates and adds a timing label.
     *
     * @param text initial text
     * @param x x position
     * @param y baseline y position
     * @param color text color
     * @return the label
     */
    private GLabel label(String text, double x, double y, Color color) {
        GLabel l = new GLabel(text, x, y);
        l.setFont(Palette.MONO);
        l.setColor(color);
        add(l);
        return l;
    }

    /**
     * Refreshes every row from the current standings.
     */
    public void update() {
        setText(lapLabel, String.format(Locale.ROOT, "LAP %d / %d", race.getLeaderLap(), race.getConfig().getLaps()));
        List<Car> standings = race.getStandings();
        for (int i = 0; i < standings.size(); i++) {
            Car car = standings.get(i);
            setText(codeLabels[i], car.getDriver().getCode());
            setText(gapLabels[i], gapText(car, i));
            Compound compound = car.getTire().getCompound();
            setText(tireLetters[i], compound.getLetter());
            Color tireColor = Palette.compoundColor(compound);
            if (!tireColor.equals(tireBadges[i].getFillColor())) {
                tireBadges[i].setFillColor(tireColor);
                tireBadges[i].setColor(tireColor);
            }
            Color team = Palette.teamColor(car.getDriver().getTeam());
            if (!team.equals(colorBars[i].getFillColor())) {
                colorBars[i].setFillColor(team);
                colorBars[i].setColor(team);
            }
            if (car == race.getPlayerCar()) {
                double y = HEADER + i * ROW_HEIGHT;
                if (playerRow.getY() != y) {
                    playerRow.setLocation(2, y);
                }
            }
        }
    }

    /**
     * Builds the gap text for one row: "Leader", "+1.234" seconds, or "+1 LAP".
     *
     * @param car the car in this row
     * @param position row index, 0 for the leader
     * @return the text
     */
    private String gapText(Car car, int position) {
        if (position == 0) {
            return car.isFinished() ? "Winner" : "Leader";
        }
        if (car.isDisqualified()) {
            return "DSQ";
        }
        if (car.isInPitLane() && !car.isFinished()) {
            return "IN PIT";
        }
        int lapsDown = race.lapsBehindLeader(car);
        if (lapsDown > 0) {
            return "+" + lapsDown + (lapsDown == 1 ? " LAP" : " LAPS");
        }
        return String.format(Locale.ROOT, "+%.3f", race.gapToLeader(car));
    }

    /**
     * Replaces a label's text only if it changed, to avoid needless repaints.
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
