public class Person {
    private final Simulation s;

    public int x, y;

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
        x = (int) (Math.random() * s.WIDTH);
        y = (int) (Math.random() * s.HEIGHT);
        reposition();
        this.state = state; // If infected, sets state to somewhere in infected range. Otherwise, normal
    }
    public void move() { // Moves the person in a random direction
        boolean[] available = new boolean[] {
                true,
                x > 0,
                y > 0,
                x < s.WIDTH - 1,
                y < s.HEIGHT - 1
        };
        int o = 0;
        for (boolean b : available) {
            if (b) o ++;
        }
        int choice = (int) (Math.random() * o);
        int i = 0;
        while (choice > 0) {
            i ++;
            if (available[i]) choice --;
        }
        move(i);
        reposition();
    }
    private void move(int i) {
        switch (i) {
            case 0 -> {}
            case 1 -> x --;
            case 2 -> y --;
            case 3 -> x ++;
            case 4 -> y ++;
        }
    }
    private void reposition() {
        next = s.movement[x][y];
        if (next == null || state <= next.state) {
            s.movement[x][y] = this;
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
            Person p = s.position[x][y];
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
