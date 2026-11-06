import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GOval;
import acm.graphics.GRect;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

/**
 * The car gauges: push level, tire compound, tire temperature against its window, tire wear,
 * brake temperature, and the ERS battery. They show whichever car the camera follows, so the
 * player can also inspect rivals.
 */
public final class GaugePanel extends GCompound {

    /** Version tag required because ACM objects are serializable. */
    private static final long serialVersionUID = 1L;

    /** Panel width in pixels. */
    public static final double WIDTH = 258;

    /** Panel height in pixels. */
    public static final double HEIGHT = 262;

    /** Lowest tire temperature on the gauge, in Celsius. */
    private static final double TIRE_MIN = 40;

    /** Highest tire temperature on the gauge, in Celsius. */
    private static final double TIRE_MAX = 150;

    /** Lowest brake temperature on the gauge, in Celsius. */
    private static final double BRAKE_MIN = 200;

    /** Highest brake temperature on the gauge, in Celsius. */
    private static final double BRAKE_MAX = 1300;

    /** Car name line. */
    private final GLabel title;

    /** Highlight behind the current push level. */
    private final GRect pushHighlight;

    /** Push level choices. */
    private final GLabel[] pushLabels = new GLabel[PushLevel.values().length];

    /** Colored circle showing the compound. */
    private final GOval compoundBadge;

    /** Compound letter inside the circle. */
    private final GLabel compoundLetter;

    /** Compound name next to the circle. */
    private final GLabel compoundName;

    /** Tire temperature. */
    private final GaugeBar tireTemp;

    /** Tire wear. */
    private final GaugeBar tireWear;

    /** Brake temperature. */
    private final GaugeBar brakeTemp;

    /** ERS battery. */
    private final GaugeBar battery;

    /** Boost status line: what the Overtake key will do. */
    private final GLabel ersLine;

    /** DRS badge: bright green while the flap is open. */
    private final GLabel drsLine;

    /** Track wetness where the car is. */
    private final GaugeBar wetGauge;

    /** Rain forecast or status line. */
    private final GLabel weatherLine;

    /** Compound whose window is currently drawn on the tire gauge. */
    private Compound shownCompound;

    /**
     * Builds the panel.
     */
    public GaugePanel() {
        GRect panel = new GRect(0, 0, WIDTH, HEIGHT);
        panel.setFilled(true);
        panel.setFillColor(Palette.PANEL);
        panel.setColor(Palette.PANEL_EDGE);
        add(panel);
        title = label("", 12, 22, Palette.HUD, Palette.TEXT);

        pushHighlight = new GRect(0, 32, 76, 20);
        pushHighlight.setFilled(true);
        pushHighlight.setFillColor(Palette.PLAYER_ROW);
        pushHighlight.setColor(Palette.PLAYER);
        add(pushHighlight);
        String[] keys = {"Q", "W", "E"};
        for (int i = 0; i < pushLabels.length; i++) {
            pushLabels[i] = label(keys[i] + " " + PushLevel.values()[i].getDisplayName(), 14 + i * 80, 47, Palette.SMALL, Palette.TEXT);
        }

        compoundBadge = new GOval(12, 62, 22, 22);
        compoundBadge.setFilled(true);
        add(compoundBadge);
        compoundLetter = label("", 18, 78, Palette.HUD, Palette.CAR_OUTLINE);
        compoundName = label("", 42, 78, Palette.HUD, Palette.TEXT);

        tireTemp = gauge("Tire temp", TIRE_MIN, TIRE_MAX, true, 98);
        tireWear = gauge("Tire wear", 0, 1.2, false, 122);
        brakeTemp = gauge("Brakes", BRAKE_MIN, BRAKE_MAX, true, 146);
        brakeTemp.setBand(Tuning.BRAKE_GOOD_LOW, Tuning.BRAKE_FADE_TEMP);
        battery = gauge("Boost", 0, 1, false, 170);
        ersLine = label("", 12, 206, Palette.SMALL, Palette.TEXT_DIM);
        drsLine = label("", WIDTH - 62, 206, Palette.HUD, Palette.TEXT_DIM);
        wetGauge = gauge("Wetness", 0, 1, false, 216);
        weatherLine = label("", 12, 250, Palette.SMALL, Palette.TEXT_DIM);
    }

    /**
     * Creates and adds a label.
     *
     * @param text initial text
     * @param x x position
     * @param y baseline y
     * @param font font
     * @param color color
     * @return the label
     */
    private GLabel label(String text, double x, double y, Font font, Color color) {
        GLabel l = new GLabel(text, x, y);
        l.setFont(font);
        l.setColor(color);
        add(l);
        return l;
    }

    /**
     * Creates and adds a gauge.
     *
     * @param name gauge name
     * @param min smallest value
     * @param max largest value
     * @param needle true for a needle gauge
     * @param y top of the gauge
     * @return the gauge
     */
    private GaugeBar gauge(String name, double min, double max, boolean needle, double y) {
        GaugeBar bar = new GaugeBar(name, min, max, needle);
        bar.setLocation(12, y);
        add(bar);
        return bar;
    }

    /**
     * Refreshes every gauge from a car and the race's weather.
     *
     * @param car the car to show
     * @param race the race, for the weather and forecast
     */
    public void update(Car car, Race race) {
        double wet = race.getConditions().wetnessAt(car.getDistance());
        Color wetColor = wet > Tuning.SLICK_WET_CLIFF ? Palette.ERS : Palette.TEXT_DIM;
        wetGauge.setValue(wet, String.format(Locale.ROOT, "%.0f%%", wet * 100), wetColor);
        setText(weatherLine, weatherText(race));
        String who = car.isPlayer() ? "YOUR CAR" : "RIVAL";
        setText(title, String.format(Locale.ROOT, "%s  #%d %s", who, car.getDriver().getNumber(), car.getDriver().getCode()));

        int push = car.getPushLevel().ordinal();
        double hx = 8 + push * 80;
        if (pushHighlight.getX() != hx) {
            pushHighlight.setLocation(hx, 32);
        }

        Tire tire = car.getTire();
        Compound compound = tire.getCompound();
        if (compound != shownCompound) {
            shownCompound = compound;
            compoundBadge.setFillColor(Palette.compoundColor(compound));
            compoundBadge.setColor(Palette.compoundColor(compound));
            compoundLetter.setLabel(compound.getLetter());
            compoundName.setLabel(compound.getDisplayName());
            tireTemp.setBand(compound.getWindowLow(), compound.getWindowHigh());
        }
        double t = tire.getTemperature();
        boolean inWindow = t >= compound.getWindowLow() && t <= compound.getWindowHigh();
        tireTemp.setValue(t, String.format(Locale.ROOT, "%.0f C", t), inWindow ? Palette.TEXT : Palette.WARNING);

        double wear = tire.getWear();
        Color wearColor = wear < Tuning.WEAR_CLIFF ? Palette.GOOD : wear < 1.0 ? Palette.WARNING : Palette.DANGER;
        tireWear.setValue(wear, String.format(Locale.ROOT, "%.0f%%", wear * 100), wearColor);

        double brake = car.getBrakes().getTemperature();
        Color brakeColor = brake > Tuning.BRAKE_FADE_TEMP ? Palette.DANGER : Palette.TEXT;
        brakeTemp.setValue(brake, String.format(Locale.ROOT, "%.0f C", brake), brakeColor);

        Ers ers = car.getErs();
        boolean ready = ers.getCharge() >= Tuning.OVERTAKE_COST;
        Color ersColor = car.isOvertakeActive() ? Palette.PLAYER : ready ? Palette.ERS : Palette.TEXT_DIM;
        battery.setValue(ers.getCharge(), String.format(Locale.ROOT, "%.0f%%", ers.getCharge() * 100), ersColor);
        String status;
        if (car.isOvertakeActive()) {
            status = "OVERTAKE ON";
        } else if (!car.isPlayer()) {
            status = ers.isDeploying() ? "deploying" : "charging";
        } else if (ready) {
            status = "SHIFT: Overtake ready";
        } else {
            status = String.format(Locale.ROOT, "Overtake needs %.0f%%", Tuning.OVERTAKE_COST * 100);
        }
        setText(ersLine, status);
        ersLine.setColor(car.isOvertakeActive() ? Palette.PLAYER : ready ? Palette.TEXT : Palette.TEXT_DIM);
        if (car.isDrsOpen()) {
            setText(drsLine, "DRS");
            drsLine.setColor(Palette.DRS);
        } else {
            setText(drsLine, race.isDrsAllowed() ? "DRS" : "");
            drsLine.setColor(Palette.TEXT_DIM);
        }
    }

    /**
     * Describes the weather: raining now, rain on the way (with a countdown from the forecast), or dry.
     *
     * @param race the race
     * @return one line of text
     */
    private static String weatherText(Race race) {
        TrackConditions c = race.getConditions();
        if (c.getRainIntensity() > Tuning.DRIZZLE) {
            return String.format(Locale.ROOT, "RAINING (%.0f%%)  track %.0f C", c.getRainIntensity() * 100, c.getTrackTemp());
        }
        Forecast f = race.getForecast();
        int lap = race.getLeaderLap();
        if (f != null && f.getShownLap() > lap) {
            return String.format(Locale.ROOT, "Rain %.0f%% in ~%d laps  track %.0f C", f.getShownChance() * 100,
                    f.getShownLap() - lap, c.getTrackTemp());
        }
        return String.format(Locale.ROOT, "Dry  track %.0f C  air %.0f C", c.getTrackTemp(), c.getAirTemp());
    }

    /**
     * Replaces a label's text only if it changed.
     *
     * @param label the label
     * @param text the new text
     */
    private static void setText(GLabel label, String text) {
        if (!text.equals(label.getLabel())) {
            label.setLabel(text);
        }
    }
}
