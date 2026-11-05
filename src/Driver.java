/**
 * A real driver from the roster file: number, three-letter code, name and team.
 * Drivers carry no invented skill ratings (the brief forbids them).
 */
public final class Driver {

    /** Car number. */
    private final int number;

    /** Three-letter timing code, such as "LEC". */
    private final String code;

    /** Full name. */
    private final String name;

    /** The driver's team. */
    private final Team team;

    /**
     * Creates a driver.
     *
     * @param number car number
     * @param code three-letter code
     * @param name full name
     * @param team the team
     */
    public Driver(int number, String code, String name, Team team) {
        this.number = number;
        this.code = code;
        this.name = name;
        this.team = team;
    }

    /**
     * Returns the car number.
     *
     * @return the number
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the three-letter code.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the full name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the team.
     *
     * @return the team
     */
    public Team getTeam() {
        return team;
    }
}
