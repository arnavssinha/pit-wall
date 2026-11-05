import acm.graphics.GLabel;
import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;

/**
 * The results screen (brief, Section 3.12): the final classification and the race report: the
 * player's tire wear by lap, gaps to the leader by lap for the player and the cars either side,
 * the best sector times, and where the player gained or lost time at each corner.
 */
public final class ResultsScreen implements Screen {

    /** The screen manager. */
    private final ScreenManager manager;

    /** The race report. */
    private final RaceReport report;

    /** The clickable buttons. */
    private final ButtonGroup buttons = new ButtonGroup();

    /**
     * Builds the screen for a finished race.
     *
     * @param manager the screen manager
     * @param race the finished race
     */
    public ResultsScreen(ScreenManager manager, Race race) {
        this.manager = manager;
        this.report = new RaceReport(race);
        GraphicsProgram p = manager.getProgram();
        double w = manager.getWidth();
        double h = manager.getHeight();
        p.add(Palette.background(w, h));
        p.add(Palette.label("RESULTS  " + race.getTrack().getProfile().getName().toUpperCase(Locale.ROOT)
                + "   seed " + race.getConfig().getSeed(), Palette.HEADLINE, Palette.TEXT), 30, 44);
        p.add(Palette.label(headline(race), Palette.HUD, Palette.PLAYER), 30, 70);
        addClassification(p, race);
        addCharts(p, race);

        UiButton again = new UiButton("New race  (Enter)", 200, 40);
        UiButton lab = new UiButton("Strategy Lab  (L)", 200, 40);
        UiButton menu = new UiButton("Menu  (Esc)", 160, 40);
        p.add(again, 560, h - 52);
        p.add(lab, 776, h - 52);
        p.add(menu, 992, h - 52);
        String trackId = race.getTrack().getProfile().getId();
        buttons.add(again, () -> manager.showSetup(trackId));
        buttons.add(lab, () -> manager.showLab(trackId, report));
        buttons.add(menu, () -> manager.showMenu());
    }

    /**
     * Writes the one-line summary of the player's race.
     *
     * @param race the race
     * @return text such as "You finished P5 from P9 (+4 places), 1 stop."
     */
    private String headline(Race race) {
        Car player = race.getPlayerCar();
        String stops = player.getStopsMade() + (player.getStopsMade() == 1 ? " stop" : " stops");
        if (player.isDisqualified()) {
            return "You were disqualified (only one dry compound used). " + stops + ".";
        }
        if (player.isRetired()) {
            return "You retired from the race. " + stops + ".";
        }
        int pos = report.playerPosition();
        int gained = player.getGridPosition() - pos;
        String change = gained == 0 ? "no change" : (gained > 0 ? "+" : "") + gained + (Math.abs(gained) == 1 ? " place" : " places");
        return String.format(Locale.ROOT, "You finished P%d from P%d (%s), %s.", pos, player.getGridPosition(), change, stops);
    }

    /**
     * Adds the classification table.
     *
     * @param p the program
     * @param race the race
     */
    private void addClassification(GraphicsProgram p, Race race) {
        List<Car> order = report.getClassification();
        double rowH = Math.min(24, (manager.getHeight() - 140) / order.size());
        p.add(Palette.panel(500, rowH * order.size() + 40), 30, 86);
        p.add(Palette.label("POS DRIVER                     TIME/GAP   STOPS", Palette.MONO, Palette.TEXT_DIM), 42, 108);
        Car winner = order.get(0);
        for (int i = 0; i < order.size(); i++) {
            Car car = order.get(i);
            String result;
            if (car.isDisqualified()) {
                result = "DSQ";
            } else if (car.isRetired()) {
                result = "DNF";
            } else if (car == winner) {
                result = clock(car.getFinishTime());
            } else if (car.getLapsCompleted() < winner.getLapsCompleted()) {
                int down = winner.getLapsCompleted() - car.getLapsCompleted();
                result = "+" + down + (down == 1 ? " lap" : " laps");
            } else {
                result = String.format(Locale.ROOT, "+%.3f", car.getFinishTime() - winner.getFinishTime());
            }
            String line = String.format(Locale.ROOT, "P%-3d %s %-22s %9s     %d", i + 1, car.getDriver().getCode(),
                    car.getDriver().getTeam().getName(), result, car.getStopsMade());
            GLabel label = Palette.label(line, Palette.MONO, car.isPlayer() ? Palette.PLAYER : Palette.TEXT);
            p.add(label, 42, 130 + i * rowH);
        }
    }

    /**
     * Adds the charts and the sector table.
     *
     * @param p the program
     * @param race the race
     */
    private void addCharts(GraphicsProgram p, Race race) {
        int laps = race.getConfig().getLaps();
        LineChart wear = new LineChart("Your tire wear by lap", 330, 190, 0, 1.2, "%.0f%%", 100, laps);
        wear.addSeries(report.playerWearByLap(), Palette.PLAYER, "you");
        p.add(wear, 550, 86);

        Car player = race.getPlayerCar();
        Car[] near = report.playerNeighbours();
        double maxGap = 1;
        double[][] series = {report.gapToLeaderByLap(player),
            near[0] == null ? null : report.gapToLeaderByLap(near[0]),
            near[1] == null ? null : report.gapToLeaderByLap(near[1])};
        for (double[] s : series) {
            if (s != null) {
                for (double v : s) {
                    if (!Double.isNaN(v)) {
                        maxGap = Math.max(maxGap, v);
                    }
                }
            }
        }
        LineChart gaps = new LineChart("Gap to leader (s)", 360, 190, 0, Math.ceil(maxGap / 5) * 5, "%.0f", 1, laps);
        gaps.addSeries(series[0], Palette.PLAYER, "you");
        if (near[0] != null) {
            gaps.addSeries(series[1], Palette.ERS, near[0].getDriver().getCode() + " ahead");
        }
        if (near[1] != null) {
            gaps.addSeries(series[2], Palette.DANGER, near[1].getDriver().getCode() + " behind");
        }
        p.add(gaps, 890, 86);

        List<Corner> corners = race.getCorners();
        String[] names = new String[corners.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = "T" + corners.get(i).getNumber();
        }
        BarChart cornersChart = new BarChart("Where you gained (green) or lost (red) time per lap, vs the field",
                700, 200, names, report.getCornerDelta(), "%+.2f", Palette.GOOD, Palette.DANGER, true);
        p.add(cornersChart, 550, 290);

        p.add(Palette.panel(700, 120), 550, 504);
        p.add(Palette.label("BEST SECTORS        fastest                    yours", Palette.MONO, Palette.TEXT_DIM), 562, 528);
        for (int s = 0; s < RaceReport.SECTORS; s++) {
            Car best = report.getBestSectorCar(s);
            String bestName = best == null ? "-" : best.getDriver().getCode();
            double mine = report.getPlayerBestSector(s);
            String line = String.format(Locale.ROOT, "Sector %d        %7.3f s  %s              %s", s + 1,
                    report.getBestSector(s), bestName, mine == Double.MAX_VALUE ? "   -" : String.format(Locale.ROOT, "%7.3f s", mine));
            boolean purple = best != null && best.isPlayer();
            p.add(Palette.label(line, Palette.MONO, purple ? Palette.PURPLE : Palette.TEXT), 562, 554 + s * 22);
        }
    }

    /**
     * Formats a race time as minutes:seconds.
     *
     * @param seconds race time
     * @return text such as "14:32.118"
     */
    private static String clock(double seconds) {
        int minutes = (int) (seconds / 60);
        return String.format(Locale.ROOT, "%d:%06.3f", minutes, seconds - minutes * 60);
    }

    /**
     * Nothing moves on this screen.
     *
     * @param realSeconds real time since the previous frame
     */
    @Override
    public void frame(double realSeconds) {
        return;
    }

    /**
     * Enter for a new race, L for the Strategy Lab, Esc for the menu.
     *
     * @param keyCode the key
     * @param shift whether Shift was held
     */
    @Override
    public void handleKey(int keyCode, boolean shift) {
        String trackId = report.getRace().getTrack().getProfile().getId();
        if (keyCode == KeyEvent.VK_ENTER) {
            manager.showSetup(trackId);
        } else if (keyCode == KeyEvent.VK_L) {
            manager.showLab(trackId, report);
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            manager.showMenu();
        }
    }

    /**
     * Clicks the button under the mouse.
     *
     * @param x x on the canvas
     * @param y y on the canvas
     */
    @Override
    public void handleClick(double x, double y) {
        buttons.click(x, y);
    }
}
