import acm.program.GraphicsProgram;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Main class: opens the window, runs the start lights and then the race at 60 frames per second.
 * Keys are queued by Java's input thread and handled by the game loop, so only one thread changes the game.
 */
public final class PitWall extends GraphicsProgram {

    /** Window width read by the ACM library. */
    public static final int APPLICATION_WIDTH = 1280;

    /** Window height read by the ACM library. */
    public static final int APPLICATION_HEIGHT = 742;

    /** Milliseconds per frame. */
    private static final double FRAME_MS = 1000.0 / 60;

    /** Time speeds the T key cycles through. */
    private static final int[] SPEEDS = {1, 2, 4};

    /** Speed of the slow motion from the start to Turn 1. */
    private static final double SLOW_MOTION = 0.4;

    /** When the player's car pulls away if SHIFT is not pressed, in seconds after lights out. */
    private static final double AUTO_LAUNCH = 1.0;

    /** When the player's car pulls away after a jump start. */
    private static final double JUMP_START_LAUNCH = 1.5;

    /** True when started with --selftest (race for a few seconds, print the frame rate, exit). */
    private static boolean selfTest;

    /** Key codes pressed since the last frame. */
    private final ArrayDeque<Integer> keys = new ArrayDeque<Integer>();

    /** The track (loaded once). */
    private Track track;

    /** The current race. */
    private Race race;

    /** The world drawing. */
    private TrackView view;

    /** The panels on top. */
    private Hud hud;

    /** Seconds since the start lights began (negative once racing). */
    private double lightsClock;

    /** When the lights go out, in seconds after they began. */
    private double lightsOut;

    /** True while paused. */
    private boolean paused;

    /** Index into SPEEDS. */
    private int speedIndex;

    /** Simulation time not yet stepped (time is stepped in fixed 1/60 s steps). */
    private double unstepped;

    /** Starts the program. */
    public static void main(String[] args) {
        selfTest = args.length > 0 && args[0].equals("--selftest");
        new PitWall().start(args);
    }

    /** Loads the track, starts a race and runs the game loop. */
    @Override
    public void run() {
        try {
            track = Track.load("data/interlagos.csv");
        } catch (IOException e) {
            add(new acm.graphics.GLabel("Could not load the track: " + e.getMessage(), 40, 40));
            return;
        }
        addKeyListeners();
        getGCanvas().setFocusable(true);
        getGCanvas().requestFocusInWindow();
        newRace();
        long start = System.nanoTime();
        int frames = 0;
        while (true) {
            long frameStart = System.nanoTime();
            frame();
            frames++;
            if (selfTest && frameStart - start > 8e9) {
                System.out.printf(Locale.ROOT, "SELFTEST OK: %.1f FPS, race time %.1f s%n", frames / 8.0, race.getTime());
                System.exit(0);
            }
            pause(Math.max(1, FRAME_MS - (System.nanoTime() - frameStart) / 1e6));
        }
    }

    /** Starts a new race with a new random seed. */
    private void newRace() {
        removeAll();
        race = new Race(track, System.nanoTime());
        race.player.launchAt = AUTO_LAUNCH;
        view = new TrackView(race);
        hud = new Hud(race, getWidth(), getHeight());
        add(view);
        add(hud);
        lightsClock = 0;
        lightsOut = 5 + 0.5 + Math.random() * 1.5;
        paused = false;
        speedIndex = selfTest ? 2 : 0;
        unstepped = 0;
    }

    /** One frame: handle keys, run the lights or step the race, then redraw. */
    private void frame() {
        while (true) {
            Integer key;
            synchronized (keys) {
                key = keys.poll();
            }
            if (key == null) {
                break;
            }
            handleKey(key);
        }
        int lightsOn = -1;
        if (lightsClock >= 0) {
            lightsClock += FRAME_MS / 1000;
            lightsOn = (int) Math.min(5, lightsClock);
            if (lightsClock >= lightsOut || selfTest) {
                lightsClock = -1;
                lightsOn = -1;
            }
        } else if (!paused) {
            boolean toTurnOne = race.player.laps == 0 && race.player.distance < race.track.toNextBrakePoint(0) + 150;
            unstepped += FRAME_MS / 1000 * (toTurnOne ? SLOW_MOTION : SPEEDS[speedIndex]);
            while (unstepped >= Race.STEP) {
                race.step();
                unstepped -= Race.STEP;
            }
        }
        view.update(getWidth(), getHeight());
        hud.update(lightsOn, paused, SPEEDS[speedIndex]);
    }

    /** Acts on one key press. */
    private void handleKey(int key) {
        Car p = race.player;
        if (key == KeyEvent.VK_R) {
            newRace();
        } else if (key == KeyEvent.VK_SHIFT && lightsClock >= 0) {
            p.launchAt = JUMP_START_LAUNCH;
            race.say("Jump start! Slow getaway.");
            lightsClock = lightsOut;
        } else if (key == KeyEvent.VK_SHIFT && p.launchAt == AUTO_LAUNCH && race.getTime() < AUTO_LAUNCH) {
            p.launchAt = race.getTime();
        } else if (key == KeyEvent.VK_SHIFT) {
            race.playerOvertake();
        } else if (key == KeyEvent.VK_A) {
            race.playerLine(Fight.Line.INSIDE);
        } else if (key == KeyEvent.VK_S) {
            race.playerLine(Fight.Line.OPTIMAL);
        } else if (key == KeyEvent.VK_D) {
            race.playerLine(Fight.Line.OUTSIDE);
        } else if (key == KeyEvent.VK_Q) {
            p.mode = Car.Mode.CONSERVE;
        } else if (key == KeyEvent.VK_W) {
            p.mode = Car.Mode.NORMAL;
        } else if (key == KeyEvent.VK_E) {
            p.mode = Car.Mode.PUSH;
        } else if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_5) {
            p.boxFor = Compound.values()[key - KeyEvent.VK_1];
        } else if (key == KeyEvent.VK_B) {
            p.boxFor = null;
        } else if (key == KeyEvent.VK_P || key == KeyEvent.VK_SPACE) {
            paused = !paused;
        } else if (key == KeyEvent.VK_T) {
            speedIndex = (speedIndex + 1) % SPEEDS.length;
        }
    }

    /** Queues a key press for the game loop (called on Java's input thread). */
    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (keys) {
            keys.add(e.getKeyCode());
        }
    }
}
