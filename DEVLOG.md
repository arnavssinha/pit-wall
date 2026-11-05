# Dev log

One entry per milestone: what was done, how it was checked, what is next.

## M0 Scaffold (2026-11-05)

**Done**
- Folder layout, `build.sh` / `run.sh` / `test.sh` / `build.bat` / `run.bat`.
- `lib/acm.jar` from the class (dated 2008-12-08 inside the jar).
- `tools/HelloACM.java` opens an ACM window; verified on JDK 21.0.12 under a virtual display.
- `PitWall.jar` with manifest `Main-Class: PitWall` and `Class-Path: lib/acm.jar`; verified
  `java -jar PitWall.jar` starts from a different working folder.
- `tools/layers.txt` plus `tools/audit.sh` (Javadoc asterisk greps, wildcard imports, ACM rule,
  layer rule, `javac -Xlint:all -Werror`, `javadoc -Xdoclint:all`).
- `tools/UmlDiagram.java` draws `docs/uml/classes.png` from the source (PlantUML could not be run here).
- `test/SimTests.java` runner with no external framework.

**Findings**
- `GCompound.scale` exists in this acm.jar and scales every child in place, so the camera can zoom
  the vector track smoothly. No fixed zoom presets are needed.
- `GImage(int[][])` exists, which keeps a no-`Graphics` rendering fallback available if frame rate suffers.
- The ACM menu bar takes about 21 px of the window, so layout uses the real canvas size.

**Next:** M1, the first deliverable.

## M1 First deliverable (2026-11-05)

**Done**
- Monza loaded from data files: `data/tracks/monza.properties` and an APPROXIMATE hand-built
  centerline (`tools/HandBuiltTrackMaker.java`, clearly labeled in the file, README and on screen data source).
- Track model: resampled every 5 m, heading and smoothed curvature per sample (`Track`, `TrackSample`).
- Speed model: grip plus downforce corner limits, a backward braking pass (`SpeedProfile`, `CarPhysics`),
  acceleration that fades toward top speed.
- Calibration: `LapCalibrator` solves one grip scale per track by bisection so a clean lap matches the
  target lap. Monza needs 0.991, so the raw physics was already close.
- 22 cars from `data/roster.csv` race 10 laps from a standing start; following cars hold station and can
  never get closer than one car length plus 1 m (`Race.planMove`).
- Timing: lap-time table and timing-loop table (both `double[][]`); gaps are interpolated at the
  follower's exact position, so they always agree with the running order.
- ACM rendering: vector track in about 200 m chunks, kerbs, edge lines, start line and grid marks;
  car polygons rotated to the heading; follow camera with smooth zoom to a whole-track view;
  timing tower, HUD, minimap, F3 debug overlay, chequered-flag banner.
- `--selftest` mode, UML v1, `tools/make_submission.sh` for the flat .java + .png upload.

**Verified (in a cloud container with a virtual X display, JDK 21)**
- `./test.sh`: 8 headless tests pass (roster, geometry, profile, calibration 85.00 s vs 85.0 s,
  config limits, determinism, full-race no-overlap, no NaN, lap sums, gap ordering). A full 22-car race
  runs headless in about 0.65 s.
- `tools/audit.sh`: all checks pass (javac and javadoc zero warnings, asterisk and import rules, layers).
- `java -jar PitWall.jar --selftest`: 59.6 FPS at 2x and 8x with 475 objects, no exceptions.
- Keys checked with `tools/UiDriver.java` (Robot key presses plus screenshots): Space, T, Z, F, G, F3 (pressed three times), and the chequered-flag banner after a full race.

**Not verified:** frame rate and double-click launch on a real Mac; the Monza target lap and the roster
are not yet checked against FastF1 data.

**Next:** M2, the car model (tires, brakes, ERS, push level, gauges).

## M1 visual pass (2026-11-05), from owner feedback

**Done**
- `TrackDrawing`: the screen draws the layout shrunk to 65 percent while the track is drawn 20 units
  wide and cars 8 units long, so there is more track and less empty grass. The simulation still uses
  real meters, so lap times and calibration are unchanged.
- Cars are drawn from 10 parts (tires, wings, body with sidepods, stripe, cockpit, helmet); the player
  has a yellow ring and helmet.
- Scenery: paved run-off along the whole lap, gravel traps on the outside of corners, mowed-grass
  stripes, chequered start line, grid slot marks.
- `Tuning.MIN_SEPARATION` raised to 14.5 m and grid slots to 16 m so the enlarged car drawings can never
  overlap. A new test checks this.

**Verified:** 9/9 tests, audit clean, self-test 59.5 FPS with 811 objects on the virtual display.
