import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * Writes Strategy Lab results to a CSV file: one row per race, then one summary row per strategy.
 * The file goes in the user's home folder, because the game's own folder may be read-only.
 */
public final class LabCsvWriter {

    /**
     * Not instantiated; all methods are static.
     */
    private LabCsvWriter() {
    }

    /**
     * Returns where the CSV for a track is written.
     *
     * @param trackId the track
     * @return the file
     */
    public static File fileFor(String trackId) {
        return new File(System.getProperty("user.home"), "PitWall-lab-" + trackId + ".csv");
    }

    /**
     * Writes the results to the standard file for a track.
     *
     * @param result the lab result
     * @param trackId the track
     * @return the full path of the file written
     * @throws IOException if the file cannot be written
     */
    public static String writeForTrack(LabResult result, String trackId) throws IOException {
        File file = fileFor(trackId);
        write(result, file);
        return file.getAbsolutePath();
    }

    /**
     * Writes the results.
     *
     * @param result the lab result
     * @param file the file to write
     * @throws IOException if the file cannot be written
     */
    public static void write(LabResult result, File file) throws IOException {
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            out.println("# Pit Wall Strategy Lab. Weather comes from the track statistics (approximate until the data export).");
            out.println("strategy,seed,grid,finish,places_gained,status,rained,track_temp_c");
            for (LabRun r : result.getRuns()) {
                out.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%s,%s,%.1f%n", r.getVariant().getName(), r.getSeed(), r.getGrid(),
                        r.getFinish(), r.placesGained(), r.getStatus(), r.isRained(), r.getTrackTemp());
            }
            out.println();
            out.println("strategy,races,win_pct,podium_pct,avg_finish,avg_places_gained");
            for (StrategyVariant v : result.getVariants()) {
                List<LabRun> runs = result.runsOf(v, 0);
                out.printf(Locale.ROOT, "%s,%d,%.1f,%.1f,%.2f,%.2f%n", v.getName(), runs.size(),
                        LabResult.shareInTop(runs, 1) * 100, LabResult.shareInTop(runs, 3) * 100,
                        LabResult.averageFinish(runs), LabResult.averageGained(runs));
            }
        }
    }
}
