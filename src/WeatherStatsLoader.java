import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Reads weather statistics by track id. It prefers data/weather_stats.csv (the real FastF1 export)
 * and falls back to data/fallback_weather.csv, whose values are approximate and flagged as such.
 */
public final class WeatherStatsLoader {

    /** Real statistics from the data export. */
    public static final String MEASURED_PATH = "data/weather_stats.csv";

    /** Approximate fallback statistics. */
    public static final String FALLBACK_PATH = "data/fallback_weather.csv";

    /**
     * Not instantiated; all methods are static.
     */
    private WeatherStatsLoader() {
    }

    /**
     * Loads the statistics for every track, measured where available.
     *
     * @return statistics keyed by track id
     * @throws IOException if neither file can be read
     */
    public static HashMap<String, WeatherStats> load() throws IOException {
        HashMap<String, WeatherStats> stats = new HashMap<String, WeatherStats>();
        read(FALLBACK_PATH, true, stats);
        try {
            read(MEASURED_PATH, false, stats);
        } catch (IOException e) {
            return stats;
        }
        return stats;
    }

    /**
     * Reads one statistics file into the map, replacing any earlier entry for the same track.
     *
     * @param path file path
     * @param approximate whether the file holds approximate values
     * @param stats the map to fill
     * @throws IOException if the file is missing or malformed
     */
    private static void read(String path, boolean approximate, HashMap<String, WeatherStats> stats) throws IOException {
        try (BufferedReader reader = DataFiles.open(path)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (DataFiles.isSkippable(line) || line.startsWith("trackId")) {
                    continue;
                }
                String[] p = line.split(",");
                try {
                    stats.put(p[0].trim(), new WeatherStats(p[0].trim(), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                            Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                            Integer.parseInt(p[6].trim()), approximate));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    throw new IOException(path + " line " + lineNumber + " is malformed: " + line, e);
                }
            }
        }
    }
}
