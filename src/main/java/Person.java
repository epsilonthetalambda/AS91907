public class Person {
    private final boolean horizontal; // Whether we are going horizontally or vertically
    private final int lane; // The lane we are in

    private int step; // The current step of our movement. Each non-edge position has 2 step values
    private final int MAX_POS; // The maximum position pos() can return
    public int pos() { // Converts step to a position
        if (step <= MAX_POS) {
            return step;
        } else {
            return 2 * MAX_POS - step;
        }
    }

    private int state; // Current state. == 0 -> normal, <= INFECTION_COOLDOWN -> infected, <= IMMUNITY_COOLDOWN immune, wraps back to 0
    public enum State { // Used externally
        NORMAL,
        INFECTED,
        IMMUNE
    }
    public State state() { // Converts state to a State
        if (state == 0) return State.NORMAL;
        else if (state <= Main.INFECTION_COOLDOWN) return State.INFECTED;
        else return State.IMMUNE;
    }

    private boolean infected = false; // Whether we will become infected next round
    public Person next; // The next Person in the list

    public Person(boolean infected) { // Constructor, whether we start infected
        horizontal = Math.random() < 0.5; // Randomises axis
        lane = (int) (Math.random() * (horizontal ? Main.HEIGHT : Main.WIDTH)); // Randomises the lane
        MAX_POS = (horizontal ? Main.WIDTH : Main.HEIGHT) - 1; // Stores the max pos
        step = (int) (MAX_POS * 2 * Math.random()); // Randomises the current step
        next = (horizontal ? Main.row : Main.column)[lane]; // Sets the pointer to the previous person in the same lane
        (horizontal ? Main.row : Main.column)[lane] = this; // Adds itself to the stack
        state = (infected ? (int) (Math.random() * Main.INFECTION_COOLDOWN) + 1 : 0); // If infected, sets state to somewhere in infected range. Otherwise, normal
    }
    public void move() { // Increments and wraps the step
        step ++;
        if (step >= 2 * MAX_POS) step = 0;
    }
    public void spread() {
        if (state() == State.INFECTED) { // Tries to infect others if able
            infect((horizontal ? Main.row : Main.column)[lane], pos());
            infect((horizontal ? Main.column : Main.row)[pos()], lane);
        }
    }
    private void infect(Person p, int pos) { // Takes a person, runs down their list, checks for matches in position, then tries to infect
        p = Main.search(p, pos);
        while (p != null && p.pos() == pos) {
            if (p.state() == State.NORMAL && Math.random() < Main.INFECTION_CHANCE) p.infected = true;
            p = p.next;
        }
    }

    public State update() { // Updates the person's state. Returns it for tallying
        if (infected) {
            infected = false;
            state = 1;
        } else if (state > 0) {
            state ++;
            if (state > Main.IMMUNITY_COOLDOWN) state = 0;
        }
        return state();
    }
    public Person popNext() {
        Person p = next;
        next = null;
        return p;
    }
    public static boolean ordered(Person a, Person b) {
        if (a == null) return false;
        if (b == null) return true;
        return (a.pos() <= b.pos());
    }
}
