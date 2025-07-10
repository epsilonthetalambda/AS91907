public class Person {
    private final Simulation s;

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
    private int x() {
        if (horizontal) return pos();
        else return lane;
    }
    private int y() {
        if (horizontal) return lane;
        else return pos();
    }

    private int state; // Current state. == 0 -> normal, <= INFECTION_COOLDOWN -> infected, <= IMMUNITY_COOLDOWN immune, wraps back to 0
    public enum State { // Used externally
        NORMAL,
        INFECTED,
        IMMUNE
    }
    public State state() { // Converts integer state to a State
        if (state == 0) return State.NORMAL;
        else if (state <= s.INFECTION_COOLDOWN) return State.INFECTED;
        else return State.IMMUNE;
    }

    private boolean infected = false; // Whether we will become infected next round
    public Person next = null; // The next Person in the list

    public Person(Simulation s, int state) { // Constructor, whether we start infected
        this.s = s;
        horizontal = Math.random() < 0.5; // Randomises axis
        lane = (int) (Math.random() * (horizontal ? this.s.HEIGHT : this.s.WIDTH)); // Randomises the lane
        MAX_POS = (horizontal ? this.s.WIDTH : this.s.HEIGHT) - 1; // Stores the max pos
        step = (int) (MAX_POS * 2 * Math.random()); // Randomises the current step
        reposition();
        this.state = state; // If infected, sets state to somewhere in infected range. Otherwise, normal
    }
    public void move() { // Increments and wraps the step
        step ++;
        if (step == 2 * MAX_POS) step = 0;
        reposition();
    }
    private void reposition() {
        next = s.movement[x()][y()];
        if (next == null || state <= next.state) {
            s.movement[x()][y()] = this;
        } else {
            Person nextnext = next.next;
            while (nextnext != null && state > nextnext.state) {
                next = nextnext;
                nextnext = next.next;
            }
            next.next = this;
            next = nextnext;
        }
    }
    public void spread() {
        if (state() == State.INFECTED) { // Tries to infect others if able
            Person p = s.position[x()][y()];
            while (p != null && p.state() == State.NORMAL) {
                if (Math.random() < s.INFECTION_CHANCE) p.infected = true;
                p = p.next;
            }
        }
    }

    public State update() { // Updates the person's state. Returns it for tallying
        if (infected) {
            infected = false;
            state = 1;
        } else if (state > 0) {
            state ++;
            if (state > s.IMMUNITY_COOLDOWN) state = 0;
        }
        return state();
    }
}
