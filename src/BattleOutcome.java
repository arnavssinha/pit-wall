/**
 * How a battle for one corner ended.
 */
public enum BattleOutcome {

    /** The attacker got past. */
    PASSED,

    /** The attacker tried but the defender kept the place. */
    HELD,

    /** The attacker chose Hold, so no move was made. */
    NO_ATTEMPT,

    /** The attacker locked a wheel, lost time and stayed behind. */
    LOCKED_UP,

    /** The attacker ran wide, lost more time and stayed behind. */
    RAN_WIDE,

    /** The cars touched; both lost time and the attacker stayed behind. */
    CONTACT,

    /** The car ahead was lapped (or already finished) and let the faster car through. */
    BLUE_FLAG
}
