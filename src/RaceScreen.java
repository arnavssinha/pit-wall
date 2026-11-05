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

        world = new WorldView(race);
        program.add(world);
        double viewLeft = TimingTower.WIDTH + 16;
        double viewTop = HudPanel.TOP_BAR_BOTTOM;
        double viewBottom = height - HudPanel.BOTTOM_PANEL_HEIGHT;
        camera = new Camera(world, race.getTrack(), viewLeft, viewTop, width - viewLeft, viewBottom - viewTop);

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
        accumulator += realSeconds * Tuning.TIME_SPEEDS[speedIndex];
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
     * Redraws everything from the current race state.
     */
    public void render() {
        camera.update(followed);
        world.updateCars();
        tower.update();
        miniMap.update();
        hud.update(followed, Tuning.TIME_SPEEDS[speedIndex], paused);
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
