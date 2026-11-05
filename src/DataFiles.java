import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Opens the game's data files wherever they are.
 * When the game runs from PitWall.jar the data folder is packed inside the jar, so files are
 * read as classpath resources; that works no matter which folder the jar was started from.
 * When running from an IDE, the files are read from a data folder in the working directory
 * or its parent.
 */
public final class DataFiles {

    /**
     * Not instantiated; all methods are static.
     */
    private DataFiles() {
    }

    /**
     * Opens a data file for reading as UTF-8 text.
     *
     * @param path path inside the project, such as "data/roster.csv"
     * @return a reader; the caller must close it
     * @throws IOException if the file cannot be found in the jar or on disk
     */
    public static BufferedReader open(String path) throws IOException {
        InputStream in = DataFiles.class.getResourceAsStream("/" + path);
        if (in == null) {
            String[] folders = {"", "../"};
            for (String folder : folders) {
                File file = new File(folder + path);
                if (file.isFile()) {
                    in = new FileInputStream(file);
                    break;
                }
            }
        }
        if (in == null) {
            throw new IOException("Data file not found: " + path
                    + " (run PitWall.jar, or start the game from the project folder)");
        }
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Returns whether a line is blank or a comment (starts with #) and should be skipped.
     *
     * @param line a line from a data file
     * @return true if the line holds no data
     */
    public static boolean isSkippable(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }
}
