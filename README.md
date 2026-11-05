# Pit Wall

An F1 race-strategy game written in Java with the ACM graphics library.
You are the race engineer for one driver in a 10-lap sprint. The car drives itself;
you make the strategy calls.

> Status: **M1, first deliverable.** 22 computer-driven cars race 10 laps of Monza with a follow camera,
> timing tower, lap counter and minimap. Strategy controls arrive in later milestones (see `DEVLOG.md`).

## Play it in three steps

1. **Install Java 21.** Download the Temurin 21 (LTS) installer for your computer from
   <https://adoptium.net/temurin/releases/?version=21> and run it. (Java 17 also works.
   Java 26 and newer do **not**: they removed the old applet classes the ACM library is built on.)
2. **Download this project.** On the GitHub page click the green **Code** button, then
   **Download ZIP**, and unzip it. Keep the files together: `PitWall.jar` needs the `lib` folder next to it.
3. **Double-click `PitWall.jar`.**

**If double-clicking does nothing or macOS blocks it:**

- macOS may say the file is from an unidentified developer. Right-click (or Control-click)
  `PitWall.jar`, choose **Open**, then click **Open** again. You only need to do this once.
  If there is no Open button, go to System Settings, Privacy and Security, and click **Open Anyway**.
- Fallback that always works: open the Terminal app, type `java -jar ` (with a space at the end),
  drag `PitWall.jar` into the Terminal window, and press Return.

## Controls

| Key | Action |
|---|---|
| Q / W / E | Push level: Conserve / Balanced / Push |
| X | ERS deploy on or off (spends the battery on straights for extra speed) |
| A / S / D | In a battle: Attack / Defend / Hold (or click the buttons). Pressed outside a battle, it pre-selects your stance for the next one |
| B | Box this lap (press again to cancel). Your plan also calls its stops automatically |
| N | Box next lap |
| 1-5 | Tires for the stop: Soft, Medium, Hard, Intermediate, Wet |
| Esc | Back to the menu |
| Space | Pause or resume |
| T | Change time speed: 1x, 2x, 4x, 8x (starts at 2x) |
| Z | Switch between follow camera and whole-track view |
| F | Camera follows the next car down the order (Shift-F: the car ahead) |
| G | Camera goes back to your car |
| R | New race (back to the setup screen) |
| F3 | Show or hide the debug overlay (frames per second and object count) |

## How a race works

1. **Menu, then Track Select** (Monza for now), **then Setup.** On the setup screen you see which driver
   you were given and where they start (from a simulated qualifying session). You pick a strategy
   (one-stop early, one-stop late or two-stop), the tires for each stint, the number of cars (10 to 22)
   and your baseline push level. Undo (U) takes back strategy edits.
2. **The race.** The car drives itself. Watch the gauges and adjust the push level (Q/W/E). When a rival
   is close at a braking zone, time slows down and you choose Attack, Defend or Hold (A/S/D).
   A red triangle over a rival means they are about to attack you.
3. **Pit stops.** Your plan calls its stops automatically (the radio tells you). Press B to cancel or to
   box early, N to box next lap, and 1-5 to change the tires. In a dry race you must use two different
   dry compounds or you are disqualified.

## Where the data comes from

- **Monza track shape: approximate, hand-built.** `data/tracks/monza_centerline.csv` was traced
  by hand from the circuit's well-known corner sequence (`tools/HandBuiltTrackMaker.java`).
  It is not survey or telemetry data. It will be replaced by FastF1 position data.
- **Monza lap length (5793 m)** is the official circuit length.
- **Monza target race lap (85.0 s)** is an approximate figure, flagged as such in
  `data/tracks/monza.properties`. It will be replaced by a median computed from FastF1 race laps.
- **Driver roster** (`data/roster.csv`) lists the 2026 driver line-up. It has not yet been checked
  against FastF1; see the note in the file. No team or driver pace ratings exist yet, so every
  car has the same base speed plus a small random variation drawn at the start of each race.

---

## For developers

### Build, test and check from the command line (macOS or Linux)

Requires a JDK (17 or 21) on the PATH.

```bash
./build.sh          # compiles src/ and rebuilds PitWall.jar
./test.sh           # headless simulation tests
tools/audit.sh      # Javadoc, import, layer and warning checks
tools/make_uml.sh   # regenerates docs/uml/classes.png
tools/make_submission.sh   # copies every .java file and the UML PNG flat into submission/
java -jar PitWall.jar --selftest   # opens the window, races for 10 s, prints frames per second, exits
```

On Windows use `build.bat` and `run.bat`.

### Project layout

All game source files sit flat in `src/` (default package) so they upload to Canvas as plain files.
`tools/layers.txt` assigns each file to a layer. Files in the `sim`, `ai`, `data` and `lab` layers
must not import `acm`, `java.awt` or `javax.swing`. `tools/audit.sh` enforces this, so the
simulation can run with no window at all (the tests and the Strategy Lab rely on that).

### jGRASP, IntelliJ, Eclipse

The game needs `lib/acm.jar` on the classpath, and the project folder itself on the
classpath so `data/...` files can be found. (The game also looks for `data/` in the
working folder and its parent.)

- **jGRASP:** open `src/PitWall.java`. Settings, PATH/CLASSPATH, Workspace, CLASSPATHS tab:
  add the full path to `lib/acm.jar` and the project folder. Compile and run `PitWall.java`.
- **IntelliJ IDEA:** File, Open, choose the project folder. File, Project Structure, Modules:
  mark `src` as Sources and `test` as Test Sources; Dependencies tab, add `lib/acm.jar`.
  Run `PitWall` with the working directory set to the project folder.
- **Eclipse:** File, New, Java Project, uncheck default location and choose the project folder.
  Project Properties, Java Build Path: source folder `src`, add `lib/acm.jar` under Libraries.
  Run `PitWall` as a Java Application.

### Troubleshooting

| Problem | Fix |
|---|---|
| `NoClassDefFoundError: acm/program/GraphicsProgram` | `lib/acm.jar` is not next to `PitWall.jar`. Unzip the whole project, not just the jar. |
| `UnsupportedClassVersionError` | Java is older than 11. Install Java 21. |
| Errors about `java.applet` or `JApplet` | Java is 26 or newer. Install Java 21. |
| `Data file not found` | Run from the project folder, or use `PitWall.jar` which contains the data. |
