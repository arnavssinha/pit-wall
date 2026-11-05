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
