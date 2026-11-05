import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The DRS zones of a track. DRS (drag reduction system) opens a flap in the rear wing on a long
 * straight, so the car goes faster. As in real F1, a car may only use it in a zone if it was less
 * than Tuning.DRS_GAP_SECONDS behind another car at that zone's detection point.
 * Zones are found automatically: every straight at least Tuning.DRS_MIN_STRAIGHT meters long gets one,
 * starting a little after the straight begins and ending where the braking starts.
 * Each zone is stored as {detection, start, end} distances in an ArrayList.
 */
public final class DrsZones {

    /** The zones as {detection point, zone start, zone end} in meters along the lap. */
    private final ArrayList<double[]> zones = new ArrayList<double[]>();

    /** The track, for wrapping distances. */
    private final Track track;

    /**
     * Finds the zones from where the track is flat out.
     *
     * @param track the track
     * @param profiles the speed profiles, which know which samples are on a straight
     */
    public DrsZones(Track track, ProfileSet profiles) {
        this.track = track;
        int n = track.getSampleCount();
        double spacing = track.getSpacing();
        int first = 0;
        while (first < n && profiles.isStraight(first * spacing)) {
            first++;
        }
        if (first == n) {
            return;
        }
        int i = first;
        int checked = 0;
        while (checked < n) {
            while (checked < n && !profiles.isStraight(i * spacing)) {
                i = (i + 1) % n;
                checked++;
            }
            int startIndex = i;
            int length = 0;
            while (checked < n && profiles.isStraight(i * spacing)) {
                i = (i + 1) % n;
                checked++;
                length++;
            }
            double meters = length * spacing;
            if (length > 0 && meters >= Tuning.DRS_MIN_STRAIGHT) {
                double start = track.wrap(startIndex * spacing + Tuning.DRS_START_AFTER);
                double end = track.wrap(startIndex * spacing + meters);
                double detection = track.wrap(start - Tuning.DRS_DETECTION_BEFORE);
                zones.add(new double[] {detection, start, end});
            }
        }
        Collections.sort(zones, (a, b) -> Double.compare(a[1], b[1]));
    }

    /**
     * Returns the number of zones.
     *
     * @return zone count
     */
    public int size() {
        return zones.size();
    }

    /**
     * Returns the zones, read-only, each as {detection, start, end} in meters along the lap.
     *
     * @return the zones
     */
    public List<double[]> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /**
     * Returns the zone whose detection point lies in the stretch a car just drove.
     *
     * @param before distance before the step (meters, may be beyond one lap)
     * @param meters distance driven this step
     * @return the zone index, or -1 if no detection point was crossed
     */
    public int detectionCrossed(double before, double meters) {
        for (int z = 0; z < zones.size(); z++) {
            double ahead = track.wrap(zones.get(z)[0] - before);
            if (ahead < meters) {
                return z;
            }
        }
        return -1;
    }

    /**
     * Returns the zone a point is in.
     *
     * @param distance distance along the lap in meters
     * @return the zone index, or -1 if the point is not in a zone
     */
    public int zoneAt(double distance) {
        double d = track.wrap(distance);
        for (int z = 0; z < zones.size(); z++) {
            double[] zone = zones.get(z);
            double into = track.wrap(d - zone[1]);
            if (into < track.wrap(zone[2] - zone[1])) {
                return z;
            }
        }
        return -1;
    }
}
