/**
 * Where a car is in a pit stop.
 */
public enum PitPhase {

    /** On track, not pitting. */
    NONE,

    /** In the pit lane, driving to the pit box at the speed limit. */
    IN_LANE,

    /** Stopped in the box while the crew changes tires. */
    STOPPED,

    /** In the pit lane, driving from the box to the pit exit at the speed limit. */
    LEAVING,

    /** Past the pit exit, moving back onto the racing line when there is room. */
    MERGING
}
