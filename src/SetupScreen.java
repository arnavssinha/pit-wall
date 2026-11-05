import acm.graphics.GLabel;
import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The setup screen (brief, Section 3.3). The driver, team and grid slot are assigned at random by a
 * simulated qualifying session and shown; the player picks a strategy preset, the compound for each
 * stint, the number of cars and a baseline push level, then starts the race.
 * Strategy edits can be undone: each edit pushes the previous choice onto a stack (a Deque), and
 * Undo pops it back.
 */
public final class SetupScreen implements Screen {

    /** Grid rows shown in the qualifying preview. */
    private static final int GRID_ROWS = 12;

    /** Compounds the stint buttons cycle through, in order. */
    private static final Compound[] CYCLE = Compound.values();

    /** The screen manager. */
    private final ScreenManager manager;

    /** The track. */
    private final Track track;

    /** The clickable buttons. */
    private final ButtonGroup buttons = new ButtonGroup();

    /** Earlier strategy choices, most recent on top, for Undo. */
    private final Deque<StrategyChoice> undo = new ArrayDeque<StrategyChoice>();

    /** Preset buttons, in preset order. */
    private final List<UiButton> presetButtons = new ArrayList<UiButton>();

    /** Race length buttons, in RaceLength order. */
    private final List<UiButton> lengthButtons = new ArrayList<UiButton>();

    /** The chosen race length. */
    private RaceLength length = RaceLength.RACE;

    /** Stint buttons (as many as the preset has stints). */
    private final List<UiButton> stintButtons = new ArrayList<UiButton>();

    /** Push level buttons, in push order. */
    private final List<UiButton> pushButtons = new ArrayList<UiButton>();

    /** The race being set up (rebuilt on re-roll or car count change). */
    private Race race;

    /** Random seed of the race. */
    private long seed;

    /** How many times the weather has been re-rolled for this seed. */
    private int weatherRoll;

    /** The race's weather, drawn from the track's statistics. */
    private Forecast forecast;

    /** Number of cars. */
    private int carCount = Tuning.DEFAULT_CARS;

    /** The chosen strategy. */
    private StrategyChoice choice;

    /** The chosen baseline push level. */
    private PushLevel push = PushLevel.BALANCED;

    /** "You are driving..." line. */
    private final GLabel assignment;

    /** Conditions line. */
    private final GLabel conditionsLine;

    /** Qualifying preview rows. */
    private final GLabel[] gridRows = new GLabel[GRID_ROWS + 1];

    /** Two-compound rule check. */
    private final GLabel ruleLine;

    /** Race length details line. */
    private final GLabel lengthLine;

    /** Car count display. */
    private final GLabel carsLine;

    /** The Undo button. */
    private final UiButton undoButton;

    /**
     * A strategy choice: the preset and the compound for each stint.
     */
    private static final class StrategyChoice {

        /** The preset (stop laps). */
        private final StrategyPreset preset;

        /** Compound per stint. */
        private final Compound[] stints;

        /**
         * Creates a choice.
         *
         * @param preset the preset
         * @param stints compound per stint (copied)
         */
        StrategyChoice(StrategyPreset preset, Compound[] stints) {
            this.preset = preset;
            this.stints = stints.clone();
        }

        /**
         * Creates the preset's suggested choice.
         *
         * @param preset the preset
         * @return the choice
         */
        static StrategyChoice suggested(StrategyPreset preset) {
            Compound[] s = new Compound[preset.stintCount()];
            for (int i = 0; i < s.length; i++) {
                s[i] = preset.stintCompound(i);
            }
            return new StrategyChoice(preset, s);
        }

        /**
         * Builds the pit plan for this choice.
         *
         * @param laps race length in laps
         * @return a fresh plan
         */
        PitPlan toPlan(int laps) {
            PitPlan plan = preset.makePlan(laps);
            for (int i = 0; i < stints.length; i++) {
                plan.setStintCompound(i, stints[i]);
            }
            return plan;
        }
    }

    /**
     * Builds the screen with a fresh random race.
     *
     * @param manager the screen manager
     * @param track the track
     */
    public SetupScreen(ScreenManager manager, Track track) {
        this.manager = manager;
        this.track = track;
        this.seed = Math.floorMod(System.nanoTime(), 1000000L);
        this.choice = StrategyChoice.suggested(defaultPreset(length.getLaps()));
        GraphicsProgram p = manager.getProgram();
        double w = manager.getWidth();
        p.add(Palette.background(w, manager.getHeight()));
        p.add(Palette.label("RACE SETUP  " + track.getProfile().getName().toUpperCase(Locale.ROOT),
                Palette.HEADLINE, Palette.TEXT), 40, 52);
        p.add(Palette.label("Enter: start race   Esc: back   R: re-roll driver and grid   V: re-roll weather   U: undo",
                Palette.SMALL, Palette.TEXT_DIM), 42, 74);

        p.add(Palette.panel(560, 560), 40, 92);
        assignment = Palette.label("", Palette.HUD, Palette.PLAYER);
        p.add(assignment, 56, 122);
        conditionsLine = Palette.label("", Palette.SMALL, Palette.TEXT);
        p.add(conditionsLine, 56, 146);
        p.add(Palette.label("QUALIFYING (sets the grid)", Palette.HUD, Palette.TEXT_DIM), 56, 180);
        for (int i = 0; i < gridRows.length; i++) {
            gridRows[i] = Palette.label("", Palette.MONO, Palette.TEXT);
            p.add(gridRows[i], 56, 206 + i * 22);
        }
        UiButton reroll = new UiButton("Re-roll  (R)", 180, 36);
        p.add(reroll, 56, 600);
        buttons.add(reroll, () -> reroll());
        UiButton weatherButton = new UiButton("Re-roll weather  (V)", 220, 36);
        p.add(weatherButton, 252, 600);
        buttons.add(weatherButton, () -> rerollWeather());

        double x = 640;
        p.add(Palette.panel(w - x - 40, 560), x, 92);
        p.add(Palette.label("RACE LENGTH  (1, 2, 3)", Palette.HUD, Palette.TEXT_DIM), x + 16, 118);
        for (int i = 0; i < RaceLength.values().length; i++) {
            RaceLength option = RaceLength.values()[i];
            UiButton b = new UiButton(option.getDisplayName(), 180, 36);
            p.add(b, x + 16 + i * 192, 128);
            lengthButtons.add(b);
            buttons.add(b, () -> chooseLength(option));
        }
        lengthLine = Palette.label("", Palette.SMALL, Palette.TEXT_DIM);
        p.add(lengthLine, x + 16, 182);

        p.add(Palette.label("STRATEGY", Palette.HUD, Palette.TEXT_DIM), x + 16, 210);
        for (int i = 0; i < StrategyPreset.values().length; i++) {
            StrategyPreset preset = StrategyPreset.values()[i];
            UiButton b = new UiButton(preset.getDisplayName(), 138, 36);
            p.add(b, x + 16 + i * 144, 220);
            presetButtons.add(b);
            buttons.add(b, () -> choosePreset(preset));
        }
        p.add(Palette.label("Tires per stint (click to change):", Palette.SMALL, Palette.TEXT), x + 16, 276);
        for (int i = 0; i < 3; i++) {
            UiButton b = new UiButton("", 180, 36);
            p.add(b, x + 16 + i * 192, 286);
            stintButtons.add(b);
            int stint = i;
            buttons.add(b, () -> cycleStint(stint));
        }
        ruleLine = Palette.label("", Palette.SMALL, Palette.TEXT);
        p.add(ruleLine, x + 16, 342);
        undoButton = new UiButton("Undo  (U)", 140, 32);
        p.add(undoButton, x + 16, 352);
        buttons.add(undoButton, () -> undo());

        p.add(Palette.label("CARS", Palette.HUD, Palette.TEXT_DIM), x + 16, 410);
        UiButton fewer = new UiButton("-", 44, 36);
        UiButton more = new UiButton("+", 44, 36);
        p.add(fewer, x + 16, 420);
        p.add(more, x + 140, 420);
        carsLine = Palette.label("", Palette.HEADLINE, Palette.TEXT);
        p.add(carsLine, x + 76, 448);
        buttons.add(fewer, () -> changeCars(-1));
        buttons.add(more, () -> changeCars(1));

        p.add(Palette.label("BASELINE PUSH (also sets your default battle stance)", Palette.HUD, Palette.TEXT_DIM), x + 16, 496);
        String[] keys = {"Q", "W", "E"};
        for (int i = 0; i < PushLevel.values().length; i++) {
            PushLevel level = PushLevel.values()[i];
            UiButton b = new UiButton(keys[i] + "  " + level.getDisplayName(), 180, 36);
            p.add(b, x + 16 + i * 192, 506);
            pushButtons.add(b);
            buttons.add(b, () -> choosePush(level));
        }

        UiButton start = new UiButton("START RACE  (Enter)", 300, 48);
        p.add(start, x + 16, 590);
        buttons.add(start, () -> startRace());
        rebuildRace();
    }

    /**
     * Makes a new race preview with the current seed and car count, which runs qualifying again.
     */
    private void rebuildRace() {
        race = new Race(new RaceConfig(carCount, length.getLaps(), seed), track, manager.getRoster());
        drawForecast();
        refresh();
    }

    /**
     * Draws the race's weather from the track's statistics (a new draw for each seed and re-roll).
     */
    private void drawForecast() {
        WeatherStats stats = manager.weatherFor(track.getProfile().getId());
        if (stats == null) {
            forecast = null;
            return;
        }
        Random weatherRandom = new Random(seed * 1000 + weatherRoll);
        forecast = ForecastGenerator.generate(stats, track.getProfile(), length.getLaps(), weatherRandom);
    }

    /**
     * Draws new weather for the same driver and grid.
     */
    private void rerollWeather() {
        weatherRoll++;
        drawForecast();
        refresh();
    }

    /**
     * Draws a new driver and grid.
     */
    private void reroll() {
        seed = Math.floorMod(seed * 31 + 17 + System.nanoTime(), 1000000L);
        rebuildRace();
    }

    /**
     * Changes the number of cars within the allowed range.
     *
     * @param delta -1 or +1
     */
    private void changeCars(int delta) {
        carCount = Math.max(Tuning.MIN_CARS, Math.min(Tuning.MAX_CARS, carCount + delta));
        rebuildRace();
    }

    /**
     * Changes the race length. The strategy falls back to a preset that fits the new length, and the
     * race preview (and its qualifying) is rebuilt.
     *
     * @param option the new length
     */
    private void chooseLength(RaceLength option) {
        length = option;
        if (!StrategyPreset.forRace(length.getLaps()).contains(choice.preset)) {
            undo.clear();
            choice = StrategyChoice.suggested(defaultPreset(length.getLaps()));
        }
        rebuildRace();
    }

    /**
     * Returns the preset suggested for a race length: one-stop late when the two-compound rule
     * applies, otherwise no stop.
     *
     * @param laps race length in laps
     * @return the preset
     */
    private static StrategyPreset defaultPreset(int laps) {
        return RaceLength.compoundRuleApplies(laps) ? StrategyPreset.ONE_STOP_LATE : StrategyPreset.NO_STOP;
    }

    /**
     * Switches to a preset's suggested strategy (undoable).
     *
     * @param preset the preset
     */
    private void choosePreset(StrategyPreset preset) {
        if (!StrategyPreset.forRace(length.getLaps()).contains(preset)) {
            return;
        }
        undo.push(choice);
        choice = StrategyChoice.suggested(preset);
        refresh();
    }

    /**
     * Changes one stint to the next compound in the cycle (undoable).
     *
     * @param stint stint number
     */
    private void cycleStint(int stint) {
        if (stint >= choice.stints.length) {
            return;
        }
        undo.push(choice);
        Compound[] next = choice.stints.clone();
        next[stint] = CYCLE[(next[stint].ordinal() + 1) % CYCLE.length];
        choice = new StrategyChoice(choice.preset, next);
        refresh();
    }

    /**
     * Restores the previous strategy choice.
     */
    private void undo() {
        if (!undo.isEmpty()) {
            choice = undo.pop();
            refresh();
        }
    }

    /**
     * Sets the baseline push level.
     *
     * @param level the push level
     */
    private void choosePush(PushLevel level) {
        push = level;
        refresh();
    }

    /**
     * Hands the choices to the race and switches to the race screen.
     */
    private void startRace() {
        if (forecast != null) {
            race.applyForecast(forecast);
        }
        race.setPlayerPlan(choice.toPlan(length.getLaps()), push);
        manager.startRace(race);
    }

    /**
     * Updates every label and button highlight from the current choices.
     */
    private void refresh() {
        Car player = race.getPlayerCar();
        Driver d = player.getDriver();
        assignment.setLabel(String.format(Locale.ROOT, "You are driving #%d %s for %s, starting P%d.",
                d.getNumber(), d.getName(), d.getTeam().getName(), player.getGridPosition()));
        conditionsLine.setLabel(forecastText(forecast) + "   Seed " + seed);
        List<QualifyingEntry> quali = race.getQualifying();
        int playerRow = player.getGridPosition() - 1;
        for (int i = 0; i < gridRows.length; i++) {
            int row = i < GRID_ROWS ? i : playerRow;
            boolean show = i < GRID_ROWS ? i < quali.size() : playerRow >= GRID_ROWS;
            if (!show) {
                gridRows[i].setLabel("");
                continue;
            }
            QualifyingEntry e = quali.get(row);
            boolean mine = row == playerRow;
            gridRows[i].setLabel(String.format(Locale.ROOT, "P%-3d %s  %-22s %s", row + 1, e.getDriver().getCode(),
                    e.getDriver().getTeam().getName(), lapText(e.getLapTime())) + (mine ? "   <- you" : ""));
            gridRows[i].setColor(mine ? Palette.PLAYER : Palette.TEXT);
        }
        for (int i = 0; i < lengthButtons.size(); i++) {
            lengthButtons.get(i).setHighlighted(RaceLength.values()[i] == length);
        }
        lengthLine.setLabel(length.getLaps() + (length.getLaps() == 1 ? " lap" : " laps") + ", " + length.getDuration()
                + " at 4x.  Tires last " + Math.round(RaceLength.tireLifeScale(length.getLaps()) * 100)
                + "% as long as in a 10-lap race.");
        List<StrategyPreset> allowed = StrategyPreset.forRace(length.getLaps());
        for (int i = 0; i < presetButtons.size(); i++) {
            StrategyPreset preset = StrategyPreset.values()[i];
            presetButtons.get(i).setEnabled(allowed.contains(preset));
            presetButtons.get(i).setHighlighted(preset == choice.preset);
        }
        PitPlan plan = choice.toPlan(length.getLaps());
        for (int i = 0; i < stintButtons.size(); i++) {
            UiButton b = stintButtons.get(i);
            b.setVisible(i < choice.stints.length);
            if (i < choice.stints.length) {
                int first = i == 0 ? 1 : plan.getStops().get(i - 1).getLap() + 1;
                int last = i < plan.getStops().size() ? plan.getStops().get(i).getLap() : length.getLaps();
                b.setLabel(String.format(Locale.ROOT, "L%d-%d  %s", first, last, choice.stints[i].getDisplayName()));
            }
        }
        boolean ruleOn = RaceLength.compoundRuleApplies(length.getLaps());
        boolean legal = !ruleOn || plan.usesTwoDryCompounds();
        ruleLine.setLabel(!ruleOn ? "Two-compound rule: off in races under " + Tuning.COMPOUND_RULE_MIN_LAPS + " laps"
                : legal ? "Two-compound rule: OK (two different dry compounds)"
                : "Two-compound rule: BROKEN, you would be disqualified in a dry race");
        ruleLine.setColor(legal ? Palette.GOOD : Palette.DANGER);
        undoButton.setEnabled(!undo.isEmpty());
        carsLine.setLabel(String.valueOf(carCount));
        for (int i = 0; i < pushButtons.size(); i++) {
            pushButtons.get(i).setHighlighted(PushLevel.values()[i] == push);
        }
    }

    /**
     * Describes a forecast in one line, flagging approximate statistics.
     *
     * @param f the forecast, or null
     * @return the text
     */
    public static String forecastText(Forecast f) {
        if (f == null) {
            return "Forecast: no weather statistics, neutral dry conditions.";
        }
        String rain = f.getShownLap() > 0
                ? String.format(Locale.ROOT, "rain %.0f%%, around lap %d", f.getShownChance() * 100, f.getShownLap())
                : String.format(Locale.ROOT, "rain %.0f%%", f.getShownChance() * 100);
        return String.format(Locale.ROOT, "Forecast: track %.0f C, air %.0f C, %s%s", f.getTrackTemp(), f.getAirTemp(),
                rain, f.isApproximate() ? " (APPROXIMATE weather data)" : "");
    }

    /**
     * Formats a lap time as minutes:seconds.
     *
     * @param seconds lap time
     * @return text such as "1:24.812"
     */
    private static String lapText(double seconds) {
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
     * Keyboard shortcuts for the setup choices.
     *
     * @param keyCode the key
     * @param shift whether Shift was held
     */
    @Override
    public void handleKey(int keyCode, boolean shift) {
        switch (keyCode) {
            case KeyEvent.VK_ENTER:
                startRace();
                break;
            case KeyEvent.VK_ESCAPE:
                manager.showTrackSelect();
                break;
            case KeyEvent.VK_R:
                reroll();
                break;
            case KeyEvent.VK_U:
                undo();
                break;
            case KeyEvent.VK_V:
                rerollWeather();
                break;
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
                chooseLength(RaceLength.values()[keyCode - KeyEvent.VK_1]);
                break;
            case KeyEvent.VK_Q:
                choosePush(PushLevel.CONSERVE);
                break;
            case KeyEvent.VK_W:
                choosePush(PushLevel.BALANCED);
                break;
            case KeyEvent.VK_E:
                choosePush(PushLevel.PUSH);
                break;
            default:
                break;
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
