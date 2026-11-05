import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The race screen: puts the world, camera, timing tower, minimap, HUD and debug overlay together,
 * handles the race-screen keys, and decides how many simulation steps to run each frame.
 * Rendering is separate from simulation: the sim always steps in fixed 1/60 s chunks, and the
 * time-speed setting only changes how many of those chunks run per real second.
 */
public final class RaceScreen {

    /** Most simulation steps allowed in one frame, so a slow frame cannot snowball. */
    private static final int MAX_STEPS_PER_FRAME = 48;

    /** The program the objects are added to. */
    private final GraphicsProgram program;

    /** The race being shown. */
    private final Race race;

    /** The scrolling, zooming world. */
    private final WorldView world;

    /** The camera that moves the world. */
    private final Camera camera;

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

    /** True while paused. */
    private boolean paused;

    /** Unspent simulation time carried to the next frame, in seconds. */
    private double accumulator;

    /** True once the player asked for a new race. */
    private boolean restartRequested;

    /**
     * Builds the screen and adds everything to the program's canvas.
     *
     * @param program the ACM program
     * @param race the race to show
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     */
    public RaceScreen(GraphicsProgram program, Race race, double width, double height) {
        this.program = program;
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
        prompt.setLocation(TimingTower.WIDTH + 24, HudPanel.TOP_BAR_BOTTOM + 8);

        debug = new DebugOverlay();
        debug.setLocation(width - 308, 8);
        render();
    }

    /**
     * Works out how many fixed simulation steps to run for this frame.
     * Real time times the speed multiplier is added to an accumulator; whole steps are taken out.
     *
     * @param realSeconds real time since the previous frame
     * @return number of steps to run, 0 when paused
     */
    public int stepsForFrame(double realSeconds) {
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
        } else if (!battle.isResolved()) {
            promptClock += realSeconds;
            if (promptClock >= Tuning.BATTLE_DECISION_SECONDS) {
                battle.lockPlayerStance();
            }
            easeClock = 0;
            return Math.min(chosen, Tuning.BATTLE_SLOWDOWN);
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
            if (car != player && car.getIntent() == Stance.ATTACK && race.carAheadOnTrack(car) == player
                    && race.trackGap(car, player) < car.getSpeed() * Tuning.INTENT_GAP_SECONDS) {
                return car;
            }
        }
        return null;
    }

    /**
     * Handles a mouse click: a stance button on the battle prompt.
     *
     * @param x x on the canvas
     * @param y y on the canvas
     */
    public void handleClick(double x, double y) {
        if (promptShown) {
            Stance stance = prompt.stanceAt(x - prompt.getX(), y - prompt.getY());
            if (stance != null) {
                race.getBattles().choosePlayerStance(stance);
            }
        }
    }

    /**
     * Redraws everything from the current race state.
     */
    public void render() {
        camera.update(followed);
        world.updateCars(warnedRival());
        updatePrompt();
        radio.update(race.getMessages());
        tower.update();
        miniMap.update();
        hud.update(followed, Tuning.TIME_SPEEDS[speedIndex], paused);
        gauges.update(followed);
        hud.refreshBanner();
    }

    /**
     * Feeds the debug overlay with this frame's timing.
     *
     * @param frameSeconds real time this frame took
     * @param steps simulation steps run this frame
     */
    public void recordFrame(double frameSeconds, int steps) {
        debug.record(frameSeconds, steps, program.getGCanvas());
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
                restartRequested = true;
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
            case KeyEvent.VK_A:
                race.getBattles().choosePlayerStance(Stance.ATTACK);
                break;
            case KeyEvent.VK_S:
                race.getBattles().choosePlayerStance(Stance.DEFEND);
                break;
            case KeyEvent.VK_D:
                race.getBattles().choosePlayerStance(Stance.HOLD);
                break;
            case KeyEvent.VK_F3:
                toggleDebug();
                break;
            default:
                break;
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

    /**
     * Returns whether the player asked for a new race.
     *
     * @return true after R was pressed
     */
    public boolean isRestartRequested() {
        return restartRequested;
    }
}
