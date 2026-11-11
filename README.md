# Pit Wall — ACM graphics feasibility prototype

**Scope:** This is the first submission / risk prototype, not the finished game.
It tests rendering a circular track and a moving, rotated car with the required
Stanford ACM Java graphics library.

## What it demonstrates

- Track.java: parametric circular track and ACM GOval rendering.
- Car.java: coordinates, heading, and rotated ACM GPolygon rendering.
- PitWallDemo.java: executable GraphicsProgram, animating one lap.
- A multi-dimensional double[][] array for twelve sample checkpoints.
- A List of double[] positions backed by ArrayList for simple telemetry.
- GeometryTest.java: headless verification of geometry and 2D arrays.

The existing main-branch dependency lib/acm.jar is reused here.
No Graphics2D drawing remains in the prototype source.

## Compile, run, test

Install **Java 17 or Java 21**. From the project root, run on macOS/Linux:

~~~bash
bash build.sh
java -jar build/PitWallDemo.jar
bash test.sh
bash docs.sh
~~~

Or run directly with this classpath:

~~~bash
java -cp "build/classes:lib/acm.jar" PitWallDemo
~~~

On Windows, add lib/acm.jar to your IDE project library and run PitWallDemo.
If using a Windows command-line classpath, separate paths with a semicolon.

JavaDoc output is created in docs/api. Standard JavaDoc accepts the @author tag
on classes, but the course asks for method-level attribution, so docs.sh adds
the custom option: -tag 'author:a:Author:'.

## Method ownership: confirmation required

**The method-level @author annotations are proposed ownership assignments,
not yet verified individual authorship. Confirm them with the students before
submission and correct any tags that do not reflect their actual work.**

| Proposed owner | Methods |
|---|---|
| @harnoor | Original Car/Track geometry, constructors, update, getters; track sampling |
| @samuel | ACM drawing, polygon corner conversion, demo animation loop |
| @arnav | Main launcher, headings, status labels and text, checkpoint display, simple tests |

Harnoor's authorship of the original simplified geometry is supported by
the first simplified branch commit. New or revised code should be reviewed and
understood by the named student before the @author claim is finalized.
If multiple people contribute to a method, credit each actual contributor.

## UML and full-submission checklist

The class diagram is at docs/uml/classes.md (rendered by GitHub).

This first deliverable specifically demonstrates graphics feasibility. Remaining
**final** deliverables include topic approval, Markdown README with instructions,
code-review presentation, a short 2–4 minute video demo with each team member
visible, a second music video with each member visible, code with JavaDoc, and
a UML diagram. Videos and presentation must be produced by the students.

## Prototype boundaries

A circular model is intentionally used instead of the full Interlagos map.
The prototype does not include multiplayer, racing AI, pit strategy, overtakes,
weather, collisions or user input. Its purpose is proving ACM rendering, a
rotated moving vehicle, and the required Java data structures.
