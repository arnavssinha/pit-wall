import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Reads data/roster.csv into drivers and teams.
 * Each row is number,code,name,team,teamColor. Teams are shared: the HashMap makes sure both
 * drivers of a team point at the same Team object.
 */
public final class RosterLoader {

    /** Location of the roster file inside the project. */
    public static final String ROSTER_PATH = "data/roster.csv";

    /**
     * Not instantiated; all methods are static.
     */
    private RosterLoader() {
    }

    /**
     * Loads every driver.
     *
     * @return drivers in file order
     * @throws IOException if the file is missing or a row is malformed
     */
    public static List<Driver> load() throws IOException {
        List<Driver> drivers = new ArrayList<Driver>();
        HashMap<String, Team> teams = new HashMap<String, Team>();
        try (BufferedReader reader = DataFiles.open(ROSTER_PATH)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (DataFiles.isSkippable(line) || line.startsWith("number")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 5) {
                    throw new IOException(ROSTER_PATH + " line " + lineNumber + " needs 5 fields: " + line);
                }
                String teamName = parts[3].trim();
                Team team = teams.get(teamName);
                if (team == null) {
                    team = new Team(teamName, parts[4].trim());
                    teams.put(teamName, team);
                }
                try {
                    drivers.add(new Driver(Integer.parseInt(parts[0].trim()), parts[1].trim(), parts[2].trim(), team));
                } catch (NumberFormatException e) {
                    throw new IOException(ROSTER_PATH + " line " + lineNumber + " has a bad car number", e);
                }
            }
        }
        return drivers;
    }
}
