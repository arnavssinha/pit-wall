# Pit Wall

An F1 race game written in Java with the ACM graphics library. You race one car in a 10-lap sprint at
Interlagos (São Paulo) against nine computer drivers. The car steers itself; you decide when to attack,
how to defend, how hard to push and when to pit.

## Play it

1. **Install Java 21** (Temurin 21 from <https://adoptium.net/temurin/releases/?version=21>).
   Java 17 also works. Java 26 and newer do not (they removed classes the ACM library needs).
2. **Download the project** (green **Code** button, **Download ZIP**) and unzip it.
   Keep `PitWall.jar` and the `lib` folder together.
3. **Double-click `PitWall.jar`.** If macOS blocks it, right-click it, choose **Open**, then **Open** again.
   If nothing happens, open Terminal, type `java -jar `, drag `PitWall.jar` in and press Return.

## Controls

| Key | What it does |
|---|---|
| SHIFT | At the start: launch when the lights go out. In the race: Overtake (uses boost battery) |
| A / S / D | Pick a line in a fight: Inside / Optimal / Outside (attack or defend) |
| Q / W / E | Pace: Conserve / Normal / Push |
| 1 - 5 | Box this lap for Soft / Medium / Hard / Intermediate / Wet tires |
| B | Cancel the pit stop |
| P or Space | Pause |
| T | Time speed 1x / 2x / 4x |
| R | Restart with a new race |

## How a race works

- **Start.** Five red lights come on one by one. Press SHIFT when they go out. Pressing too early is a
  jump start and you get away slowly. The game runs in slow motion until Turn 1.
- **Fights.** When you are less than a second behind a car before a braking zone, an ATTACK panel opens.
  Pick a line with A/S/D and press SHIFT for the Overtake boost. The panel shows your chance. When a car
  attacks you, a DEFEND panel opens. Cover the line you think they will take. The colored dots on the
  track show the three lines.
- **The chance** (`Fight.chance`) starts at 35% and goes up with better pace, better tire grip, the boost
  and DRS. It goes down with a bigger gap and when the defender covers your line.
- **Tires** wear each lap (Softs fastest, Hards slowest). Worn tires have less grip. Push mode is faster
  but wears tires and charges more boost.
- **Rain.** In about half the races it rains for a few laps. The track gets wet slowly and dries slowly.
  Intermediates and Wets are faster on a wet track; slicks are faster on a dry one. The engineer tells
  you on the radio when to change.
- **Finish.** The timing tower on the left shows the order. When you cross the line after lap 10 the
  game shows your finishing position. Press R to race again.

## The code (9 classes in `src/`)

| Class | Job |
|---|---|
| `Track` | Loads the centerline, finds the racing line and the safe speed at every point |
| `Compound` | Enum of the five tire types: grip and wear |
| `Car` | One car's state: position, speed, tires, boost, laps |
| `Fight` | One overtaking attempt and its chance formula |
| `Ai` | The computer drivers' choices: pace, pit stops, attack and defend |
| `Race` | Runs the race one 1/60 s step at a time |
| `PitWall` | Main class: window, game loop, keys, start lights |
| `TrackView` | Draws the track and the cars (ACM) |
| `Hud` | Draws the timing tower, gauges, radio and prompts (ACM) |

The simulation classes (the first six) do not import any graphics, so the tests can run a whole race with no window.

**Data structures used:**
- `ArrayList` for the cars and the open fights.
- `PriorityQueue` for upcoming weather changes, earliest first.
- `ArrayDeque` for the radio messages and the key queue.
- `HashMap` for when each engineer reminder was last said.
- A 2D array `lapTimes[car][lap]` for lap times.

UML diagram: `docs/uml/classes.png`.

## Where the data comes from

- **Interlagos layout: approximate.** `data/interlagos.csv` was traced from a circuit outline picture,
  then rounded and spaced out so the wide game track does not overlap itself. The track is drawn about
  four times wider than a real one so the cars are easy to see. The pit lane is drawn on the outside.
- **Lap time** (about 74 s) comes from the speed limits in `Track` and is close to a real race lap.
- **Drivers** are the 2026 line-up, two per team for five teams. All cars have the same speed apart from
  a small random difference each race.

## For developers

```bash
./build.sh                           # compile src/ and rebuild PitWall.jar
./test.sh                            # headless tests (test/SimTests.java)
tools/audit.sh                       # course rules: Javadoc, imports, no warnings
tools/make_uml.sh                    # redraw docs/uml/classes.png
tools/make_submission.sh             # copy every .java file and the UML PNG into submission/
java -jar PitWall.jar --selftest     # open the window, race 8 s, print the frame rate, exit
```

On Windows use `build.bat` and `run.bat`. In an IDE, add `lib/acm.jar` to the classpath, mark `src` as
the source folder and run `PitWall` from the project folder (so `data/interlagos.csv` is found).

| Problem | Fix |
|---|---|
| `NoClassDefFoundError: acm/program/GraphicsProgram` | `lib/acm.jar` is not next to `PitWall.jar`. Unzip the whole project. |
| `UnsupportedClassVersionError` | Java is older than 11. Install Java 21. |
| Errors about `java.applet` or `JApplet` | Java is 26 or newer. Install Java 21. |
