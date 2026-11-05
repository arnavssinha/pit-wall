import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Reads a track from data/tracks: the profile from (id).properties and the centerline from
 * (id)_centerline.csv. The centerline is rotated so it starts at the start/finish line.
 */
public final class TrackLoader {

    /**
     * Not instantiated; all methods are static.
     */
    private TrackLoader() {
    }

    /**
     * Loads one track.
     *
     * @param id track id, such as "monza"
     * @return the built track
     * @throws IOException if a file is missing or a value cannot be read
     */
    public static Track load(String id) throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = DataFiles.open("data/tracks/" + id + ".properties")) {
            props.load(reader);
        }
        TrackProfile profile = new TrackProfile(
                id,
                required(props, "name", id),
                required(props, "country", id),
                number(props, "lapLengthMeters", id),
                number(props, "medianRaceLapSeconds", id),
                number(props, "trackWearMultiplier", id),
                required(props, "difficultyLabel", id),
                required(props, "description", id),
                required(props, "dataSource", id));
        int startIndex = (int) number(props, "startFinishIndex", id);
        List<double[]> points = readCenterline("data/tracks/" + id + "_centerline.csv");
        if (startIndex < 0 || startIndex >= points.size()) {
            throw new IOException("startFinishIndex " + startIndex + " is outside the centerline for " + id);
        }
        List<double[]> fromLine = new ArrayList<double[]>();
        for (int i = 0; i < points.size(); i++) {
            fromLine.add(points.get((startIndex + i) % points.size()));
        }
        return new Track(profile, fromLine);
    }

    /**
     * Reads "x,y" rows in meters, skipping comments and the header row.
     *
     * @param path file path inside the project
     * @return the points in driving order
     * @throws IOException if the file is missing or a row is malformed
     */
    private static List<double[]> readCenterline(String path) throws IOException {
        List<double[]> points = new ArrayList<double[]>();
        try (BufferedReader reader = DataFiles.open(path)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (DataFiles.isSkippable(line) || line.startsWith("x")) {
                    continue;
                }
                String[] parts = line.split(",");
                try {
                    points.add(new double[] {Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())});
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    throw new IOException(path + " line " + lineNumber + " is not an x,y pair: " + line, e);
                }
            }
        }
        return points;
    }

    /**
     * Reads a required text value.
     *
     * @param props the loaded properties
     * @param key the key
     * @param id track id, for the error message
     * @return the value
     * @throws IOException if the key is missing
     */
    private static String required(Properties props, String key, String id) throws IOException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("Track " + id + " is missing '" + key + "'");
        }
        return value.trim();
    }

    /**
     * Reads a required number.
     *
     * @param props the loaded properties
     * @param key the key
     * @param id track id, for the error message
     * @return the value
     * @throws IOException if the key is missing or not a number
     */
    private static double number(Properties props, String key, String id) throws IOException {
        String value = required(props, key, id);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IOException("Track " + id + ": '" + key + "' is not a number: " + value, e);
        }
    }
}
