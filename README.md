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
| SHIFT | **Overtake.** At the start: launch when the lights go out. In the race: switch on the Overtake boost (battery power, DRS on a DRS straight, late braking, a little extra push) against the car ahead, then pick a line |
| A / S / D | Your line: Inside / Optimal / Outside (when attacking, defending, or into Turn 1 at the start) |
| Q / W / E | Pace: Conserve / Balanced / Push |
| B | Box this lap (press again to cancel). Your plan also calls its stops automatically |
| N | Box next lap |
| 1-5 | Tires for the stop: Soft, Medium, Hard, Intermediate, Wet |
| Esc | Back to the menu |
| Space or P | Pause or resume |
| T | Change time speed: 1x, 2x, 4x, 8x (starts at 4x) |
| Z | Switch between follow camera and whole-track view |
| M | Show or hide the minimap |
| F | Camera follows the next car down the order |
| G | Camera goes back to your car |
| R | New race (back to the setup screen) |
| F3 | Show or hide the debug overlay (frames per second and object count) |

## How a race works

1. **Menu, then Track Select** (Monza or Sao Paulo, keys 1-2), **then Setup.** On the setup screen you see which driver
   you were given and where they start (from a simulated qualifying session). You pick a strategy
   (one-stop early, one-stop late, two-stop, or no stop in short races), the tires for each stint, the
   race length (1-lap shootout, 3-lap sprint of about a minute, or 12-lap race of about five minutes,
   keys 1-3), the number of cars (10 to 22) and your baseline push level. Undo (U) takes back strategy edits.
2. **The start.** Five red lights come on one by one. Press **SHIFT** the moment they go out: a quick
   reaction gets you away first, too early is a jump start, and waiting too long is a slow getaway. The run
   to Turn 1 is in slow motion. Colored lines show your three ways in (blue Inside, green Optimal, orange
   Outside); press A, S or D to pick one, and SHIFT again for the Overtake boost (with late braking). The
   other cars spread out two and three wide too, so the start is the best chance to gain places.
3. **The race.** The car drives itself; set the pace with Q/W/E (Push is about 3% faster but wears tires
   about twice as fast; Conserve saves them).
   **Attacking:** when you close to within about a second of a car before a corner, time slows and a red
   panel says ATTACK. Press **SHIFT** for the Overtake boost (it needs 15% boost battery, shown in the
   gauges): battery power, DRS if you are on a DRS straight, late braking into the corner and a little extra
   push. Then pick your line: **A** inside, **S** optimal, **D** outside, shown as colored lines on the track.
   You can also press SHIFT any time you are within 1.5 s of the car ahead. If you do nothing, you just follow.
   **Defending:** when a car behind really attacks, time slows and a blue panel shows which way it is
   going (and its line as red dashes). Press **A** to cover the inside, **S** to hold the optimal line or
   **D** to cover the outside.
   **How a pass is decided:** on the road. If you are side by side at the turn-in, the corner goes to
   whoever is further alongside, with the inside line ahead of the outside (you must be level to win around
   the outside); late braking helps but can lock up (white smoke, you run wide); a close call can end in
   contact. The loser lifts and tucks in behind. An inside line exits slowly, so the car you passed can
   come back at you on the exit. The panel shows three facts: straight-line speed against the rival, grip,
   and your boost (and DRS).
   **DRS:** from lap 2, a car less than a second behind at a detection point opens its rear wing (it turns
   green, and DRS lights up in the gauges) on the next long straight; the Overtake boost also opens it on a
   DRS straight. Off in the rain and behind the safety car.
   **The screen:** timing tower on the left, your car's gauges on the right, the radio pops up at the top
   for a few seconds, and the controls are listed at the bottom. Your car has a yellow marker; tire edges
   show the compound color; a blinking red tire is a puncture; white smoke is a lock-up. M shows the minimap.
4. **Weather and incidents.** The setup screen shows a forecast (press V for different weather). Rain
   wets the track gradually and some corners dry before others; slicks fall off a cliff on a wet track.
   A car stopping on track brings out the safety car: no overtaking, the field bunches up, and a pit stop
   costs less time. Cars can also retire with mechanical failures. Tires worn past the limit can puncture
   (contact can too): the car slows badly, moves to the edge so others can pass, and is called into the
   pits. Your engineer warns you on the radio.
5. **Pit stops.** Your plan calls its stops automatically (the radio tells you). Press B to cancel or to
   box early, N to box next lap, and 1-5 to change the tires. Each team has its own box along the pit
   lane; cars pull into their box so others can drive past, and teammates queue (a "double stack"). In a dry race you must use two different
   dry compounds or you are disqualified.

6. **Results.** After the flag (or press Enter once you have finished) you get the classification, your
   tire wear and gaps lap by lap, the best sectors, and where you gained or lost time at each corner.
7. **Strategy Lab** (from the menu or the results, key L). It races every strategy on the same random
   grids, weather and incidents (about 200 races, around 15 to 20 seconds) and shows the win and podium
   chances, average finish, places gained, how each strategy did in rain or dry and on a hot or cool
   track, and the best pit-stop lap. It also saves the numbers to `PitWall-lab-<track>.csv` in your home
   folder (on a Mac: your user folder, the one with Desktop and Documents in it).

## Testing it yourself (no programming needed)

A ten-minute check that everything works:

1. Double-click `PitWall.jar`. The menu appears. Press Enter, Enter again (Monza), and you are on the
   setup screen. Press R a few times: your driver and grid slot change. Press V: the weather forecast changes.
2. Press Enter to start. Press SHIFT when the five red lights go out, then A, S or D for your line into
   Turn 1. Press F3: the top-right box shows the frame rate (it should be close to 60).
3. Press T until it says 8x. Press E (Push) and watch the tire temperature needle and wear bar rise faster.
4. When the screen slows down and the red ATTACK panel appears, press SHIFT (Overtake), then A, S or D.
   When a blue DEFEND panel appears, press A, S or D to cover. Read the result in the radio pop-up.
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
