import java.util.HashMap;
import java.util.Locale;

/**
 * The player's race engineer: watches the player's car and the weather and sends short, useful
 * radio messages (tires too hot or cold, tires going off, brakes fading, battery flat, a rival
 * closing, rain on the way). Each topic has a cooldown, kept in a HashMap of when it was last sent,
 * and the engineer waits between any two messages, so the radio never spams.
 */
public final class RadioEngineer {

    /** The race. */
    private final Race race;

    /** Race time each topic was last sent. */
    private final HashMap<String, Double> lastSent = new HashMap<String, Double>();

    /** Race time of the last message the engineer sent, on any topic. */
    private double lastAnyMessage = -Double.MAX_VALUE;

    /** Race time of the last check. */
    private double lastCheck = -Double.MAX_VALUE;

    /** Gap to the car behind at the last check, in seconds, to tell if it is closing. */
    private double lastGapBehind = Double.MAX_VALUE;

    /**
     * Creates the engineer.
     *
     * @param race the race
     */
    public RadioEngineer(Race race) {
        this.race = race;
    }

    /**
     * Checks the situation a couple of times a second and sends any message that is due.
     */
    public void update() {
        double time = race.getTime();
        if (time - lastCheck < Tuning.RADIO_CHECK_SECONDS) {
            return;
        }
        lastCheck = time;
        Car car = race.getPlayerCar();
        if (car.isFinished() || car.isRetired() || race.isStartPhase()) {
            return;
        }
        Tire tire = car.getTire();
        Compound compound = tire.getCompound();
        if (tire.getTemperature() > compound.getWindowHigh() + Tuning.RADIO_TEMP_MARGIN) {
            send("hot", "Tires are running hot. Back off a little?");
        } else if (tire.getTemperature() < compound.getWindowLow() - Tuning.RADIO_TEMP_MARGIN && car.getLapsCompleted() > 0) {
            send("cold", "Tires are cold. Push to bring them in?");
        }
        if (tire.getWear() >= Tuning.WEAR_CLIFF && car.getPitRequest() == 0 && !car.isInPitLane()) {
            send("worn", "Tires are going off. Think about boxing.");
        }
        if (car.getBrakes().getTemperature() > Tuning.BRAKE_FADE_TEMP) {
            send("brakes", "Brakes are overheating. Lift and coast.");
        }
        checkRival(car);
        checkWeather(car);
    }

    /**
     * Warns when the car behind is within a second and gaining.
     *
     * @param car the player's car
     */
    private void checkRival(Car car) {
        Car behind = null;
        for (Car other : race.getCars()) {
            if (other != car && !other.isInPitLane() && !other.isRetired() && race.carAheadOnTrack(other) == car) {
                behind = other;
            }
        }
        if (behind == null) {
            lastGapBehind = Double.MAX_VALUE;
            return;
        }
        double gap = race.trackGap(behind, car) / Math.max(1, behind.getSpeed());
        if (gap < Tuning.RADIO_RIVAL_GAP && gap < lastGapBehind - 0.01) {
            send("rival", String.format(Locale.ROOT, "%s is closing, %.1f s behind. Ready to defend.",
                    behind.getDriver().getCode(), gap));
        }
        lastGapBehind = gap;
    }

    /**
     * Talks about rain: a warning before it arrives, and tire advice once the track is wet or drying.
     *
     * @param car the player's car
     */
    private void checkWeather(Car car) {
        RainEvent rain = race.getRain();
        double lap = race.getTrack().getProfile().getMedianRaceLapSeconds();
        if (rain != null && race.getTime() < rain.getStart()) {
            int lapsAway = (int) Math.ceil((rain.getStart() - race.getTime()) / lap);
            if (lapsAway <= Tuning.RADIO_RAIN_WARN_LAPS) {
                send("rain-coming", "Rain in about " + lapsAway + (lapsAway == 1 ? " lap" : " laps") + ". Do we box?");
            }
        }
        double wet = race.getConditions().averageWetness();
        if (car.getTire().getCompound().isDry() && wet > Tuning.SLICK_WET_CLIFF) {
            send("wet", "Track is wet. Box for Intermediates: press 4, then B.");
        } else if (!car.getTire().getCompound().isDry() && wet < Tuning.AI_DRY_WETNESS) {
            send("drying", "Track is drying. Slicks now: press 1, 2 or 3, then B.");
        }
    }

    /**
     * Sends a message unless the same topic was sent recently or the engineer spoke a moment ago.
     * The engineer only says one thing at a time, like a real race engineer who keeps the radio clear.
     *
     * @param topic short topic key
     * @param text the message
     */
    private void send(String topic, String text) {
        Double last = lastSent.get(topic);
        boolean topicQuiet = last == null || race.getTime() - last >= Tuning.RADIO_TOPIC_COOLDOWN;
        boolean radioQuiet = race.getTime() - lastAnyMessage >= Tuning.RADIO_MIN_GAP_SECONDS;
        if (topicQuiet && radioQuiet) {
            lastSent.put(topic, race.getTime());
            lastAnyMessage = race.getTime();
            race.addMessage(text, true);
        }
    }
}
