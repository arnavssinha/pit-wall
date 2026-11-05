import acm.graphics.GLabel;
import acm.program.GraphicsProgram;
import java.awt.Color;
import java.awt.Font;

/**
 * Main class of Pit Wall, an F1 race strategy game drawn with the ACM graphics library.
 * This scaffold version only opens the game window.
 */
public final class PitWall extends GraphicsProgram {

    /** Version tag required because ACM programs are serializable. */
    private static final long serialVersionUID = 1L;

    /** Window width read by the ACM library. */
    public static final int APPLICATION_WIDTH = 1280;

    /** Window height read by the ACM library; includes room for the ACM menu bar. */
    public static final int APPLICATION_HEIGHT = 742;

    /**
     * Creates the program. The ACM library builds the window in start().
     */
    public PitWall() {
        super();
    }

    /**
     * Shows the title text.
     */
    @Override
    public void run() {
        setBackground(new Color(24, 28, 40));
        GLabel title = new GLabel("PIT WALL", 80, 120);
        title.setFont(new Font("SansSerif", Font.BOLD, 48));
        title.setColor(Color.WHITE);
        add(title);
    }

    /**
     * Starts the game.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new PitWall().start(args);
    }
}
