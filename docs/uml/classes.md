# UML class diagram — first deliverable

This models the prototype on this branch, not the complete game on main.

~~~mermaid
classDiagram
    GraphicsProgram <|-- PitWallDemo
    PitWallDemo --> Track : creates and draws
    PitWallDemo --> Car : animates
    Car --> Track : update(t, track)
    PitWallDemo ..> GCanvas : uses
    Track ..> GCanvas : draws on
    Car ..> GCanvas : draws on
    class PitWallDemo {
        +main(String[]) void
        +run() void
        -drawCheckpoints(GCanvas, Track) void
        -addHeading(GCanvas) void
        -addStatusLabel(GCanvas) GLabel
        -formatStatus(Car, int) String
    }
    class Track {
        -double radius
        -double trackWidth
        +getX(double) double
        +getY(double) double
        +getHeading(double) double
        +sampleCenterline(int) double[][]
        +draw(GCanvas, double, double, double) void
        +getRadius() double
        +getTrackWidth() double
    }
    class Car {
        -double length
        -double width
        -double x
        -double y
        -double angle
        +update(double, Track) void
        +draw(GCanvas, double, double, double) GPolygon
        -addCorner(GPolygon, double, double) void
        +getX() double
        +getY() double
        +getAngle() double
    }
~~~

Data structures: Track.sampleCenterline returns double[][]; PitWallDemo.run
saves positions in a List<double[]> implemented by an ArrayList<double[]>.
