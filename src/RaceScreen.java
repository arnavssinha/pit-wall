import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The race screen: puts the world, camera, timing tower, minimap, HUD and debug overlay together,
 * handles the race-screen keys, and decides how many simulation steps to run each frame.
 * Rendering is separate from simulation: the sim always steps in fixed 1/60 s chunks, and the
 * time-speed setting only changes how many of those chunks run per real second.
 */
public final class RaceScreen implements Screen {

    /** Most simulation steps allowed in one frame, so a slow frame cannot snowball. */
    private static final int MAX_STEPS_PER_FRAME = 48;

    /** The screen manager, for switching screens. */
    private final ScreenManager manager;

    /** The program the objects are added to. */
    private final GraphicsProgram program;

    /** The race being shown. */
    private final Race race;

    /** The scrolling, zooming world. */
    private final WorldView world;

    /** The camera that moves the world. */
    private final Camera camera;

    /** Rain tint and streaks over the track. */
    private final WeatherOverlay weather;

    /** Real seconds since the previous frame, for the rain animation. */
    private double lastFrameSeconds;

    /** Timing tower on the left. */
    private final TimingTower tower;

    /** Minimap in the bottom-right corner. */
    private final MiniMap miniMap;

    /** HUD text. */
    private final HudPanel hud;

    /** Car gauges on the right. */
    private final GaugePanel gauges;

    /** Team radio on the right. */
    private final RadioFeed radio;

    /** The battle prompt. */
    private final BattlePrompt prompt;

    /** True while the prompt is on screen. */
    private boolean promptShown;

    /** The player battle the prompt is showing, or null. */
    private Battle shownBattle;

    /** Real seconds since the shown battle's prompt opened. */
    private double promptClock;

    /** Real seconds since the shown battle was resolved. */
    private double resultClock;

    /** Real seconds since time started easing back after a battle. */
    private double easeClock = Tuning.BATTLE_EASE_BACK_SECONDS;

    /** F3 debug overlay. */
    private final DebugOverlay debug;

    /** Car the camera follows. */
    private Car followed;

    /** Index into Tuning.TIME_SPEEDS. */
    private int speedIndex;

    /** True while the debug overlay is on screen. */
    private boolean debugShown;

    /** Real seconds since the race ended (the results follow after a short pause). */
    private double overClock;

    /** True while paused. */
    private boolean paused;

    /** Unspent simulation time carried to the next frame, in seconds. */
    private double accumulator;


    /**
     * Builds the screen and adds everything to the program's canvas.
     *
     * @param manager the screen manager
     * @param race the race to show
     */
    public RaceScreen(ScreenManager manager, Race race) {
        this.manager = manager;
        this.program = manager.getProgram();
        double width = manager.getWidth();
        double height = manager.getHeight();
        program.setBackground(Palette.GRASS);
        this.race = race;
        this.followed = race.getPlayerCar();
        this.speedIndex = Tuning.DEFAULT_TIME_SPEED_INDEX;

        TrackDrawing drawing = new TrackDrawing(race.getTrack());
        world = new WorldView(race, drawing);
        program.add(world);
        double viewLeft = TimingTower.WIDTH + 16;
        double viewTop = HudPanel.TOP_BAR_BOTTOM;
        double viewBottom = height - HudPanel.BOTTOM_PANEL_HEIGHT;
        camera = new Camera(world, drawing, viewLeft, viewTop, width - viewLeft, viewBottom - viewTop);

        weather = new WeatherOverlay(width, height);
        program.add(weather);

        tower = new TimingTower(race);
        tower.setLocation(8, 8);
        program.add(tower);

        double mapW = 250;
        double mapH = 190;
        miniMap = new MiniMap(race, mapW, mapH);
        miniMap.setLocation(width - mapW - 8, height - mapH - 8);
        program.add(miniMap);

        hud = new HudPanel(race, width, height);
        program.add(hud);

        gauges = new GaugePanel();
        gauges.setLocation(width - GaugePanel.WIDTH - 8, HudPanel.TOP_BAR_BOTTOM + 10);
        program.add(gauges);

        radio = new RadioFeed();
        radio.setLocation(width - RadioFeed.WIDTH - 8, HudPanel.TOP_BAR_BOTTOM + GaugePanel.HEIGHT + 18);
        program.add(radio);

        prompt = new BattlePrompt();
        prompt.setLocation(TimingTower.WIDTH + 24, viewBottom - BattlePrompt.HEIGHT - 6);
        camera.setPanelReserve(BattlePrompt.HEIGHT + 12);

        debug = new DebugOverlay();
        debug.setLocation(width - 308, 8);
        render();
    }

    /**
     * Runs one frame: works out how many fixed simulation steps this frame needs, runs them, redraws,
     * and feeds the debug overlay.
     *
     * @param realSeconds real time since the previous frame
     */
    @Override
    public void frame(double realSeconds) {
        if (race.isOver()) {
            overClock += realSeconds;
            if (overClock >= Tuning.RESULTS_DELAY_SECONDS) {
                manager.showResults(race);
                return;
            }
        }
        int steps = stepsForFrame(realSeconds);
        for (int i = 0; i < steps; i++) {
            race.step();
        }
        lastFrameSeconds = realSeconds;
        render();
        debug.record(realSeconds, steps, program.getGCanvas());
    }

    /**
     * Returns the race on this screen.
     *
     * @return the race
     */
    public Race getRace() {
        return race;
    }

    /**
     * Works out how many fixed simulation steps to run for this frame.
     * Real time times the speed multiplier is added to an accumulator; whole steps are taken out.
     *
     * @param realSeconds real time since the previous frame
     * @return number of steps to run, 0 when paused
     */
    private int stepsForFrame(double realSeconds) {
        if (paused) {
            accumulator = 0;
            return 0;
        }
        accumulator += realSeconds * effectiveSpeed(realSeconds);
        int steps = (int) Math.floor(accumulator / Tuning.SIM_STEP_SECONDS);
        if (steps > MAX_STEPS_PER_FRAME) {
            steps = MAX_STEPS_PER_FRAME;
            accumulator = 0;
        } else {
            accumulator -= steps * Tuning.SIM_STEP_SECONDS;
        }
        return steps;
    }

    /**
     * Works out this frame's time speed. While the player is deciding a battle, time runs at
     * Tuning.BATTLE_SLOWDOWN; afterwards it eases back to the chosen speed. Also runs the prompt's
     * countdown and locks the player's stance when time is up.
     *
     * @param realSeconds real time since the previous frame
     * @return the speed multiplier for this frame
     */
    private double effectiveSpeed(double realSeconds) {
        double chosen = Tuning.TIME_SPEEDS[speedIndex];
        Battle battle = race.getBattles().getPlayerBattle();
        if (battle != shownBattle) {
            shownBattle = battle;
            promptClock = 0;
            resultClock = 0;
        }
        if (battle == null) {
            easeClock += realSeconds;
        } else if (!battle.isResolved() && !battle.isPlayerDecided()) {
            promptClock += realSeconds;
            if (promptClock >= Tuning.BATTLE_DECISION_SECONDS) {
                Car player = race.getPlayerCar();
                race.getBattles().choosePlayerLine(
                        LineChoicePolicy.playerDefault(player, battle.getGapSeconds(), battle.isPlayerAttacking()));
                battle.markPlayerDecided();
            }
            easeClock = 0;
            return Math.min(chosen, Tuning.BATTLE_SLOWDOWN);
        } else if (!battle.isResolved()) {
            easeClock += realSeconds;
        } else {
            resultClock += realSeconds;
            easeClock += realSeconds;
            if (resultClock >= Tuning.BATTLE_RESULT_SECONDS) {
                race.getBattles().clearPlayerBattle();
            }
        }
        if (easeClock >= Tuning.BATTLE_EASE_BACK_SECONDS || chosen <= Tuning.BATTLE_SLOWDOWN) {
            return chosen;
        }
        double t = easeClock / Tuning.BATTLE_EASE_BACK_SECONDS;
        return Tuning.BATTLE_SLOWDOWN + (chosen - Tuning.BATTLE_SLOWDOWN) * t;
    }

    /**
     * Shows, refreshes or hides the battle prompt.
     */
    private void updatePrompt() {
        Battle battle = race.getBattles().getPlayerBattle();
        if (battle == null) {
            if (promptShown) {
                program.remove(prompt);
                promptShown = false;
            }
            return;
        }
        if (!promptShown) {
            program.add(prompt);
            promptShown = true;
        }
        if (battle.isResolved()) {
            prompt.showResult(battle);
        } else {
            prompt.showOpen(battle, race, Tuning.BATTLE_DECISION_SECONDS - promptClock);
        }
    }

    /**
     * Finds the rival that is about to attack the player: the car right behind, within the intent
     * gap, planning an Attack.
     *
     * @return the rival, or null
     */
    private Car warnedRival() {
        Car player = race.getPlayerCar();
        for (Car car : race.getCars()) {
            if (car != player && car.getIntent().leavesLine() && race.carAheadOnTrack(car) == player
                    && race.trackGap(car, player) < car.getSpeed() * Tuning.INTENT_GAP_SECONDS) {
                return car;
            }
        }
        return null;
    }

    /**
     * Handles a mouse click: a line button or the late braking switch on the battle panel.
     *
     * @param x x on the canvas
     * @param y y on the canvas
     */
    @Override
    public void handleClick(double x, double y) {
        if (promptShown) {
            double px = x - prompt.getX();
            double py = y - prompt.getY();
            LineChoice line = prompt.choiceAt(px, py);
            if (line != null) {
                race.getBattles().choosePlayerLine(line);
            } else if (prompt.isLateBrakeAt(px, py)) {
                race.getBattles().togglePlayerLateBrake();
            }
        }
    }

    /**
     * Redraws everything from the current race state.
     */
    private void render() {
        Battle battle = race.getBattles().getPlayerBattle();
        camera.update(followed, battle);
        world.updateCars(warnedRival(), followed);
        world.updatePreview();
        updatePrompt();
        radio.update(race.getMessages());
        weather.update(race.getConditions(), lastFrameSeconds);
        tower.update();
        miniMap.update();
        hud.update(followed, Tuning.TIME_SPEEDS[speedIndex], paused);
        gauges.update(followed, race);
        hud.refreshBanner();
    }

    /**
     * Returns the latest frames-per-second measurement.
     *
     * @return frames per second
     */
    public double getFps() {
        return debug.getFps();
    }

    /**
     * Reacts to a key press.
     *
     * @param keyCode the key, as a KeyEvent constant
     * @param shift whether Shift was held
     */
    @Override
    public void handleKey(int keyCode, boolean shift) {
        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                paused = !paused;
                break;
            case KeyEvent.VK_T:
                speedIndex = (speedIndex + 1) % Tuning.TIME_SPEEDS.length;
                break;
            case KeyEvent.VK_Z:
                camera.toggleOverview();
                break;
            case KeyEvent.VK_F:
                followNext(shift ? -1 : 1);
                break;
            case KeyEvent.VK_G:
                followed = race.getPlayerCar();
                break;
            case KeyEvent.VK_R:
                manager.showSetup(race.getTrack().getProfile().getId());
                break;
            case KeyEvent.VK_ESCAPE:
                manager.showMenu();
                break;
            case KeyEvent.VK_ENTER:
                if (race.getPlayerCar().isFinished() || race.getPlayerCar().isRetired()) {
                    race.runToEnd(Tuning.SKIP_TO_RESULTS_SECONDS);
                    manager.showResults(race);
                }
                break;
            case KeyEvent.VK_Q:
                race.getPlayerCar().setPushLevel(PushLevel.CONSERVE);
                break;
            case KeyEvent.VK_W:
                race.getPlayerCar().setPushLevel(PushLevel.BALANCED);
                break;
            case KeyEvent.VK_E:
                race.getPlayerCar().setPushLevel(PushLevel.PUSH);
                break;
            case KeyEvent.VK_X:
                race.getPlayerCar().setErsWanted(!race.getPlayerCar().isErsWanted());
                break;
            case KeyEvent.VK_B:
                togglePitCall(1);
                break;
            case KeyEvent.VK_N:
                togglePitCall(2);
                break;
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
                chooseCompound(Compound.values()[keyCode - KeyEvent.VK_1]);
                break;
            case KeyEvent.VK_A:
                race.getBattles().choosePlayerLine(LineChoice.INSIDE);
                break;
            case KeyEvent.VK_S:
                race.getBattles().choosePlayerLine(LineChoice.OPTIMAL);
                break;
            case KeyEvent.VK_O:
                race.getBattles().choosePlayerLine(LineChoice.OUTSIDE);
                break;
            case KeyEvent.VK_D:
                race.getBattles().choosePlayerLine(LineChoice.STAY_BEHIND);
                break;
            case KeyEvent.VK_L:
                race.getBattles().togglePlayerLateBrake();
                break;
            case KeyEvent.VK_C:
                race.getBattles().requestPlayerAttack();
                break;
            case KeyEvent.VK_F3:
                toggleDebug();
                break;
            default:
                break;
        }
    }

    /**
     * Calls or cancels a pit stop for the player. Pressing the key again cancels the call (as long as
     * the car has not reached the pit entry); cancelling the stop the plan called drops it from the plan.
     *
     * @param entries 1 to box this lap, 2 to box next lap
     */
    private void togglePitCall(int entries) {
        Car player = race.getPlayerCar();
        if (player.isInPitLane() || player.isFinished()) {
            return;
        }
        PitStop planned = player.getPlan().nextStop();
        if (player.getPitRequest() == entries) {
            player.cancelPit();
            if (planned != null && planned.getLap() <= player.getLapsCompleted() + 1) {
                player.getPlan().removeNextStop();
            }
            race.addMessage("Copy, staying out. Box when you want with B.", true);
            return;
        }
        Compound compound = player.getRequestedCompound();
        if (player.getPitRequest() == 0) {
            compound = planned != null ? planned.getCompound()
                    : player.getTire().getCompound() == Compound.MEDIUM ? Compound.HARD : Compound.MEDIUM;
        }
        player.requestPit(entries, compound);
        race.addMessage((entries == 1 ? "Box this lap" : "Box next lap") + " for " + compound.getDisplayName() + "s.", true);
    }

    /**
     * Picks the tires for the player's pit call, or for the next planned stop if no stop is called.
     *
     * @param compound the compound
     */
    private void chooseCompound(Compound compound) {
        Car player = race.getPlayerCar();
        if (player.getPitRequest() > 0 && !player.isInPitLane()) {
            player.setRequestedCompound(compound);
        } else if (player.getPlan().nextStop() != null) {
            player.getPlan().nextStop().setCompound(compound);
        }
    }

    /**
     * Moves the camera to the next or previous car in the standings.
     *
     * @param direction 1 for the car behind, -1 for the car ahead
     */
    private void followNext(int direction) {
        List<Car> standings = race.getStandings();
        int index = standings.indexOf(followed);
        followed = standings.get(Math.floorMod(index + direction, standings.size()));
    }

    /**
     * Shows or hides the debug overlay.
     */
    private void toggleDebug() {
        if (debugShown) {
            program.remove(debug);
        } else {
            program.add(debug);
        }
        debugShown = !debugShown;
    }

    /**
     * Shows the debug overlay (used by the self-test).
     */
    public void showDebug() {
        if (!debugShown) {
            program.add(debug);
            debugShown = true;
        }
    }

    /**
     * Sets the time speed directly (used by the self-test).
     *
     * @param index index into Tuning.TIME_SPEEDS
     */
    public void setSpeedIndex(int index) {
        speedIndex = index;
    }

}
