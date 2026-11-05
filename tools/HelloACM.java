import acm.graphics.GLabel;
import acm.graphics.GRect;
import acm.program.GraphicsProgram;
import java.awt.Color;
import java.awt.Font;

/**
 * A tiny window that proves the installed JDK can run the class's acm.jar.
 * Run it with "java -cp lib/acm.jar:build/tools HelloACM" after compiling it.
 * It closes itself after three seconds so it can be used in scripts.
 */
public final class HelloACM extends GraphicsProgram {

    /** Version tag required because ACM programs are serializable. */
    private static final long serialVersionUID = 1L;

    /** Window width read by the ACM library. */
    public static final int APPLICATION_WIDTH = 480;

    /** Window height read by the ACM library. */
    public static final int APPLICATION_HEIGHT = 240;

    /**
     * Creates the program. The ACM library sets up the window in start().
     */
    public HelloACM() {
        super();
    }

    /**
     * Draws a rectangle and a label, waits three seconds, then exits.
     */
    @Override
    public void run() {
        GRect box = new GRect(40, 40, 400, 120);
        box.setFilled(true);
        box.setFillColor(new Color(30, 40, 60));
        add(box);
        GLabel label = new GLabel("Hello ACM on Java " + System.getProperty("java.version"), 60, 110);
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
        label.setColor(Color.WHITE);
        add(label);
        System.out.println("HelloACM window open, canvas " + getWidth() + "x" + getHeight());
        pause(3000);
        System.exit(0);
    }

    /**
     * Starts the program.
     *
     * @param args command-line arguments passed to the ACM library
     */
    public static void main(String[] args) {
        new HelloACM().start(args);
    }
}
