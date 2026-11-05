import acm.graphics.GLabel;
import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

/**
 * Main class of Pit Wall, an F1 race strategy game drawn with the ACM graphics library.
 * It loads the data, builds the race screen and runs the main loop: read keys, run the
 * simulation steps this frame needs, redraw, then pause until the next frame (60 per second).
 * Start with --selftest to race for ten seconds, print frame rates and exit.
 */
public final class PitWall extends GraphicsProgram {

    /** Version tag required because ACM programs are serializable. */
    private static final long serialVersionUID = 1L;

    /** Window width read by the ACM library. */
    public static final int APPLICATION_WIDTH = 1280;

    /** Window height read by the ACM library; includes about 21 pixels for the ACM menu bar. */
    public static final int APPLICATION_HEIGHT = 742;

    /** Track used until the track select screen exists (M5). */
    private static final String TRACK_ID = "monza";

    /** Target frame time in milliseconds (60 frames per second). */
    private static final double FRAME_MS = 1000.0 / 60.0;

    /** Seconds the self-test spends at each time speed it measures. */
    private static final double SELFTEST_SECONDS_PER_SPEED = 5.0;

    /** Marker in the input queue for a mouse click (no real key has this code). */
    private static final int CLICK = -1;

    /** True when started with --selftest. */
    private static boolean selfTest;

    /**
     * Key presses and mouse clicks since the last frame. Filled by the input thread, emptied by the
     * game loop. A key is {code, shift}; a click is {CLICK, x, y}.
     */
    private final transient ArrayDeque<int[]> pendingKeys = new ArrayDeque<int[]>();

    /** The track, loaded once. */
    private transient Track track;

    /** All drivers, loaded once. */
    private transient List<Driver> roster;

    /** The race currently on screen. */
    private transient Race currentRace;

    /** The current race screen. */
    private transient RaceScreen screen;

    /**
     * Creates the program. The ACM library builds the window in start().
     */
    public PitWall() {
        super();
    }

    /**
     * Loads data and runs the game loop until the window is closed.
     */
    @Override
    public void run() {
        setBackground(Palette.GRASS);
        try {
            track = TrackLoader.load(TRACK_ID);
            roster = RosterLoader.load();
        } catch (IOException e) {
            showError("Could not load game data: " + e.getMessage());
            return;
        }
        addKeyListeners();
        addMouseListeners();
        getGCanvas().setFocusable(true);
        getGCanvas().requestFocusInWindow();
        startRace(newSeed());
        try {
            loop();
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (selfTest) {
                System.out.println("SELFTEST FAILED: " + e);
                System.exit(1);
            }
            showError("Something went wrong: " + e);
        }
    }

    /**
     * The main loop: one pass per frame.
     */
    private void loop() {
        long last = System.nanoTime();
        long selfTestStart = last;
        int selfTestPhase = 0;
        if (selfTest) {
            screen.showDebug();
        }
        while (true) {
            long now = System.nanoTime();
            double frameSeconds = (now - last) / 1e9;
            last = now;

            handlePendingKeys();
            if (screen.isRestartRequested()) {
                startRace(newSeed());
            }
            int steps = screen.stepsForFrame(frameSeconds);
            Race race = currentRace;
            for (int i = 0; i < steps; i++) {
                race.step();
            }
            screen.render();
            screen.recordFrame(frameSeconds, steps);

            if (selfTest) {
                double elapsed = (System.nanoTime() - selfTestStart) / 1e9;
                if (elapsed > SELFTEST_SECONDS_PER_SPEED * (selfTestPhase + 1)) {
                    int speed = Tuning.TIME_SPEEDS[selfTestPhase == 0 ? Tuning.DEFAULT_TIME_SPEED_INDEX : Tuning.TIME_SPEEDS.length - 1];
                    System.out.printf(Locale.ROOT, "SELFTEST %dx: %.1f FPS, race time %.1f s, objects %d%n",
                            speed, screen.getFps(), race.getTime(), DebugOverlay.countObjects(getGCanvas()));
                    selfTestPhase++;
                    screen.setSpeedIndex(Tuning.TIME_SPEEDS.length - 1);
                    if (selfTestPhase == 2) {
                        System.out.println("SELFTEST OK: no exceptions");
                        System.exit(0);
                    }
                }
            }

            double spentMs = (System.nanoTime() - now) / 1e6;
            pause(Math.max(1.0, FRAME_MS - spentMs));
        }
    }

    /**
     * Clears the canvas and starts a fresh race with a new seed.
     *
     * @param seed random seed for the race
     */
    private void startRace(long seed) {
        removeAll();
        currentRace = new Race(new RaceConfig(Tuning.DEFAULT_CARS, Tuning.RACE_LAPS, seed), track, roster, Compound.MEDIUM);
        screen = new RaceScreen(this, currentRace, getWidth(), getHeight());
    }

    /**
     * Makes a short random seed (up to six digits) that is easy to read and type.
     *
     * @return the seed
     */
    private static long newSeed() {
        return Math.floorMod(System.nanoTime(), 1000000L);
    }

    /**
     * Passes queued key presses to the screen. Runs on the game loop thread.
     */
    private void handlePendingKeys() {
        while (true) {
            int[] key;
            synchronized (pendingKeys) {
                key = pendingKeys.poll();
            }
            if (key == null) {
                return;
            }
            if (key[0] == CLICK) {
                screen.handleClick(key[1], key[2]);
            } else {
                screen.handleKey(key[0], key[1] == 1);
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
        synchronized (pendingKeys) {
            pendingKeys.add(new int[] {e.getKeyCode(), e.isShiftDown() ? 1 : 0});
        }
    }

    /**
     * Queues a mouse click for the game loop, like key presses.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        synchronized (pendingKeys) {
            pendingKeys.add(new int[] {CLICK, e.getX(), e.getY()});
        }
    }

    /**
     * Shows an error message on the canvas, because players who double-click the jar see no console.
     *
     * @param message the message
     */
    private void showError(String message) {
        removeAll();
        GLabel label = new GLabel(message, 30, 60);
        label.setFont(Palette.HUD);
        label.setColor(Palette.TEXT);
        add(label);
        System.err.println(message);
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
