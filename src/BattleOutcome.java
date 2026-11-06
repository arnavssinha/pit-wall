/**
 * How a fight for one corner ended.
 */
public enum BattleOutcome {

    /** The attacker got past. */
    PASSED,

    /** The defender kept the place. */
    HELD,

    /** The attacker chose to stay behind. */
    NO_ATTEMPT,

    /** A late-braking car locked its brakes and ran wide. */
    LOCKED_UP,

    /** The two cars touched; both lost time. */
    CONTACT,

    /** A lapped or finished car let a faster car through. */
    BLUE_FLAG
}
