import acm.graphics.GLabel;
import acm.program.GraphicsProgram;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Switches between the game's screens: Menu, Track Select, Setup and Race.
 * It owns the shared things every screen needs (the program, the canvas size, the roster and the
 * loaded tracks). Tracks are loaded once and kept in a HashMap by id.
 */
public final class ScreenManager {

    /** The ACM program everything is drawn on. */
    private final GraphicsProgram program;

    /** Canvas width in pixels. */
    private final double width;

    /** Canvas height in pixels. */
    private final double height;

    /** Every driver. */
    private final List<Driver> roster;

    /** Ids of the available tracks, in menu order. */
    private final List<String> trackIds;

    /** Tracks already loaded, by id. */
    private final HashMap<String, Track> tracks = new HashMap<String, Track>();

    /** Weather statistics by track id. */
    private final HashMap<String, WeatherStats> weather;

    /** The screen on display. */
    private Screen current;

    /**
     * Creates the manager.
     *
     * @param program the ACM program
     * @param width canvas width
     * @param height canvas height
     * @param roster every driver
     * @param trackIds available track ids
     * @param weather weather statistics by track id
     */
    public ScreenManager(GraphicsProgram program, double width, double height, List<Driver> roster,
                         List<String> trackIds, HashMap<String, WeatherStats> weather) {
        this.program = program;
        this.width = width;
        this.height = height;
        this.roster = roster;
        this.trackIds = trackIds;
        this.weather = weather;
    }

    /**
     * Returns the weather statistics for a track.
     *
     * @param trackId the track
     * @return the statistics, or null if there are none
     */
    public WeatherStats weatherFor(String trackId) {
        return weather == null ? null : weather.get(trackId);
    }

    /**
     * Shows the main menu.
     */
    public void showMenu() {
        program.removeAll();
        current = new MenuScreen(this);
    }

    /**
     * Shows the track select screen.
     */
    public void showTrackSelect() {
        program.removeAll();
        current = new TrackSelectScreen(this);
    }

    /**
     * Shows the setup screen for a track.
     *
     * @param trackId the track
     */
    public void showSetup(String trackId) {
        Track track = track(trackId);
        if (track != null) {
            program.removeAll();
            current = new SetupScreen(this, track);
        }
    }

    /**
     * Starts a race on the race screen.
     *
     * @param race a race that is set up and not yet started
     * @return the race screen
     */
    public RaceScreen startRace(Race race) {
        program.removeAll();
        RaceScreen screen = new RaceScreen(this, race);
        current = screen;
        return screen;
    }

    /**
     * Shows the results of a finished race.
     *
     * @param race the race
     */
    public void showResults(Race race) {
        program.removeAll();
        current = new ResultsScreen(this, race);
    }

    /**
     * Shows the Strategy Lab for a track.
     *
     * @param trackId the track
     * @param lastRace the player's last race report on this track, or null
     */
    public void showLab(String trackId, RaceReport lastRace) {
        Track track = track(trackId);
        if (track != null) {
            program.removeAll();
            current = new LabScreen(this, track, lastRace);
        }
    }

    /**
     * Starts a race straight away with default settings (used by the self-test).
     *
     * @param trackId the track
     * @param seed the random seed
     * @return the race screen, or null if the track could not be loaded
     */
    public RaceScreen startQuickRace(String trackId, long seed) {
        Track track = track(trackId);
        if (track == null) {
            return null;
        }
        return startRace(new Race(new RaceConfig(Tuning.DEFAULT_CARS, Tuning.RACE_LAPS, seed), track, roster));
    }

    /**
     * Returns a track, loading it the first time. Shows an error on screen if it cannot be loaded.
     *
     * @param id the track id
     * @return the track, or null on error
     */
    public Track track(String id) {
        Track track = tracks.get(id);
        if (track == null) {
            try {
                track = TrackLoader.load(id);
                tracks.put(id, track);
            } catch (IOException e) {
                showError("Could not load track " + id + ": " + e.getMessage());
            }
        }
        return track;
    }

    /**
     * Clears the screen and shows an error message (players who double-click the jar see no console).
     *
     * @param message the message
     */
    public void showError(String message) {
        program.removeAll();
        GLabel label = new GLabel(message, 30, 60);
        label.setFont(Palette.HUD);
        label.setColor(Palette.TEXT);
        program.add(label);
        System.err.println(message);
        current = null;
    }

    /**
     * Returns the screen on display.
     *
     * @return the current screen, or null after an error
     */
    public Screen getCurrent() {
        return current;
    }

    /**
     * Returns the ACM program.
     *
     * @return the program
     */
    public GraphicsProgram getProgram() {
        return program;
    }

    /**
     * Returns the canvas width.
     *
     * @return pixels
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the canvas height.
     *
     * @return pixels
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns every driver.
     *
     * @return the roster
     */
    public List<Driver> getRoster() {
        return roster;
    }

    /**
     * Returns the available track ids.
     *
     * @return ids in menu order
     */
    public List<String> getTrackIds() {
        return trackIds;
    }
}
