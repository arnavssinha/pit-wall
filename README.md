# Pit Wall

An F1 race-strategy game written in Java with the ACM graphics library.
You are the race engineer for one driver in a 10-lap sprint. The car drives itself;
you make the strategy calls.

> Status: **M7 complete, plus feedback rounds.** Two circuits (Monza and Sao Paulo), the full race weekend
> flow (menu, track select, setup, race, results) and the Strategy Lab. More tracks, real data and the demo
> mode come in M8 and M9 (see `DEVLOG.md`).

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
| A / S / O / D | In a fight: **attacking** Inside / Optimal / Outside / Stay behind; **defending** Cover inside / Optimal / Cover outside. Pressed outside a fight, it pre-selects your line for the next one |
| L | Late braking on or off for the fight (gains ground, risks a lock-up) |
| C | Attack the car ahead now (within 1.5 s): opens a fight for the next corner |
| B | Box this lap (press again to cancel). Your plan also calls its stops automatically |
| N | Box next lap |
| 1-5 | Tires for the stop: Soft, Medium, Hard, Intermediate, Wet |
| Esc | Back to the menu |
| Space | Pause or resume |
| T | Change time speed: 1x, 2x, 4x, 8x (starts at 4x) |
| Z | Switch between follow camera and whole-track view |
| F | Camera follows the next car down the order (Shift-F: the car ahead) |
| G | Camera goes back to your car |
| R | New race (back to the setup screen) |
| F3 | Show or hide the debug overlay (frames per second and object count) |

## How a race works

1. **Menu, then Track Select** (Monza or Sao Paulo, keys 1-2), **then Setup.** On the setup screen you see which driver
   you were given and where they start (from a simulated qualifying session). You pick a strategy
   (one-stop early, one-stop late, two-stop, or no stop in short races), the tires for each stint, the
   race length (1-lap shootout, 3-lap sprint of about a minute, or 12-lap race of about five minutes,
   keys 1-3), the number of cars (10 to 22) and your baseline push level. Undo (U) takes back strategy edits.
2. **The race.** The car drives itself. Watch the gauges and adjust the push level (Q/W/E).
   **Fights:** when you are within about a second of another car before a corner (or you press C), time
   slows and a panel opens: red frame when you attack, blue when you defend. The track shows each choice as
   a colored line (blue Inside, green Optimal, orange Outside, matching the buttons), the rival's line as
   red dashes, and where you will brake. The panel lists the factors, not a percentage: the gap, your
   straight-line speed against theirs and why (DRS, battery, tow), grip and tire wear, battery, how good the
   corner is for passing, your lock-up risk and the rival's line. Green means it favors you, red them.
   **How a pass is decided:** on the road. Speed differences come only from tow, DRS, ERS, grip and push.
   If you are side by side at the turn-in, the corner goes to whoever is further alongside, with the inside
   line ahead of the outside (you must be level to win around the outside); late braking helps but can lock
   up (white smoke, you run wide); a close call can end in contact. The loser lifts and tucks in behind. An
   Inside line exits slowly, so a car that covered the inside can be passed back on the exit (a switchback).
   You can pass two or three cars on a straight, and cars that are not fighting simply follow.
   **DRS:** from lap 2, if you are less than a second behind the car ahead at a detection point, your
   rear wing opens (it turns green) on the next long straight. It is off in the rain and behind the safety car.
   **Push levels matter:** Push is about 2% faster but wears tires about twice as fast and runs them
   hotter; Conserve is about 2.5% slower and saves nearly half the wear.
   **On the cars:** tire edges show the compound color; nearby cars have a tire-condition bar (green, then
   yellow, then red as the tires wear); a blinking red tire is a puncture; a red ring marks your rival in a
   fight and a red triangle a car about to attack you.
3. **Weather and incidents.** The setup screen shows a forecast (press V for different weather). Rain
   wets the track gradually and some corners dry before others; slicks fall off a cliff on a wet track.
   A car stopping on track brings out the safety car: no overtaking, the field bunches up, and a pit stop
   costs less time. Cars can also retire with mechanical failures. Tires worn past the limit can puncture
   (contact can too): the car slows badly and is called into the pits. Your engineer warns you on the radio.
4. **Pit stops.** Your plan calls its stops automatically (the radio tells you). Press B to cancel or to
   box early, N to box next lap, and 1-5 to change the tires. Each team has its own box along the pit
   lane; cars pull into their box so others can drive past, and teammates queue (a "double stack"). In a dry race you must use two different
   dry compounds or you are disqualified.

5. **Results.** After the flag (or press Enter once you have finished) you get the classification, your
   tire wear and gaps lap by lap, the best sectors, and where you gained or lost time at each corner.
6. **Strategy Lab** (from the menu or the results, key L). It races every strategy on the same random
   grids, weather and incidents (about 200 races, around 15 to 20 seconds) and shows the win and podium
   chances, average finish, places gained, how each strategy did in rain or dry and on a hot or cool
   track, and the best pit-stop lap. It also saves the numbers to `PitWall-lab-<track>.csv` in your home
   folder (on a Mac: your user folder, the one with Desktop and Documents in it).

## Testing it yourself (no programming needed)

A ten-minute check that everything works:

1. Double-click `PitWall.jar`. The menu appears. Press Enter, Enter again (Monza), and you are on the
   setup screen. Press R a few times: your driver and grid slot change. Press V: the weather forecast changes.
2. Press Enter to start. Press F3: the top-right box shows the frame rate (it should be close to 60).
3. Press T until it says 8x. Press E (Push) and watch the tire temperature needle and wear bar rise faster.
4. When the screen slows down and the fight panel appears, look at the colored lines on the track, press A, S
   or O (and L for late braking). Read the result on the radio. Press C behind a car to attack on demand.
5. Wait for your planned pit stop (the bottom panel tells you the lap), or press B to box now.
   Watch the car drive through the pit lane.
6. Let the race finish (or press Enter after you cross the line): check the results screen.
7. Press L for the Strategy Lab, then Enter. Wait about 20 seconds for the results.

## Where the data comes from

- **Monza track shape: approximate, hand-built.** `data/tracks/monza_centerline.csv` was traced
  by hand from the circuit's well-known corner sequence (`tools/HandBuiltTrackMaker.java`).
  It is not survey or telemetry data. It will be replaced by FastF1 position data.
- **Monza lap length (5793 m)** is the official circuit length.
- **Monza target race lap (85.0 s)** is an approximate figure, flagged as such in
  `data/tracks/monza.properties`. It will be replaced by a median computed from FastF1 race laps.
- **Sao Paulo track shape: approximate, traced.** `data/tracks/saopaulo_centerline.csv` was traced from a
  circuit outline picture with `tools/trace_outline.py` (thinned to a line, ordered counter-clockwise, scaled
  to the official 4309 m). The Bico de Pato hairpin was moved slightly away from the pit straight so the wide
  game track does not merge. Its pit lane is drawn on the outside (the real one is on the inside).
- **Sao Paulo target race lap (74.0 s)** is an approximate figure, flagged in `data/tracks/saopaulo.properties`.
- **Corner speeds** come from the traced/hand-built layout at real track width; the wide track you see is a
  rounded, stylized version of it, so a chicane stays slow even though it is drawn as a smooth curve.
- **Weather: APPROXIMATE.** `data/fallback_weather.csv` holds rough temperatures and a rain chance for
  each track from general knowledge of the race weekend, because the FastF1 export could not run yet.
  It is not measured data, and the setup screen says "APPROXIMATE weather data" while it is in use.
  When `data/weather_stats.csv` (from `tools/export_data.py`) exists, the game uses it instead.
- **Safety car chance, overtaking difficulty, tire wear and weather volatility per track** are balance
  choices, marked as such in each track's properties file.
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
java -jar PitWall.jar --selftest   # opens the window, races 10 s with rain and a safety car, prints FPS, exits
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
