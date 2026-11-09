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

## Feedback pass: visuals, pace, racing line, overtaking, race length (2026-11-06)

Owner feedback after M7. These changes override parts of the original brief (fixed 10 laps; passes
resolved only at braking zones); the owner asked for them.

**Done**
- Track drawing: the scenery is now one picture painted pixel by pixel (`TrackRaster`): each pixel's
  sideways distance from the nearest centerline point picks asphalt, edge line, kerb, run-off, gravel,
  pit lane or garage, with smoothed edges. Corners stay clean at any width (the old polygon strips folded
  over in tight bends). Built from an int[][] pixel array via ACM's GImage, so no java.awt.Graphics.
  Objects on screen fell from about 950 to about 690.
- Racing line (`RacingLine`): repeated smoothing ("stiff rubber band") within the track width gives the
  outside-inside-outside line. Cars ride it, the speed profile uses its curvature (the calibration adapted
  automatically), and car drawings point where they are actually going.
- Overtaking: battles open up to 4 s before a braking zone, so moves start on the straights. Choices are
  Inside (A), Outside (O), Defend (S) and Hold (D). Attackers tuck into the slipstream, then pull out;
  defenders move across to cover the inside; computer attackers switch sides if the door is shut. Cars can
  run side by side and drag past on the straight. At the braking point the pass formula now includes how
  far alongside the attacker is. A failed attacker visibly backs off. Never guaranteed: about 22 percent
  of attacks succeed at Monza (about 20 passes per race), far fewer against a car that covers.
- Overtake mode (from the 2026 rules): extra ERS power for a car within a second of the car ahead.
- Pace: default speed 4x; tire temperature reacts in about 7 s, brakes heat and cool faster; ERS gives
  +8 percent with a blue exhaust flame; computer drivers push and deploy while fighting; every car's form
  wanders slightly; time slows to 0.35x in battles and the camera zooms in.
- Race length (`RaceLength`): 1-lap shootout, 3-lap sprint (about 1 minute at 4x), 12-lap race (about
  5 minutes). Tire life scales with length; strategy stops are fractions of the race; No stop is offered
  (and the two-compound rule is off) below 5 laps. The Lab uses the race length of your last race.
- Rain effects: falling streaks and a tint that deepens with track wetness.
- ERS deploy switches itself off when the battery is flat (one radio message instead of repeats).

**Verified:** 33/33 tests (including short races), audit clean, self-test 59.5 FPS with 685 objects,
Lab 196 races in about 20 s on 4 cores. Screenshots of setup, battles, chicanes and rain.

## Feedback pass 2: one scale, organic racing line, DRS, push levels, radio, pit boxes (2026-11-06)

Owner feedback: cars sometimes spin; the racing line and corners feel unnatural; overtaking feels
weird; add DRS; make Conserve/Balanced/Push more extreme; the team radio spams; pit stops back up.

**Root causes found**
- The drawing was stretched about 5 times more across the track than along it (layout shrunk to 0.5,
  width blown up 2.7 times). Every sideways move was exaggerated, so cars looked like they slid or spun,
  and tight corners pinched into blobs.
- The racing line search converged so slowly that the line never used the track width: it only cut
  each apex by about 5 m instead of going wide, clipping the apex and running out wide again.
- A pass could stay side by side for a minute when the passing car was stuck behind a third car, and
  cars leaving the pits waited at the pit-lane offset for a gap that never came in traffic.

**Done**
- One scale in every direction (`TrackDrawing`). The game world is stylized instead: a 56 m wide
  track with 20 m long cars on the real lap length; all sideways distances in `Tuning` were rescaled.
- Corners tighter than 40 m radius are rounded off automatically when a track is built
  (`Track.limitCurvature`), so the wide track never folds and chicanes become flowing esses.
- Racing line: coarse-to-fine smoothing (every 32nd point, then 16th, ... then every point) so the
  line now uses the whole width: wide on entry, apex, wide on exit.
- Corners are found at the slowest point of the racing line, and the two halves of a chicane count as
  one corner. Monza's Turn 1 is now its easiest place to pass, as in real life.
- No spinning: a car can never move sideways at a steeper angle than about 17 degrees to its direction
  of travel (`Tuning.MAX_LATERAL_RATE`), and the drawn car turns at most 12 degrees per frame.
- DRS (`DrsZones`): zones on every flat-out straight of 400 m or more, detection point 250 m before.
  Less than 1 s behind at detection gives +4% top speed in the zone; off on lap 1, in the wet and under
  the safety car. The rear wing turns green when open; the gauge panel shows DRS ready / DRS OPEN.
  It replaces overtake mode.
- Passing: the car that loses a side-by-side fight runs at least 5 m/s slower until it is clear
  behind; once the passing car is half a car ahead, both head back to the racing line, so the passing
  car moves across in front. Pass score base lowered to keep about 50 to 60 passes per 12-lap race.
- Push levels: Push +2% pace, about 2x wear, hotter tires and brakes; Conserve -2.5% pace, about 0.55x
  wear, cooler.
- Radio: the engineer waits at least 12 s between any two messages and 60 s before repeating a topic;
  only your own passes, contact and battles you chose a move for are reported.
- Pit stops: every team has its own box along the lane (22 m apart, matching the drawn garages). Cars
  pull out of the fast lane into the box so others drive past; a teammate queues behind (double stack).
  Computer cars move each planned stop up to a lap earlier or later at random. Leaving the pits, a car
  runs along the track edge, then moves to the racing line when there is room (or squeezes in after 4 s),
  lifting if a car is alongside.
- Known simplification: in the pit lane, a car swinging into its box can briefly overlap a
  neighbour leaving the next box (boxes are 22 m apart for 20 m cars). The strict no-overlap rule is
  tested everywhere else. A car parked in its box never blocks another team's car.
- Retired cars ignore any later steering order (one had been pulled back onto the track by a battle
  and blocked the whole field).

**Tests:** 34 (new: DRS zones and rules). The pit-stop test accepts a box on either side of the
line; the compound-rule test uses a seed where the player does not retire; Monza is loaded once.

## Feedback pass 3: road-decided overtaking, fight panels, Sao Paulo, natural corner speeds (2026-11-06)

Owner feedback: overtakes unpredictable with speed differences "out of nowhere"; the field bunches up and the
midfield gets stuck; add Sao Paulo (from an outline picture); defence and attack need clear visual feedback
(both cars' lines, colored options, grip); separate attack and defence; multi-car passes, DRS passes, late
braking; cars slow too much in places; AI drivers all identical; more car indicators (grip/wear, punctures,
compound). Owner choices: slow motion while choosing, show factors but no percentages, prompts plus an attack
key, ignore deliverables for now.

**Research used:** F1 overtaking practice (out-braking down the inside, the outside line needs the car to be
level, DRS/slipstream passes on straights, switchbacks after a defender covers the inside); F1 Manager's
colored tire-wear blocks with a "danger" band and pace modes; Motorsport Manager's red wear zone and the common
complaint that abstract circles do not show who is alongside; Gran Turismo's tire colors.

**Done**
- Overtaking rebuilt (`LineChoice`, `LinePlan`, `Battle`, `BattleManager`, `BattleResolver`, `LineChoicePolicy`).
  The old hidden dice roll at the braking point and the forced "pass speed" are gone. Each fight choice is a
  real path the car drives (move across to a lane before braking, hold it to the apex, rejoin on the exit),
  late braking shifts the braking point by 25 m with a lock-up risk, an Inside line costs exit speed and an
  Outside line costs entry speed. Any two cars may run side by side when there is room. Who gets a corner is
  settled at turn-in by a claim built from visible factors: how far alongside, inside or outside, grip, late
  braking, lock-ups, track difficulty, small noise; close calls can end in contact (and a puncture). The loser
  lifts and tucks in behind (never below 60% pace, never longer than 6 s, never a circular give-way). Cars that
  are not attacking follow the car ahead instead of drifting alongside; a "Stay behind" car drops back.
  Result: 10 to 40 real passes per AI race, multi-car passes on straights, switchbacks, lock-ups.
- Dirty air: up to 4% corner speed lost within a second behind another car; the tow only applies when in its wake.
- Side-by-side guard: runs up to 4 passes, moves back the car that moved, stays inside the track edges.
  0 overlaps in 8 AI races on both tracks.
- Fight UI: separate ATTACK (red) and DEFEND (blue) panels, buttons color-matched to lines drawn on the track
  (`LinePreview`: the exact LinePlan paths, rival red dashed, a BRAKE / BRAKE LATE marker), factor rows colored
  green/red, L for late braking, C to attack on demand. Time slows only until a choice is made. The camera frames
  the car and the corner above the panel (`Camera`).
- Car indicators: compound-colored tire edges, tire-condition bar on nearby cars, blinking red punctured tire,
  lock-up smoke, red ring on the rival. Punctures: worn-out tires (wear above 95%) and contact; the car slows to
  55% and is called in.
- AI variety: every driver's line wanders a few meters from the racing line (more on lap 1), start reaction
  times differ, and each driver has a battery style and decides afresh on every 500 m stretch.
- Corner speeds: taken from the traced/hand-built layout at real width (capped at 30 m radius) instead of the
  wide, rounded drawn track, so chicanes stay slow and fast corners fast. Braking is 1.5 times cornering grip
  plus drag (F1 cars brake at up to about 5 g).
- Corner rounding for the drawn track now spreads too-sharp turning over a longer arc (keeps each corner's total
  turn), so U-shaped sections and hairpins keep their shape instead of flattening.
- Corners: chicane halves and corners that brake before the previous apex are merged.
- Sao Paulo: traced from the owner's outline picture with the new `tools/trace_outline.py` (Zhang-Suen
  thinning, ordered walk, counter-clockwise, start line on the main straight, pieces closer than 62 m pushed
  apart by moving the tighter curve as a whole). Overlay checked against the picture. 4309 m, 74 s approximate
  lap, Senna S, Descida do Lago, the infield U and Bico de Pato, Junção.
- Tests: corner-claim rules replace the old pass-probability test; the full-race test now checks that no two
  cars are ever drawn on top of each other (except pit lane and retiring cars); 34 tests.
- Removed 40 constants of the old pass system.

## Feedback pass 4: one Overtake button, a real start, cleaner screen, fewer trains (2026-11-07)

Owner feedback: the screen and the fight system had become cluttered and hard to use; DRS seemed gone;
wanted one Overtake button (ERS + DRS + late braking + more), slow motion when in range, A/S/D lines after
pressing it; passing was hard (track too thin); the start was too fast and should be an overtaking chance with
color-coded directions; cars backed up badly; the leader was hard to chase; part of Interlagos merged.
Owner choices: boost limited by battery; defending is just picking a line; lights + reaction launch + slow-mo to
Turn 1; equal cars but Push a bit stronger and AI switching modes in the background (no AI-vs-AI indicators);
SHIFT = Overtake; slightly wider track and slimmer cars; keep only the timing tower and gauges on screen; keep
only the player marker, tire colors and puncture/lock-up effects on the cars.

**Done**
- Overtake (SHIFT): spends 15% of the battery, then until the next corner exit (at most 500 m) the car deploys
  everything, runs 2.5% faster, may open DRS anywhere in a DRS zone when within a second, and brakes late into
  the corner. Then A Inside / S Optimal / D Outside. Doing nothing means following. Computer attackers use the
  same boost. The player's normal battery use keeps a reserve so the boost is there when needed.
- Defending: a blue panel only when a car behind really attacks; A cover inside, S optimal, D cover outside.
- Panel rebuilt: one place, one title, one big SHIFT OVERTAKE button or three colored line buttons, three facts
  (speed, grip, boost/DRS). Fights open at least 2 s before braking.
- Start: five lights (0.8 s apart, random hold), SHIFT reaction launch (jump start and slow getaway), slow
  motion (0.75x) until the leader is through Turn 1, every car takes its own line into Turn 1 straight off the
  grid (computer drivers pick at random, some brake late), colored lines and the Overtake boost for the player.
  Timing tower shows gaps in meters for the first 20 s.
- Screen: radio is a pop-up at the top; minimap off (M); bottom bar with position/speed/laps/pit plan and key
  caps; gauges show BOOST and a DRS badge; player marked by a yellow chevron; rings, tire bars, warning triangle
  and preview dots removed.
- Room: track 62 m wide (was 56), cars 6.2 m wide (was 7.2), side-by-side clearance 7.4 m, lanes at 16 m.
- Interlagos merge fixed for good: building a track now alternates corner rounding with a gentle separation step
  that keeps far-apart pieces at least track width plus 12 m apart (capped 1 m per pass, spread smoothly, the
  tighter curve moves). Closest pieces at Sao Paulo now 71 m apart.
- Trains: a limping car (puncture or time loss) moves to the edge and the cars behind drive past (one punctured
  car had held up twelve cars for a lap); cars not fighting follow at 0.35 s instead of nose to tail; a car that
  lost a corner eases off gently; stronger tow; AI drivers stuck behind the same car grow bolder; AI drivers
  switch between Push, Balanced and Conserve every couple of kilometers. Push is now +3% pace.
- Engineer stays quiet during the start.

## Simplification rewrite (2026-11-09), from owner feedback

**Asked:** strip the code and the game down so three high-school students can understand and explain every part:
fewer classes, short one-line Javadoc, no feature whose code costs more than it adds. Remove the Strategy Lab,
Monza, the menu/setup screens and the pre-race planner; 10 drivers; simpler overtakes, AI and racing line.
Owner choices: keep rain and the start lights (simplified); overtakes are a simple chance plus a lane swap; keep a
few data structures where they fit naturally; go straight into the race, R restarts.

**Done**
- Rewritten from scratch: 87 classes and about 17,000 lines down to 9 classes and about 1,700 lines
  (`Track`, `Compound`, `Car`, `Fight`, `Ai`, `Race`, `PitWall`, `TrackView`, `Hud`).
- Removed: menu, track select, setup, results charts, Strategy Lab, Monza, safety car, retirements, brakes,
  tire temperature, punctures, minimap, raster renderer, data loaders, pre-race pit planner.
- Interlagos geometry baked into `data/interlagos.csv` (the old rounding and separation code is gone).
- Racing line: one smoothing loop. Speed: corner limit from curvature plus one backward braking pass.
- Overtakes: a fight opens within 1 s of the car ahead before a braking point; at the braking point
  `Fight.chance` decides; on success the cars swap lanes. Start fights happen naturally off the grid.
- Pit stops: keys 1-5 box this lap for that tire; B cancels.
- Data structures: ArrayList (cars, fights), PriorityQueue (weather), ArrayDeque (radio, keys), HashMap
  (radio cooldowns), 2D array (lap times).
- Fixed a deadlock found while testing: two cars at exactly the same distance each blocked the other.

**Verified**
- `./test.sh`: 20 full races finish with all 10 cars, winner's best lap 70-80 s, rain and rain tires happen,
  pit stop fits the chosen tires, chance formula range/boost/cover, tire grip and AI tire choice.
- `tools/audit.sh`: all checks pass. `--selftest`: about 60 FPS.
- Played through a full race on a virtual display: lights, launch, attack and defend prompts with colored lines,
  a pass, a pit stop, the finish banner.

**Not verified:** double-click launch on the owner's Mac.
