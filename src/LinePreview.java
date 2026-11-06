import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GObject;
import acm.graphics.GOval;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the lines of the player's fight on the track: each choice the player can make (Inside in
 * blue, Optimal in green, Outside in orange, matching the panel buttons) from the car to the corner
 * exit, the rival's line in red (dashed), and the player's braking point. The paths come from the same
 * LinePlan the car will drive, so the preview shows exactly what will happen. The chosen line is drawn
 * thick; once the player has chosen, only the chosen line and the rival's are shown.
 */
public final class LinePreview {

    /** Straight pieces per drawn path. */
    private static final int SEGMENTS = 22;

    /** Strokes per path: the chosen line is drawn three times, slightly offset, to look thick. */
    private static final int LAYERS = 3;

    /** Path slots: the three player options, then the rival. */
    private static final int SLOTS = 4;

    /** Slot of the rival's line. */
    private static final int RIVAL = 3;

    /** Radius of the dot at the end of each option, in pixels. */
    private static final double DOT = 5;

    /** The player options in slot order. */
    private static final LineChoice[] OPTIONS = {LineChoice.INSIDE, LineChoice.OPTIMAL, LineChoice.OUTSIDE};

    /** Key letters shown on the option dots, in slot order. */
    private static final String[] KEYS = {"A", "S", "O"};

    /** The lines, by slot, layer and segment. */
    private final GLine[][][] lines = new GLine[SLOTS][LAYERS][SEGMENTS];

    /** Dots at the apex of each path. */
    private final GOval[] dots = new GOval[SLOTS];

    /** Key letters next to the dots. */
    private final GLabel[] keys = new GLabel[SLOTS];

    /** Marker across the track where the player starts braking. */
    private final GLine[] brakeMark = new GLine[LAYERS];

    /** Label for the braking marker. */
    private final GLabel brakeLabel;

    /** Every drawn object, for adding to the world. */
    private final List<GObject> objects = new ArrayList<GObject>();

    /** True while anything is shown. */
    private boolean shown = true;

    /**
     * Creates the (hidden) preview objects.
     */
    public LinePreview() {
        for (int slot = 0; slot < SLOTS; slot++) {
            Color color = slot == RIVAL ? Palette.LINE_RIVAL : Palette.lineColor(OPTIONS[slot]);
            for (int layer = 0; layer < LAYERS; layer++) {
                for (int s = 0; s < SEGMENTS; s++) {
                    GLine line = new GLine(0, 0, 0, 0);
                    line.setColor(color);
                    lines[slot][layer][s] = line;
                    objects.add(line);
                }
            }
            GOval dot = new GOval(2 * DOT, 2 * DOT);
            dot.setFilled(true);
            dot.setFillColor(color);
            dot.setColor(Palette.CAR_OUTLINE);
            dots[slot] = dot;
            objects.add(dot);
            GLabel key = new GLabel(slot == RIVAL ? "!" : KEYS[slot]);
            key.setFont(Palette.HUD);
            key.setColor(color);
            keys[slot] = key;
            objects.add(key);
        }
        for (int layer = 0; layer < LAYERS; layer++) {
            brakeMark[layer] = new GLine(0, 0, 0, 0);
            brakeMark[layer].setColor(Palette.BRAKE_MARK);
            objects.add(brakeMark[layer]);
        }
        brakeLabel = new GLabel("BRAKE");
        brakeLabel.setFont(Palette.SMALL);
        brakeLabel.setColor(Palette.BRAKE_MARK);
        objects.add(brakeLabel);
        hideAll();
    }

    /**
     * Returns every object to add to the world view.
     *
     * @return the objects
     */
    public List<GObject> getObjects() {
        return objects;
    }

    /**
     * Updates the preview for the player's current battle (or hides it when there is none).
     *
     * @param race the race
     * @param drawing converts track positions to drawing units
     * @param zoom pixels per drawing unit
     */
    public void update(Race race, TrackDrawing drawing, double zoom) {
        Battle battle = race.getBattles().getPlayerBattle();
        if (battle == null || battle.isResolved()) {
            hideAll();
            return;
        }
        shown = true;
        Car player = race.getPlayerCar();
        boolean attacking = battle.isPlayerAttacking();
        LineChoice chosen = battle.getPlayerLine();
        boolean decided = battle.isPlayerDecided() && chosen != null;
        LinePlan chosenPlan = null;
        for (int slot = 0; slot < OPTIONS.length; slot++) {
            LineChoice option = OPTIONS[slot];
            boolean isChosen = option == chosen;
            if (decided && !isChosen) {
                hideSlot(slot);
                continue;
            }
            LinePlan plan = planFor(race, player, option, battle.isPlayerLate(), battle.getCorner(), attacking);
            if (isChosen) {
                chosenPlan = plan;
            }
            drawSlot(slot, race, player, plan, drawing, zoom, isChosen ? LAYERS : 1, false);
        }
        LineChoice rivalLine = battle.getRivalLine();
        Car rival = battle.getRival();
        if (rivalLine == null || rivalLine == LineChoice.STAY_BEHIND) {
            hideSlot(RIVAL);
        } else {
            LinePlan plan = planFor(race, rival, rivalLine, battle.isRivalLate(), battle.getCorner(), !attacking);
            drawSlot(RIVAL, race, rival, plan, drawing, zoom, 2, true);
        }
        if (chosenPlan == null && chosen == LineChoice.STAY_BEHIND) {
            hideBrake();
        } else {
            LinePlan reference = chosenPlan != null ? chosenPlan
                    : planFor(race, player, LineChoice.OPTIMAL, battle.isPlayerLate(), battle.getCorner(), attacking);
            drawBrake(race, player, reference, drawing, zoom);
        }
    }

    /**
     * Returns the plan a car has (if it matches) or would have for a choice.
     *
     * @param race the race
     * @param car the car
     * @param line the line
     * @param late true for late braking
     * @param corner the corner
     * @param attacker true for the car behind
     * @return the plan
     */
    private static LinePlan planFor(Race race, Car car, LineChoice line, boolean late, Corner corner, boolean attacker) {
        LinePlan actual = car.getLinePlan();
        if (actual != null && actual.getChoice() == line && actual.isLateBrake() == late && actual.getCorner() == corner) {
            return actual;
        }
        boolean early = attacker && line.leavesLine() && BattleManager.hasStraightEdge(car);
        return new LinePlan(line, corner, late, attacker, car.getDistance(), early, race.getTrack());
    }

    /**
     * Draws one path from the car's position to the end of its plan.
     *
     * @param slot the slot
     * @param race the race
     * @param car the car
     * @param plan the plan
     * @param drawing the drawing converter
     * @param zoom pixels per drawing unit
     * @param layers how many strokes (1 thin, 3 thick)
     * @param dashed true to leave every other piece out
     */
    private void drawSlot(int slot, Race race, Car car, LinePlan plan, TrackDrawing drawing, double zoom,
                          int layers, boolean dashed) {
        double start = car.getDistance();
        double end = Math.max(start + 1, plan.getEndAt());
        double[] xs = new double[SEGMENTS + 1];
        double[] ys = new double[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            double d = start + (end - start) * i / SEGMENTS;
            double lateral = plan.lateralAt(d, normalLine(race, car, d));
            xs[i] = drawing.x(d, lateral) * zoom;
            ys[i] = -drawing.y(d, lateral) * zoom;
        }
        for (int layer = 0; layer < LAYERS; layer++) {
            for (int s = 0; s < SEGMENTS; s++) {
                GLine line = lines[slot][layer][s];
                boolean visible = layer < layers && !(dashed && s % 2 == 1);
                if (visible) {
                    line.setStartPoint(xs[s] + layer % 2, ys[s] + layer / 2);
                    line.setEndPoint(xs[s + 1] + layer % 2, ys[s + 1] + layer / 2);
                }
                if (line.isVisible() != visible) {
                    line.setVisible(visible);
                }
            }
        }
        double apex = Math.min(end, Math.max(start, plan.getApexAt()));
        double lateral = plan.lateralAt(apex, normalLine(race, car, apex));
        double ax = drawing.x(apex, lateral) * zoom;
        double ay = -drawing.y(apex, lateral) * zoom;
        dots[slot].setLocation(ax - DOT, ay - DOT);
        dots[slot].setVisible(true);
        keys[slot].setLocation(ax + DOT + 2, ay + DOT);
        keys[slot].setVisible(true);
    }

    /**
     * Draws the braking marker across the track where the player will start braking.
     *
     * @param race the race
     * @param car the player's car
     * @param plan the player's plan
     * @param drawing the drawing converter
     * @param zoom pixels per drawing unit
     */
    private void drawBrake(Race race, Car car, LinePlan plan, TrackDrawing drawing, double zoom) {
        double at = plan.getBrakeAt() + (plan.isLateBrake() ? Tuning.LATE_BRAKE_METERS : 0);
        if (at < car.getDistance()) {
            hideBrake();
            return;
        }
        double center = plan.lateralAt(at, normalLine(race, car, at));
        double half = Tuning.CAR_WIDTH;
        for (int layer = 0; layer < LAYERS; layer++) {
            double shift = layer - 1;
            brakeMark[layer].setStartPoint(drawing.x(at + shift, center - half) * zoom, -drawing.y(at + shift, center - half) * zoom);
            brakeMark[layer].setEndPoint(drawing.x(at + shift, center + half) * zoom, -drawing.y(at + shift, center + half) * zoom);
            brakeMark[layer].setVisible(true);
        }
        brakeLabel.setLabel(plan.isLateBrake() ? "BRAKE LATE" : "BRAKE");
        brakeLabel.setLocation(drawing.x(at, center + half) * zoom + 4, -drawing.y(at, center + half) * zoom);
        brakeLabel.setVisible(true);
    }

    /**
     * Returns a car's normal line at a distance ahead (racing line plus its own small offset).
     *
     * @param race the race
     * @param car the car
     * @param distance absolute distance along the race
     * @return sideways position in meters
     */
    private static double normalLine(Race race, Car car, double distance) {
        double limit = Tuning.TRACK_WIDTH / 2 - Tuning.RACING_LINE_EDGE_MARGIN;
        return Math.max(-limit, Math.min(limit, race.getTrack().lineAt(distance) + car.getLineBias()));
    }

    /**
     * Hides one path.
     *
     * @param slot the slot
     */
    private void hideSlot(int slot) {
        for (int layer = 0; layer < LAYERS; layer++) {
            for (int s = 0; s < SEGMENTS; s++) {
                if (lines[slot][layer][s].isVisible()) {
                    lines[slot][layer][s].setVisible(false);
                }
            }
        }
        dots[slot].setVisible(false);
        keys[slot].setVisible(false);
    }

    /**
     * Hides the braking marker.
     */
    private void hideBrake() {
        for (GLine line : brakeMark) {
            line.setVisible(false);
        }
        brakeLabel.setVisible(false);
    }

    /**
     * Hides everything.
     */
    private void hideAll() {
        if (!shown) {
            return;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            hideSlot(slot);
        }
        hideBrake();
        shown = false;
    }
}
