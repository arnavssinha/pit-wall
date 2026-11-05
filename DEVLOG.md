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
- `Tuning.MIN_SEPARATION` raised (now 22 m) and grid slots (now 24 m) so the enlarged car drawings can never
  overlap. A new test checks this.

**Verified:** 9/9 tests, audit clean, self-test 59.5 FPS with 811 objects on the virtual display.
- Second pass from owner feedback: layout shrunk to 50 percent, track drawn 32 units wide, cars 10 units
  long. The inside edge of tight bends is pinched (`TrackDrawing.limitInside`) so the wide track never
  folds over itself at the chicanes.

## M2 Car model (2026-11-05)

**Done**
- `Compound` (Soft, Medium, Hard, Intermediate, Wet) with dry grip, temperature window, life in laps
  and ideal wetness. `PushLevel` (Conserve, Balanced, Push) with pace, intensity (wear grows with its
  square) and heat.
- `Tire`: temperature chases a target set by load and push; wear per meter is defined so a lap at
  Balanced on a neutral track uses exactly 1 / lifeLaps; grip = dry grip x temperature x wear (with a
  cliff) x wetness. Sprint scaling is documented in `Compound`.
- `Brakes` heat with braking power and fade above 1000 C. `Ers` charges under braking and gives a 3 percent
  speed boost on straights while deploying.
- `ProfileSet` caches a speed profile per grip level (HashMap), so worn or cold tires and fading brakes
  really slow the car in corners and braking zones. `LapCalibrator.meanLoadPerMeter` makes heat and wear
  fair across tracks.
- Tire-wear history table (cars x laps, `double[][]`) in `Race`.
- UI: `GaugePanel` (push level, compound, tire temperature with its green window, wear, brakes, ERS),
  tire icons in the timing tower, keys Q/W/E and X.

**Verified:** 15/15 tests. Measured on Monza, normalized to a neutral track: Soft 5.2, Medium 7.4,
Hard 11.1 laps of life. Medium lap 2: Conserve 85.98, Balanced 85.22, Push 84.87 s. Fresh Soft beats Hard
by 0.6 s; at lap 9 the Soft is 7.6 s slower. Gauges and keys checked with screenshots.

**Next:** M3, multi-car racing (qualifying grid, slipstream, lapped cars).

## M3 Multi-car racing (2026-11-05)

**Done**
- Simulated qualifying (`QualifyingEntry`, sorted with a Comparator): each car's lap is the track's
  reference lap divided by its race pace plus random noise; the sorted list is the grid. The player is a
  random driver, so the starting slot varies. Car pace is still a small random spread per race (team pace
  from data waits for M8).
- Slipstream: within 1 s behind another car on a straight, a speed bonus of up to 2.5 percent.
- Standing start, standings, gaps and the no-overlap rule carry over from M1.

**Verified:** 17/17 tests (grid follows qualifying, one player car, slipstream bands).

**Next:** M4, battles and corner calls.

## M4 Battles and corner calls (2026-11-05)

**Done**
- `Corner.findAll`: corners come from the track (curvature runs, apex, braking point from the speed
  profile); only corners with a real braking zone host battles. Monza has 8.
- `BattleManager`: a battle opens 1.6 s before a braking point when the car ahead is within 1.0 s.
  Computer drivers plan their stance one corner early (`intent`), which drives the red warning
  triangle above a rival about to attack you. Lapped or finished cars give way (blue flags).
- `BattleResolver`: the brief's pass score and logistic probability, plus attack risk (lock-up, run
  wide, contact) that grows with wear, brake heat, wetness and aggression. Costs: Defend loses 0.15 s
  whether or not an attack comes; Attack heats brakes and wears tires; ERS can be spent on the move.
- Visible passes: the attacker moves to the inside and drives past; cars may only be closer than the
  minimum gap when they are a passing pair side by side (enforced by a test every step).
- `DefaultStancePolicy` (computer drivers, and the player when the prompt times out) and `AiDriver`
  (push level, ERS use). Aggression is drawn from a narrow random band per race.
- UI: `BattlePrompt` with clickable `UiButton`s and a countdown, time slows to 0.25x and eases back,
  keys A/S/D, `RadioFeed` (ArrayDeque) for results.

**Verified:** 20/20 tests. AI-only Monza races: 33 percent of attacks succeed, about 34 passes per race.
Pass chance for equal cars: easy track vs Hold 0.74, vs Defend 0.23; very hard track vs Defend 0.02.
Screenshots show prompts, countdown, default stance, results and radio.

**Not yet seen on screen:** a pass by the player car up close (the passing logic is covered by tests).

**Next:** M5, pit stops, strategy and the setup screen.

## M5 Pit stops, strategy and screens (2026-11-05)

**Done**
- `PitLane`: entry, box at the start/finish line, exit, 80 km/h limit, random crew time (about 2.4 s).
  Cars merge back only when there is room for the whole merge. Pit loss comes out of the geometry
  (about 20 s at Monza), not from a typed-in number. Cars in the pit lane queue among themselves.
- `PitPlan`, `PitStop`, `StrategyPreset` (one-stop early, one-stop late, two-stop with suggested
  compounds). `PitStrategyPlanner` gives computer drivers a random preset, calls planned stops, and makes
  emergency stops at 97 percent wear. The player's plan calls its stops automatically; B cancels or
  boxes early, N boxes next lap, 1-5 pick tires.
- Two-compound rule (on by default): a dry-race finisher with one dry compound is disqualified and
  classified last (`Race.getClassification`).
- Screens: `ScreenManager` switches Menu, Track Select (cards with outlines from the data), Setup
  (assignment from qualifying, preset, tires per stint with an undo Deque, car count, baseline push) and
  Race. `ButtonGroup` maps buttons to actions with a HashMap.
- Pit lane drawing: ramps, lane, pit wall, a garage per team.
- Pacing: player prompts are at least 20 s of race time apart (the field often runs nose to tail).

**Verified:** 23/23 tests (stop on planned lap with the right tires, pit cost 20.3 s, rule disqualifies a
no-stop car, every computer car legal, presets legal). Self-test 59.5 FPS with about 950 objects. The full
flow Menu, Track Select, Setup, Race and a pit stop were checked with screenshots.

**Next:** M6, weather and events.

## M6 Weather and events (2026-11-05)

**Done**
- Weather statistics: `WeatherStatsLoader` reads `data/weather_stats.csv` (real export) if present,
  otherwise `data/fallback_weather.csv`, which holds APPROXIMATE values (owner approved; flagged in the
  file, the README and on the setup screen).
- `ForecastGenerator`: temperatures from clamped normal distributions, rain with the track's rain chance;
  the forecast shown is uncertain (a percentage, and a start lap that can be off by a lap or two).
- `TrackConditions`: wetness grid (double[][] of 150 m cells) plus a per-cell drying-rate grid, so some
  corners dry before others; wetness lags the rain. Wet asphalt is drawn darker.
- `EventQueue` (PriorityQueue of `RaceEvent`): rain start and stop, a car stopping on track (safety car),
  safety car in, mechanical retirements. `SafetyCar`: 60 percent pace, catch-up for cars far behind, no
  battles; pit stops become cheap. Retired cars park off the track and are classified DNF.
- `RadioEngineer`: tire temperature and wear, brake fade, flat battery, rival closing, rain coming,
  wet or drying track; a per-topic cooldown in a HashMap stops repeats.
- Computer drivers fit Intermediates or Wets on a wet track and slicks again when it dries. A wet race
  lifts the two-compound rule.
- Setup shows the forecast with a weather re-roll (V); gauges show wetness and a rain countdown.
- Not done from the brief's table: a separate surface-type grid (asphalt, kerb, grass, pit). Nothing in
  the simulation needs it, so it would be decorative; dropped as the brief allows.

**Verified:** 29/29 tests, including forecast statistics (rain in 12.8 percent of 4000 draws vs 15 percent),
a rain race (all running computer cars switched to rain tires, no disqualifications), uneven drying, safety
car (no passes; the cars that stayed out closed from 1259 m to 593 m), retirement (DNF, race ends), event
order. The self-test now includes early rain and a safety car: 59.3 FPS, no exceptions.

**Next:** M7, results screen and Strategy Lab.

## M7 Results and Strategy Lab (2026-11-05)

**Done**
- `RaceReport`: classification (finishers, then DNF, then DSQ), tire wear by lap, gap to the leader by lap
  for the player and the cars either side, best sectors (three per lap, from the timing loops), the
  player's sectors by lap (TreeMap), and seconds gained or lost per lap at each corner against the field
  median (clean laps only).
- `ResultsScreen` with `LineChart` and `BarChart` drawn from ACM shapes. The race moves to the results 4 s
  after the last car finishes, or straight away with Enter once the player has finished.
- Strategy Lab (`StrategyLab`, `StrategyVariant`, `LabRun`, `LabResult`, `LabCsvWriter`, `LabScreen`): each
  scenario (seed plus weather drawn from the statistics) is raced once per strategy (three presets and a
  Medium-Hard pit-window sweep at laps 2, 4, 6, 8), so comparisons are paired. Races run in parallel on
  every core (ExecutorService). Output: win and podium share, average finish and places gained, rain/dry
  and hot/cool splits, best pit window, comparison with the player's last race, CSV in the home folder.
- Speed: neighbour lookups use a road order sorted once per step instead of comparing every pair
  (identical results, about 2x faster); straights are precomputed per track.

**Verified:** 32/32 tests (report sectors add up to the lap time; lab scenarios share grids; CSV written;
196 races in 14.9 s on 4 cores against a 60 s target). Results and Lab screens checked with screenshots.

**Next:** M8 (all six tracks, real data export, roster check, balance), M9 (docs, demo mode, final audit).
