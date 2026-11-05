import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Developer tool (not part of the game) that writes the APPROXIMATE hand-built Monza
 * centerline used until FastF1 position data is exported.
 * The layout is described as a list of straights and constant-radius arcs that follow
 * Monza's real corner sequence (Rettifilo, Curva Grande, Roggia, the two Lesmos, Ascari,
 * Parabolica). Angles and radii are rough estimates, not survey data. Two straight lengths
 * are solved so the loop closes exactly.
 * Usage: java tools/HandBuiltTrackMaker.java
 */
public final class HandBuiltTrackMaker {

    /** Spacing of the output points in meters. */
    private static final double STEP = 2.0;

    /** Fraction of the main straight, from Parabolica exit, where the start/finish line sits. */
    private static final double START_LINE_FRACTION = 0.45;

    /**
     * One piece of the layout. A straight has radius 0. Angles are degrees, left turns positive.
     */
    private static final class Piece {
        /** Straight length in meters (ignored for arcs). */
        double length;
        /** Turn angle in degrees, positive is left. */
        double angle;
        /** Arc radius in meters, 0 for a straight. */
        double radius;
        /** True if this straight's length is solved to close the loop. */
        boolean solved;
        /** Label for the printout. */
        String name;

        /**
         * Creates a piece.
         *
         * @param name label
         * @param length straight length
         * @param angle turn angle in degrees, left positive
         * @param radius arc radius, 0 for a straight
         * @param solved whether the length is solved
         */
        Piece(String name, double length, double angle, double radius, boolean solved) {
            this.name = name;
            this.length = length;
            this.angle = angle;
            this.radius = radius;
            this.solved = solved;
        }
    }

    /**
     * Not instantiated.
     */
    private HandBuiltTrackMaker() {
    }

    /**
     * Builds the Monza layout, closes it and writes the CSV.
     *
     * @param args unused
     * @throws IOException if the file cannot be written
     */
    public static void main(String[] args) throws IOException {
        List<Piece> p = new ArrayList<>();
        p.add(new Piece("main straight (solved)", 1000, 0, 0, true));
        p.add(new Piece("T1 Rettifilo right", 0, -80, 18, false));
        p.add(new Piece("", 25, 0, 0, false));
        p.add(new Piece("T2 Rettifilo left", 0, 75, 20, false));
        p.add(new Piece("", 130, 0, 0, false));
        p.add(new Piece("T3 Curva Grande", 0, -95, 330, false));
        p.add(new Piece("", 250, 0, 0, false));
        p.add(new Piece("T4 Roggia left", 0, 55, 22, false));
        p.add(new Piece("", 20, 0, 0, false));
        p.add(new Piece("T5 Roggia right", 0, -60, 26, false));
        p.add(new Piece("", 200, 0, 0, false));
        p.add(new Piece("T6 Lesmo 1", 0, -60, 70, false));
        p.add(new Piece("", 150, 0, 0, false));
        p.add(new Piece("T7 Lesmo 2", 0, -45, 55, false));
        p.add(new Piece("Serraglio straight (solved)", 600, 0, 0, true));
        p.add(new Piece("Serraglio kink", 0, 15, 900, false));
        p.add(new Piece("", 80, 0, 0, false));
        p.add(new Piece("T8 Ascari left", 0, 50, 55, false));
        p.add(new Piece("T9 Ascari right", 0, -75, 75, false));
        p.add(new Piece("T10 Ascari left", 0, 40, 70, false));
        p.add(new Piece("back straight", 500, 0, 0, false));
        p.add(new Piece("T11 Parabolica entry", 0, -70, 100, false));
        p.add(new Piece("T11 Parabolica exit", 0, -110, 230, false));

        double total = 0;
        for (Piece piece : p) {
            total += piece.angle;
        }
        if (Math.abs(total + 360) > 1e-9) {
            throw new IllegalStateException("Turns must sum to -360 (one clockwise loop), got " + total);
        }
        solveClosure(p);

        List<double[]> pts = trace(p);
        double mainLen = p.get(0).length;
        int startIndex = (int) Math.round(mainLen * START_LINE_FRACTION / STEP);
        double length = 0;
        for (Piece piece : p) {
            length += piece.radius == 0 ? piece.length : Math.toRadians(Math.abs(piece.angle)) * piece.radius;
        }

        File out = new File("data/tracks/monza_centerline.csv");
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            w.println("# APPROXIMATE HAND-BUILT CENTERLINE. NOT FastF1 OR SURVEY DATA.");
            w.println("# Made by tools/HandBuiltTrackMaker.java from Monza's corner sequence with estimated");
            w.println("# angles and radii. The loader rescales it to the official lap length.");
            w.println("# Replace with FastF1 position data (tools/export_data.py) when available.");
            w.println("x_m,y_m");
            for (double[] pt : pts) {
                w.printf(java.util.Locale.ROOT, "%.2f,%.2f%n", pt[0], pt[1]);
            }
        }
        for (Piece piece : p) {
            if (piece.solved) {
                System.out.printf("%-30s solved length %.1f m%n", piece.name, piece.length);
            }
        }
        System.out.printf("Raw length %.1f m, %d points, start/finish index %d%n", length, pts.size(), startIndex);
    }

    /**
     * Sets the two solved straight lengths so the path ends where it started.
     * The end point is linear in those two lengths, so this is a 2-by-2 linear system.
     *
     * @param p the pieces
     */
    private static void solveClosure(List<Piece> p) {
        int a = -1;
        int b = -1;
        for (int i = 0; i < p.size(); i++) {
            if (p.get(i).solved) {
                if (a < 0) {
                    a = i;
                } else {
                    b = i;
                }
            }
        }
        p.get(a).length = 0;
        p.get(b).length = 0;
        double[] end = endPoint(p);
        double[] da = direction(p, a);
        double[] db = direction(p, b);
        double det = da[0] * db[1] - da[1] * db[0];
        double la = (-end[0] * db[1] + end[1] * db[0]) / det;
        double lb = (-da[0] * end[1] + da[1] * end[0]) / det;
        if (la <= 0 || lb <= 0) {
            throw new IllegalStateException("Closure needs negative straights: " + la + ", " + lb);
        }
        p.get(a).length = la;
        p.get(b).length = lb;
    }

    /**
     * Heading direction (unit vector) at the start of piece i.
     *
     * @param p the pieces
     * @param index piece index
     * @return unit direction
     */
    private static double[] direction(List<Piece> p, int index) {
        double heading = Math.PI / 2;
        for (int i = 0; i < index; i++) {
            heading += Math.toRadians(p.get(i).angle);
        }
        return new double[] {Math.cos(heading), Math.sin(heading)};
    }

    /**
     * End point of the path when traced exactly (no sampling).
     *
     * @param p the pieces
     * @return end point x and y
     */
    private static double[] endPoint(List<Piece> p) {
        double x = 0;
        double y = 0;
        double heading = Math.PI / 2;
        for (Piece piece : p) {
            if (piece.radius == 0) {
                x += piece.length * Math.cos(heading);
                y += piece.length * Math.sin(heading);
            } else {
                double turn = Math.toRadians(piece.angle);
                double sign = Math.signum(turn);
                double cx = x - sign * piece.radius * Math.sin(heading);
                double cy = y + sign * piece.radius * Math.cos(heading);
                heading += turn;
                x = cx + sign * piece.radius * Math.sin(heading);
                y = cy - sign * piece.radius * Math.cos(heading);
            }
        }
        return new double[] {x, y};
    }

    /**
     * Walks the path and emits a point every STEP meters.
     *
     * @param p the pieces
     * @return the points
     */
    private static List<double[]> trace(List<Piece> p) {
        List<double[]> pts = new ArrayList<>();
        double x = 0;
        double y = 0;
        double heading = Math.PI / 2;
        double carry = 0;
        for (Piece piece : p) {
            double len = piece.radius == 0 ? piece.length : Math.toRadians(Math.abs(piece.angle)) * piece.radius;
            double curvature = piece.radius == 0 ? 0 : Math.signum(piece.angle) / piece.radius;
            double d = carry;
            while (d < len) {
                double[] pos = advance(x, y, heading, curvature, d);
                pts.add(pos);
                d += STEP;
            }
            carry = d - len;
            double[] endPos = advance(x, y, heading, curvature, len);
            x = endPos[0];
            y = endPos[1];
            heading += curvature * len;
        }
        return pts;
    }

    /**
     * Position after moving distance d along a straight or arc.
     *
     * @param x start x
     * @param y start y
     * @param heading start heading in radians
     * @param curvature signed curvature, 0 for straight
     * @param d distance travelled
     * @return the new position
     */
    private static double[] advance(double x, double y, double heading, double curvature, double d) {
        if (curvature == 0) {
            return new double[] {x + d * Math.cos(heading), y + d * Math.sin(heading)};
        }
        double r = 1 / curvature;
        double h2 = heading + curvature * d;
        return new double[] {x + r * (Math.sin(h2) - Math.sin(heading)), y - r * (Math.cos(h2) - Math.cos(heading))};
    }
}
