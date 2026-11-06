import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Main class of Pit Wall, an F1 race strategy game drawn with the ACM graphics library.
 * It loads the data, opens the menu and runs the main loop: pass queued keys and clicks to the
 * current screen, let the screen run one frame, then pause until the next frame (60 per second).
 * Start with --selftest to race for ten seconds, print frame rates and exit.
 */
public final class PitWall extends GraphicsProgram {

    /** Version tag required because ACM programs are serializable. */
    private static final long serialVersionUID = 1L;

    /** Window width read by the ACM library. */
    public static final int APPLICATION_WIDTH = 1280;

    /** Window height read by the ACM library; includes about 21 pixels for the ACM menu bar. */
    public static final int APPLICATION_HEIGHT = 742;

    /** Target frame time in milliseconds (60 frames per second). */
    private static final double FRAME_MS = 1000.0 / 60.0;

    /** Seconds the self-test spends at each time speed it measures. */
    private static final double SELFTEST_SECONDS_PER_SPEED = 5.0;

    /** Race time when the self-test's rain starts, in seconds. */
    private static final double SELFTEST_RAIN_START = 5.0;

    /** How long the self-test's rain lasts, in seconds. */
    private static final double SELFTEST_RAIN_SECONDS = 120.0;

    /** Race time when a car stops in the self-test, bringing out the safety car. */
    private static final double SELFTEST_SAFETY_CAR = 15.0;

    /** Marker in the input queue for a mouse click (no real key has this code). */
    private static final int CLICK = -1;

    /** True when started with --selftest. */
    private static boolean selfTest;

    /**
     * Key presses and mouse clicks since the last frame. Filled by the input thread, emptied by the
     * game loop. A key is {code, shift}; a click is {CLICK, x, y}.
     */
    private final transient ArrayDeque<int[]> pendingInput = new ArrayDeque<int[]>();

    /** Switches between screens. */
    private transient ScreenManager screens;

    /**
     * Creates the program. The ACM library builds the window in start().
     */
    public PitWall() {
        super();
    }

    /**
     * Loads data, shows the menu (or starts the self-test race) and runs the game loop.
     */
    @Override
    public void run() {
        setBackground(Palette.SCREEN_BACK);
        List<Driver> roster;
        List<String> trackIds;
        HashMap<String, WeatherStats> weather;
        try {
            roster = RosterLoader.load();
            trackIds = TrackLoader.listTrackIds();
            weather = WeatherStatsLoader.load();
        } catch (IOException e) {
            screens = new ScreenManager(this, getWidth(), getHeight(), null, null, null);
            screens.showError("Could not load game data: " + e.getMessage());
            return;
        }
        screens = new ScreenManager(this, getWidth(), getHeight(), roster, trackIds, weather);
        addKeyListeners();
        addMouseListeners();
        getGCanvas().setFocusable(true);
        getGCanvas().requestFocusInWindow();
        try {
            if (selfTest) {
                runSelfTest(trackIds.get(0));
            } else {
                screens.showMenu();
                loop();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (selfTest) {
                System.out.println("SELFTEST FAILED: " + e);
                System.exit(1);
            }
            screens.showError("Something went wrong: " + e);
        }
    }

    /**
     * The main loop: one pass per frame, forever.
     */
    private void loop() {
        long last = System.nanoTime();
        while (true) {
            long now = System.nanoTime();
            double frameSeconds = (now - last) / 1e9;
            last = now;
            runFrame(frameSeconds);
            double spentMs = (System.nanoTime() - now) / 1e6;
            pause(Math.max(1.0, FRAME_MS - spentMs));
        }
    }

    /**
     * Runs one frame of the current screen after handing it the queued input.
     *
     * @param frameSeconds real time since the previous frame
     */
    private void runFrame(double frameSeconds) {
        handlePendingInput();
        Screen current = screens.getCurrent();
        if (current != null) {
            current.frame(frameSeconds);
        }
    }

    /**
     * Races for a few seconds at 2x and then at 8x with the debug overlay on, prints the frame rate
     * for each, and exits. Rain and a safety car are scheduled early so the weather and incident
     * code is exercised with drawing on.
     *
     * @param trackId the track to race on
     */
    private void runSelfTest(String trackId) {
        RaceScreen race = screens.startQuickRace(trackId, 1L);
        Race sim = race.getRace();
        sim.setRain(new RainEvent(SELFTEST_RAIN_START, SELFTEST_RAIN_SECONDS, 1.0));
        Car stopper = sim.getCars().get(sim.getCars().size() - 1);
        if (stopper != sim.getPlayerCar()) {
            sim.scheduleSafetyCar(SELFTEST_SAFETY_CAR, stopper);
        }
        race.showDebug();
        race.skipStartLights();
        int[] speeds = {Tuning.DEFAULT_TIME_SPEED_INDEX, Tuning.TIME_SPEEDS.length - 1};
        for (int speedIndex : speeds) {
            race.setSpeedIndex(speedIndex);
            long start = System.nanoTime();
            long last = start;
            while ((System.nanoTime() - start) / 1e9 < SELFTEST_SECONDS_PER_SPEED) {
                long now = System.nanoTime();
                runFrame((now - last) / 1e9);
                last = now;
                pause(Math.max(1.0, FRAME_MS - (System.nanoTime() - now) / 1e6));
            }
            System.out.printf(Locale.ROOT, "SELFTEST %dx: %.1f FPS, race time %.1f s, objects %d%n",
                    Tuning.TIME_SPEEDS[speedIndex], race.getFps(), race.getRace().getTime(),
                    DebugOverlay.countObjects(getGCanvas()));
        }
        System.out.println("SELFTEST OK: no exceptions");
        System.exit(0);
    }

    /**
     * Passes queued key presses and clicks to the current screen. Runs on the game loop thread.
     */
    private void handlePendingInput() {
        while (true) {
            int[] input;
            synchronized (pendingInput) {
                input = pendingInput.poll();
            }
            Screen current = screens.getCurrent();
            if (input == null || current == null) {
                return;
            }
            if (input[0] == CLICK) {
                current.handleClick(input[1], input[2]);
            } else {
                current.handleKey(input[0], input[1] == 1);
            }
        }
    }

    /**
     * Queues a key press. Called by Java's input thread, so it only records the key;
     * the game loop acts on it, which keeps all game changes on one thread.
     *
     * @param e the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (pendingInput) {
            pendingInput.add(new int[] {e.getKeyCode(), e.isShiftDown() ? 1 : 0});
        }
    }

    /**
     * Queues a mouse click for the game loop, like key presses.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        synchronized (pendingInput) {
            pendingInput.add(new int[] {CLICK, e.getX(), e.getY()});
        }
    }

    /**
     * Starts the game.
     *
     * @param args command-line arguments; --selftest runs the timed self-test
     */
    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--selftest")) {
                selfTest = true;
            }
        }
        new PitWall().start(new String[0]);
    }
}
