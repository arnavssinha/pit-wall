import acm.graphics.GLabel;
import acm.graphics.GRect;
import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * The Strategy Lab screen: choose how many races to run, run them in the background with a progress
 * bar, then show which strategy and which pit window did best, how the strategies did in rain or dry
 * and on hot or cool tracks, and how the player's last race compares. Results are also saved as CSV.
 */
public final class LabScreen implements Screen {

    /** Smallest number of races per run. */
    private static final int MIN_RACES = 50;

    /** Largest number of races per run. */
    private static final int MAX_RACES = 1000;

    /** Change in race count per click. */
    private static final int RACE_STEP = 50;

    /** Width of the progress bar in pixels. */
    private static final double BAR_WIDTH = 600;

    /** The screen manager. */
    private final ScreenManager manager;

    /** The track. */
    private final Track track;

    /** The player's last race report, or null if there is none. */
    private final RaceReport lastRace;

    /** The lab. */
    private final StrategyLab lab;

    /** The clickable buttons. */
    private final ButtonGroup buttons = new ButtonGroup();

    /** Race count display. */
    private final GLabel racesLabel;

    /** Progress text. */
    private final GLabel progressLabel;

    /** Progress bar fill. */
    private final GRect progressBar;

    /** Number of races to run. */
    private int races = Tuning.LAB_DEFAULT_RACES;

    /** True while a run is in progress. */
    private volatile boolean running;

    /** The finished result, set by the background thread. */
    private volatile LabResult result;

    /** An error from the background thread, or null. */
    private volatile String error;

    /** True once the current result has been drawn. */
    private boolean resultShown;

    /**
     * Builds the screen.
     *
     * @param manager the screen manager
     * @param track the track to study
     * @param lastRace the player's last race report on this track, or null
     */
    public LabScreen(ScreenManager manager, Track track, RaceReport lastRace) {
        this.manager = manager;
        this.track = track;
        this.lastRace = lastRace;
        this.lab = new StrategyLab(track, manager.getRoster(), manager.weatherFor(track.getProfile().getId()), Tuning.DEFAULT_CARS);
        GraphicsProgram p = manager.getProgram();
        p.add(Palette.background(manager.getWidth(), manager.getHeight()));
        p.add(Palette.label("STRATEGY LAB  " + track.getProfile().getName().toUpperCase(Locale.ROOT), Palette.HEADLINE, Palette.TEXT), 40, 50);
        p.add(Palette.label("Races every strategy on the same random grids, weather and incidents, with the computer driving your car.",
                Palette.SMALL, Palette.TEXT_DIM), 42, 72);
        p.add(Palette.label("Enter: run   - / +: number of races   Esc: back", Palette.SMALL, Palette.TEXT_DIM), 42, 90);

        UiButton fewer = new UiButton("-", 44, 36);
        UiButton more = new UiButton("+", 44, 36);
        UiButton run = new UiButton("Run  (Enter)", 180, 36);
        p.add(fewer, 40, 104);
        p.add(more, 180, 104);
        p.add(run, 240, 104);
        racesLabel = Palette.label("", Palette.HUD, Palette.TEXT);
        p.add(racesLabel, 94, 128);
        buttons.add(fewer, () -> changeRaces(-RACE_STEP));
        buttons.add(more, () -> changeRaces(RACE_STEP));
        buttons.add(run, () -> startRun());

        GRect barBack = new GRect(440, 110, BAR_WIDTH, 22);
        barBack.setFilled(true);
        barBack.setFillColor(Palette.GAUGE_BACK);
        barBack.setColor(Palette.PANEL_EDGE);
        p.add(barBack);
        progressBar = new GRect(440, 110, 1, 22);
        progressBar.setFilled(true);
        progressBar.setFillColor(Palette.ERS);
        progressBar.setColor(Palette.ERS);
        p.add(progressBar);
        progressLabel = Palette.label("Press Enter to run.", Palette.SMALL, Palette.TEXT);
        p.add(progressLabel, 440, 150);
        updateRacesLabel();
    }

    /**
     * Changes the number of races.
     *
     * @param delta change in races
     */
    private void changeRaces(int delta) {
        if (!running) {
            races = Math.max(MIN_RACES, Math.min(MAX_RACES, races + delta));
            updateRacesLabel();
        }
    }

    /**
     * Shows the race count.
     */
    private void updateRacesLabel() {
        racesLabel.setLabel(races + " races");
    }

    /**
     * Starts a run on a background thread so the window stays responsive.
     */
    private void startRun() {
        if (running) {
            return;
        }
        running = true;
        result = null;
        error = null;
        resultShown = false;
        long seed = Math.floorMod(System.nanoTime(), 1000000L);
        Thread worker = new Thread(() -> {
            try {
                result = lab.run(races, seed);
            } catch (RuntimeException e) {
                error = e.getMessage();
            } finally {
                running = false;
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Updates the progress bar, and draws the results once they arrive.
     *
     * @param realSeconds real time since the previous frame
     */
    @Override
    public void frame(double realSeconds) {
        if (running) {
            int done = lab.getFinished();
            int planned = Math.max(1, lab.getPlanned());
            progressBar.setSize(Math.max(1, BAR_WIDTH * done / planned), 22);
            progressLabel.setLabel(String.format(Locale.ROOT, "Running: %d of %d races...", done, planned));
        } else if (error != null) {
            progressLabel.setLabel("The lab stopped with an error: " + error);
        } else if (result != null && !resultShown) {
            resultShown = true;
            progressBar.setSize(BAR_WIDTH, 22);
            showResult(result);
        }
    }

    /**
     * Draws the results: a table per strategy, bar charts for strategies and pit windows, the
     * comparison with the player's last race, and the CSV location.
     *
     * @param r the result
     */
    private void showResult(LabResult r) {
        GraphicsProgram p = manager.getProgram();
        String saved;
        try {
            saved = "Saved " + LabCsvWriter.writeForTrack(r, track.getProfile().getId());
        } catch (IOException e) {
            saved = "Could not save the CSV: " + e.getMessage();
        }
        progressLabel.setLabel(String.format(Locale.ROOT, "%d races in %.1f s.   %s", r.getRuns().size(), r.getSeconds(), saved));

        p.add(Palette.panel(1200, 150), 40, 168);
        p.add(Palette.label(String.format(Locale.ROOT, "%-16s %5s %6s %7s %8s %7s  %-9s %-9s %-9s %-9s", "STRATEGY", "races",
                "win", "podium", "avg pos", "gained", "rain", "dry", "hot", "cool"), Palette.MONO, Palette.TEXT_DIM), 54, 192);
        List<StrategyVariant> variants = r.getVariants();
        StrategyVariant best = r.bestPreset();
        int row = 0;
        for (StrategyVariant v : variants) {
            if (v.isWindow()) {
                continue;
            }
            List<LabRun> all = r.runsOf(v, 0);
            String line = String.format(Locale.ROOT, "%-16s %5d %5.0f%% %6.0f%% %8.1f %+7.1f  %-9s %-9s %-9s %-9s", v.getName(),
                    all.size(), LabResult.shareInTop(all, 1) * 100, LabResult.shareInTop(all, 3) * 100,
                    LabResult.averageFinish(all), LabResult.averageGained(all), bucket(r, v, 1), bucket(r, v, 2),
                    bucket(r, v, 3), bucket(r, v, 4));
            p.add(Palette.label(line, Palette.MONO, v == best ? Palette.PLAYER : Palette.TEXT), 54, 220 + row * 24);
            row++;
        }
        p.add(Palette.label(String.format(Locale.ROOT, "rain / dry / hot / cool columns: average finish (number of races). Hot means track at or above %.0f C.",
                r.getHotThreshold()), Palette.SMALL, Palette.TEXT_DIM), 54, 300);

        String[] presetNames = new String[3];
        double[] presetGains = new double[3];
        String[] windowNames = new String[variants.size() - 3];
        double[] windowGains = new double[variants.size() - 3];
        int pi = 0;
        int wi = 0;
        for (StrategyVariant v : variants) {
            double gain = LabResult.averageGained(r.runsOf(v, 0));
            if (v.isWindow()) {
                windowNames[wi] = "lap " + v.getWindowLap();
                windowGains[wi++] = gain;
            } else {
                presetNames[pi] = v.getName();
                presetGains[pi++] = gain;
            }
        }
        p.add(new BarChart("Average places gained by strategy", 580, 200, presetNames, presetGains, "%+.1f",
                Palette.GOOD, Palette.DANGER, false), 40, 330);
        StrategyVariant window = r.bestWindow();
        String windowTitle = "Pit window, Medium then Hard. Best: " + (window == null ? "-" : window.getName().toLowerCase(Locale.ROOT));
        p.add(new BarChart(windowTitle, 600, 200, windowNames, windowGains, "%+.1f", Palette.GOOD, Palette.DANGER, false), 640, 330);

        p.add(Palette.panel(1200, 74), 40, 544);
        p.add(Palette.label("Best strategy here: " + best.getName() + String.format(Locale.ROOT,
                " (average %+.1f places from the start, %.0f%% podiums).", LabResult.averageGained(r.runsOf(best, 0)),
                LabResult.shareInTop(r.runsOf(best, 0), 3) * 100), Palette.HUD, Palette.PLAYER), 54, 570);
        p.add(Palette.label(compareWithLastRace(), Palette.HUD, Palette.TEXT), 54, 598);
    }

    /**
     * Describes the average finish of one strategy in one weather bucket.
     *
     * @param r the result
     * @param v the strategy
     * @param filter 1 rain, 2 dry, 3 hot, 4 cool
     * @return text such as "8.4 (12)", or "- (0)"
     */
    private static String bucket(LabResult r, StrategyVariant v, int filter) {
        List<LabRun> runs = r.runsOf(v, filter);
        return runs.isEmpty() ? "- (0)" : String.format(Locale.ROOT, "%.1f (%d)", LabResult.averageFinish(runs), runs.size());
    }

    /**
     * Compares the player's last race with the lab's best strategy.
     *
     * @return one line of text
     */
    private String compareWithLastRace() {
        if (lastRace == null) {
            return "Race on this track first, then open the lab from the results to compare your own race.";
        }
        Car player = lastRace.getRace().getPlayerCar();
        if (player.isRetired() || player.isDisqualified()) {
            return "Your last race did not finish (" + (player.isRetired() ? "DNF" : "DSQ") + "), so it cannot be compared.";
        }
        int gained = player.getGridPosition() - lastRace.playerPosition();
        return String.format(Locale.ROOT, "Your last race: P%d from P%d (%+d places, %d %s). The lab's best strategy averages %+.1f.",
                lastRace.playerPosition(), player.getGridPosition(), gained, player.getStopsMade(),
                player.getStopsMade() == 1 ? "stop" : "stops", LabResult.averageGained(result.runsOf(result.bestPreset(), 0)));
    }

    /**
     * Enter runs, + and - change the race count, Esc goes back.
     *
     * @param keyCode the key
     * @param shift whether Shift was held
     */
    @Override
    public void handleKey(int keyCode, boolean shift) {
        if (keyCode == KeyEvent.VK_ENTER) {
            startRun();
        } else if (keyCode == KeyEvent.VK_EQUALS || keyCode == KeyEvent.VK_PLUS || keyCode == KeyEvent.VK_ADD) {
            changeRaces(RACE_STEP);
        } else if (keyCode == KeyEvent.VK_MINUS || keyCode == KeyEvent.VK_SUBTRACT) {
            changeRaces(-RACE_STEP);
        } else if (keyCode == KeyEvent.VK_ESCAPE && !running) {
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
