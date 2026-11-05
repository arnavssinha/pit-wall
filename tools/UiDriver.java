import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Developer tool (not part of the game) that presses keys and takes screenshots on a display,
 * so the game's controls can be checked on a machine with no keyboard (for example a virtual X display).
 * Usage: java tools/UiDriver.java wait:3000 click:640,400 key:Z shot:out.png ...
 */
public final class UiDriver {

    /**
     * Not instantiated.
     */
    private UiDriver() {
    }

    /**
     * Runs the commands in order.
     *
     * @param args commands: wait:MS, click:X,Y, key:NAME (a KeyEvent VK_ name without VK_), shot:FILE
     * @throws Exception if the robot or a screenshot fails
     */
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        for (String arg : args) {
            String[] parts = arg.split(":", 2);
            switch (parts[0]) {
                case "wait":
                    Thread.sleep(Long.parseLong(parts[1]));
                    break;
                case "click":
                    String[] xy = parts[1].split(",");
                    robot.mouseMove(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    break;
                case "key":
                    int code = KeyEvent.class.getField("VK_" + parts[1]).getInt(null);
                    robot.keyPress(code);
                    Thread.sleep(40);
                    robot.keyRelease(code);
                    break;
                case "shot":
                    Rectangle area = new Rectangle(0, 0, 1310, 800);
                    ImageIO.write(robot.createScreenCapture(area), "png", new File(parts[1]));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown command " + arg);
            }
            Thread.sleep(30);
        }
    }
}
