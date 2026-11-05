import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Developer tool (not part of the game) that reads src/*.java and writes the UML class
 * diagram as PlantUML text (docs/uml/classes.puml) and as a PNG (docs/uml/classes.png).
 * It draws with plain Java2D so no extra software is needed. Classes are grouped into
 * bands by layer, taken from tools/layers.txt.
 * Usage: java tools/UmlDiagram.java
 */
public final class UmlDiagram {

    /** Order and titles of the layer bands. */
    private static final String[][] BANDS = {
        {"ui", "ui  (ACM graphics and input)"},
        {"sim", "sim  (pure simulation, no graphics imports)"},
        {"ai", "ai  (computer drivers)"},
        {"data", "data  (file loaders)"},
        {"lab", "lab  (headless Strategy Lab)"},
    };

    /** Widest the image may grow before boxes wrap to a new row. */
    private static final int MAX_ROW_WIDTH = 2600;

    /** One parsed class. */
    private static final class Clazz {
        /** Class name. */
        String name;
        /** class, enum or interface. */
        String kind = "class";
        /** Superclass name or null. */
        String parent;
        /** Interfaces implemented. */
        List<String> interfaces = new ArrayList<>();
        /** Instance fields as "name : Type". */
        List<String> fields = new ArrayList<>();
        /** Raw field types, used to find associations. */
        List<String> fieldTypes = new ArrayList<>();
        /** Public methods and constructors. */
        List<String> methods = new ArrayList<>();
        /** Count of static constants (shown as one line). */
        int constants;
        /** Layer from layers.txt. */
        String layer = "sim";
        /** Box geometry in the image. */
        int x, y, w, h;
    }

    /**
     * Not instantiated.
     */
    private UmlDiagram() {
    }

    /**
     * Parses the sources and writes both outputs.
     *
     * @param args unused
     * @throws IOException if a file cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> layers = readLayers(new File("tools/layers.txt"));
        Map<String, Clazz> classes = new LinkedHashMap<>();
        File[] files = new File("src").listFiles((dir, n) -> n.endsWith(".java"));
        java.util.Arrays.sort(files);
        for (File f : files) {
            Clazz c = parse(f);
            if (c != null) {
                c.layer = layers.getOrDefault(f.getName(), "sim");
                classes.put(c.name, c);
            }
        }
        new File("docs/uml").mkdirs();
        writePuml(classes, new File("docs/uml/classes.puml"));
        writePng(classes, new File("docs/uml/classes.png"));
        System.out.println("Wrote docs/uml/classes.puml and docs/uml/classes.png (" + classes.size() + " classes)");
    }

    /**
     * Reads tools/layers.txt into a file-name to layer map.
     *
     * @param file the layers file
     * @return map from file name to layer
     * @throws IOException if reading fails
     */
    private static Map<String, String> readLayers(File file) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            String[] parts = t.split("\\s+");
            if (parts.length == 2) {
                map.put(parts[1], parts[0]);
            }
        }
        return map;
    }

    /**
     * Pulls the class header, instance fields and public methods out of one file.
     * Only top-level members (four-space indent) are read, which matches this project's style.
     *
     * @param file the source file
     * @return the parsed class, or null if no top-level type was found
     * @throws IOException if reading fails
     */
    private static Clazz parse(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        Pattern header = Pattern.compile("^public (?:final |abstract )*(class|enum|interface) (\\w+)"
                + "(?:<[^>]*>)?(?: extends ([\\w.]+))?(?: implements ([\\w, <>]+))?");
        Pattern field = Pattern.compile("^    (?:private|public|protected) (static )?(?:final )?(?:transient )?"
                + "([\\w<>\\[\\], ]+?) (\\w+)(?: = .*)?;");
        Pattern method = Pattern.compile("^    public (?:static )?(?:final )?(?:([\\w<>\\[\\], ]+?) )?(\\w+)\\(([^)]*)\\)");
        Clazz c = null;
        boolean inComment = false;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("/**") || t.startsWith("/*")) {
                inComment = !t.endsWith("*/");
                continue;
            }
            if (inComment) {
                if (t.endsWith("*/")) {
                    inComment = false;
                }
                continue;
            }
            if (c == null) {
                Matcher m = header.matcher(line);
                if (m.find()) {
                    c = new Clazz();
                    c.kind = m.group(1);
                    c.name = m.group(2);
                    c.parent = m.group(3);
                    if (m.group(4) != null) {
                        for (String i : m.group(4).split(",")) {
                            c.interfaces.add(i.trim().replaceAll("<.*", ""));
                        }
                    }
                }
                continue;
            }
            Matcher fm = field.matcher(line);
            if (fm.find()) {
                if (fm.group(1) != null) {
                    c.constants++;
                } else {
                    c.fields.add(fm.group(3) + " : " + fm.group(2));
                    c.fieldTypes.add(fm.group(2));
                }
                continue;
            }
            Matcher mm = method.matcher(line);
            if (mm.find()) {
                String ret = mm.group(1);
                String params = simplifyParams(mm.group(3));
                String sig = mm.group(2) + "(" + params + ")" + (ret == null ? "" : " : " + ret);
                c.methods.add(sig);
            }
        }
        return c;
    }

    /**
     * Keeps only the parameter types, which keeps boxes narrow.
     *
     * @param params the raw parameter list
     * @return comma-separated parameter types
     */
    private static String simplifyParams(String params) {
        if (params.trim().isEmpty()) {
            return "";
        }
        List<String> types = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (char ch : params.toCharArray()) {
            if (ch == '<') {
                depth++;
            } else if (ch == '>') {
                depth--;
            }
            if (ch == ',' && depth == 0) {
                types.add(typeOf(cur.toString()));
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        types.add(typeOf(cur.toString()));
        return String.join(", ", types);
    }

    /**
     * Drops the parameter name from "Type name".
     *
     * @param param one parameter
     * @return its type
     */
    private static String typeOf(String param) {
        String p = param.trim().replace("final ", "");
        int space = p.lastIndexOf(' ');
        return space < 0 ? p : p.substring(0, space).trim();
    }

    /**
     * Finds project classes mentioned in a field type, such as Car in {@code List<Car>}.
     *
     * @param type the field type
     * @param classes all known classes
     * @return names of referenced project classes
     */
    private static List<String> referenced(String type, Map<String, Clazz> classes) {
        List<String> out = new ArrayList<>();
        for (String token : type.split("[^\\w]+")) {
            if (classes.containsKey(token) && !out.contains(token)) {
                out.add(token);
            }
        }
        return out;
    }

    /**
     * Writes PlantUML text so the diagram can also be rendered with PlantUML.
     *
     * @param classes all classes
     * @param out destination file
     * @throws IOException if writing fails
     */
    private static void writePuml(Map<String, Clazz> classes, File out) throws IOException {
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            w.println("@startuml");
            w.println("' Generated by tools/UmlDiagram.java from src/. Do not edit by hand.");
            w.println("hide circle");
            for (String[] band : BANDS) {
                boolean any = false;
                for (Clazz c : classes.values()) {
                    if (c.layer.equals(band[0])) {
                        any = true;
                    }
                }
                if (!any) {
                    continue;
                }
                w.println("package " + band[0] + " <<Frame>> {");
                for (Clazz c : classes.values()) {
                    if (!c.layer.equals(band[0])) {
                        continue;
                    }
                    w.println("  " + c.kind + " " + c.name + " {");
                    for (String f : c.fields) {
                        w.println("    -" + f);
                    }
                    if (c.constants > 0) {
                        w.println("    {static} " + c.constants + " constants");
                    }
                    for (String m : c.methods) {
                        w.println("    +" + m);
                    }
                    w.println("  }");
                }
                w.println("}");
            }
            for (Clazz c : classes.values()) {
                if (c.parent != null) {
                    w.println(c.parent + " <|-- " + c.name);
                }
                for (String i : c.interfaces) {
                    w.println(i + " <|.. " + c.name);
                }
                List<String> seen = new ArrayList<>();
                for (String t : c.fieldTypes) {
                    for (String r : referenced(t, classes)) {
                        if (!r.equals(c.name) && !seen.contains(r)) {
                            seen.add(r);
                            w.println(c.name + " --> " + r);
                        }
                    }
                }
            }
            w.println("@enduml");
        }
    }

    /**
     * Lays out the boxes in layer bands and draws boxes, then association and inheritance arrows.
     *
     * @param classes all classes
     * @param out destination PNG
     * @throws IOException if writing fails
     */
    private static void writePng(Map<String, Clazz> classes, File out) throws IOException {
        Font titleFont = new Font("SansSerif", Font.BOLD, 15);
        Font bodyFont = new Font("SansSerif", Font.PLAIN, 12);
        Font bandFont = new Font("SansSerif", Font.BOLD, 18);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D pg = probe.createGraphics();
        FontMetrics tm = pg.getFontMetrics(titleFont);
        FontMetrics bm = pg.getFontMetrics(bodyFont);
        int lineH = bm.getHeight();
        int gap = 40;
        int y = 30;
        int maxX = 0;
        List<int[]> bandBoxes = new ArrayList<>();
        List<String> bandTitles = new ArrayList<>();
        for (String[] band : BANDS) {
            List<Clazz> members = new ArrayList<>();
            for (Clazz c : classes.values()) {
                if (c.layer.equals(band[0])) {
                    members.add(c);
                }
            }
            if (members.isEmpty()) {
                continue;
            }
            int bandTop = y;
            y += 36;
            int x = 30;
            int rowH = 0;
            for (Clazz c : members) {
                int w = tm.stringWidth(title(c)) + 24;
                for (String s : body(c)) {
                    w = Math.max(w, bm.stringWidth(s) + 20);
                }
                c.w = w;
                c.h = 30 + lineH * body(c).size() + 14;
                if (x + w > MAX_ROW_WIDTH && x > 30) {
                    x = 30;
                    y += rowH + gap;
                    rowH = 0;
                }
                c.x = x;
                c.y = y;
                x += w + gap;
                rowH = Math.max(rowH, c.h);
                maxX = Math.max(maxX, c.x + c.w);
            }
            y += rowH + 30;
            bandBoxes.add(new int[] {10, bandTop, 0, y - bandTop});
            bandTitles.add(band[1]);
            y += 20;
        }
        int width = maxX + 40;
        int height = y + 10;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < bandBoxes.size(); i++) {
            int[] b = bandBoxes.get(i);
            g.setColor(new Color(244, 246, 250));
            g.fillRoundRect(b[0], b[1], width - 20, b[3], 16, 16);
            g.setColor(new Color(150, 160, 180));
            g.drawRoundRect(b[0], b[1], width - 20, b[3], 16, 16);
            g.setFont(bandFont);
            g.setColor(new Color(60, 70, 90));
            g.drawString(bandTitles.get(i), b[0] + 14, b[1] + 25);
        }
        g.setStroke(new BasicStroke(1.4f));
        for (Clazz c : classes.values()) {
            List<String> seen = new ArrayList<>();
            for (String t : c.fieldTypes) {
                for (String r : referenced(t, classes)) {
                    if (!r.equals(c.name) && !seen.contains(r)) {
                        seen.add(r);
                        drawArrow(g, c, classes.get(r), false);
                    }
                }
            }
            if (c.parent != null && classes.containsKey(c.parent)) {
                drawArrow(g, c, classes.get(c.parent), true);
            }
        }
        for (Clazz c : classes.values()) {
            g.setColor(new Color(255, 252, 235));
            g.fillRect(c.x, c.y, c.w, c.h);
            g.setColor(new Color(90, 80, 40));
            g.drawRect(c.x, c.y, c.w, c.h);
            g.drawLine(c.x, c.y + 28, c.x + c.w, c.y + 28);
            g.setFont(titleFont);
            g.setColor(Color.BLACK);
            g.drawString(title(c), c.x + 12, c.y + 20);
            g.setFont(bodyFont);
            int ly = c.y + 28 + lineH;
            for (String s : body(c)) {
                g.setColor(s.startsWith("+") ? new Color(20, 60, 120) : new Color(70, 70, 70));
                g.drawString(s, c.x + 10, ly);
                ly += lineH;
            }
        }
        g.setFont(bodyFont);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Solid arrow with open head: has-a (field). Hollow triangle: extends. "
                + "Generated by tools/UmlDiagram.java", 20, height - 6);
        ImageIO.write(img, "png", out);
    }

    /**
     * Builds the box title, adding the superclass and interfaces.
     *
     * @param c the class
     * @return the title text
     */
    private static String title(Clazz c) {
        String t = (c.kind.equals("class") ? "" : "<<" + c.kind + ">> ") + c.name;
        if (c.parent != null) {
            t += " extends " + c.parent;
        }
        if (!c.interfaces.isEmpty()) {
            t += " implements " + String.join(", ", c.interfaces);
        }
        return t;
    }

    /**
     * Builds the box body lines: fields, constant count, then public methods.
     *
     * @param c the class
     * @return the body lines
     */
    private static List<String> body(Clazz c) {
        List<String> lines = new ArrayList<>();
        for (String f : c.fields) {
            lines.add("- " + f);
        }
        if (c.constants > 0) {
            lines.add("  (" + c.constants + " static constants)");
        }
        for (String m : c.methods) {
            lines.add("+ " + m);
        }
        return lines;
    }

    /**
     * Draws a line between two boxes, clipped to their edges, with an arrow head at the target.
     *
     * @param g the graphics context
     * @param from source box
     * @param to target box
     * @param inheritance true for a hollow triangle head, false for an open head
     */
    private static void drawArrow(Graphics2D g, Clazz from, Clazz to, boolean inheritance) {
        double fx = from.x + from.w / 2.0;
        double fy = from.y + from.h / 2.0;
        double tx = to.x + to.w / 2.0;
        double ty = to.y + to.h / 2.0;
        double[] start = clip(fx, fy, tx, ty, from);
        double[] end = clip(tx, ty, fx, fy, to);
        g.setColor(inheritance ? new Color(40, 40, 40) : new Color(70, 110, 170, 170));
        g.drawLine((int) start[0], (int) start[1], (int) end[0], (int) end[1]);
        double angle = Math.atan2(end[1] - start[1], end[0] - start[0]);
        double size = 12;
        int[] xs = {(int) end[0], (int) (end[0] - size * Math.cos(angle - 0.4)), (int) (end[0] - size * Math.cos(angle + 0.4))};
        int[] ys = {(int) end[1], (int) (end[1] - size * Math.sin(angle - 0.4)), (int) (end[1] - size * Math.sin(angle + 0.4))};
        if (inheritance) {
            g.setColor(Color.WHITE);
            g.fillPolygon(xs, ys, 3);
            g.setColor(new Color(40, 40, 40));
            g.drawPolygon(xs, ys, 3);
        } else {
            g.drawLine(xs[0], ys[0], xs[1], ys[1]);
            g.drawLine(xs[0], ys[0], xs[2], ys[2]);
        }
    }

    /**
     * Moves a point from a box center to where the line toward (ox, oy) leaves the box.
     *
     * @param cx box center x
     * @param cy box center y
     * @param ox other end x
     * @param oy other end y
     * @param box the box
     * @return the edge point
     */
    private static double[] clip(double cx, double cy, double ox, double oy, Clazz box) {
        double dx = ox - cx;
        double dy = oy - cy;
        double sx = dx == 0 ? Double.MAX_VALUE : (box.w / 2.0) / Math.abs(dx);
        double sy = dy == 0 ? Double.MAX_VALUE : (box.h / 2.0) / Math.abs(dy);
        double s = Math.min(1.0, Math.min(sx, sy));
        return new double[] {cx + dx * s, cy + dy * s};
    }
}
