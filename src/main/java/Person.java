/*
 *  AS91907.Person
 *  Last Updated: 01/08/2025
 *  Purpose: A unit of the simulation. Moves around the grid, has a state, and can infect others on the same tile.
 */

public class Person {

    // Labels for different kinds of states. Used externally
    public static final char NORMAL = 0;
    public static final char INFECTED = 1;
    public static final char IMMUNE = 2;
    public static final char EMPTY = 3; // Not possible for a Person to be in this state, but useful to be considered a state for rendering

    private final Simulation s; // The simulation it came from

    private int x, y; // Its position

    private int state; // Internal state. == 0 -> normal, <= INFECTION_COOLDOWN -> infected, <= IMMUNITY_COOLDOWN immune, wraps back to 0
    public char state() { // Converts internal state to an external state
        if (state == 0) return NORMAL;
        if (state <= s.INFECTION_COOLDOWN) return INFECTED;
        return IMMUNE;
    }

    private boolean infected = false; // Whether we will become infected this round
    public Person next = null; // Persons are stored in linked lists, this is the pointer

    public Person(Simulation s, char state) { // Constructor, intialised with a state
        this.s = s;
        // Randomises the position
        x = (int) (Math.random() * s.WIDTH);
        y = (int) (Math.random() * s.HEIGHT);
        reposition(); // Adds the Person to the movement array
        this.state = switch (state) { // Initialises the state
            case NORMAL -> 0;
            case INFECTED -> 1;
            case IMMUNE -> s.INFECTION_COOLDOWN + 1;
            default -> throw new IllegalStateException(); // Should not be possible
        };
    }
    private void move(int i) { // Used internally for movement
        switch (i) {
                            // Stay still
            case 1 -> y --; // Move up
            case 2 -> x --; // Move left
            case 3 -> y ++; // Move down
            case 4 -> x ++; // Move right
        }
    }
    public void move() { // Moves the person in a random direction
        boolean[] available = new boolean[] { // Stores whether each move is possible
                true, // Stay still
                y > 0, // Move up
                x > 0, // Move left
                y < s.HEIGHT - 1, // Move down
                x < s.WIDTH - 1 // Move right
        };
        // Calculates how many options are available
        int options = 0;
        for (boolean b : available) if (b) options ++;

        // Picks a random movement choice out of the available options
        int choice = (int) (Math.random() * options);
        // Looks for that movement choice

        int i = 0; // Initialses the movement index
        while (choice > 0) { // While we still need to find the choice. This code would not work if the first option is always true
            i ++; // Moves forwards
            if (available[i]) choice --; // If this is a possible move, decrement the choice, and check the (choice > 0) condition
        }
        move(i); // Executes the corresponding move
        reposition(); // Reassigns itself to the movement array
    }
    private void reposition() { // Moves this person into its corresponding list in the movement array, sorting itself by state.
        next = s.movement[x][y]; // Sets the pointer to the head of the list
        if (next == null || state <= next.state) s.movement[x][y] = this; // If this should be the new head, do it
        else { // Otherwise, next will be the Person before this
            Person nextnext = next.next; // Creates a second pointer, pointing after next.
            while (nextnext != null && state > nextnext.state) { // While we haven't reached the end of the loop, and the current state is still less than ours
                // Shift the pointers forwards
                next = nextnext;
                nextnext = next.next;
            }
            next.next = this; // Inserts this into the list
            next = nextnext; // Reconnects the end of the list
        }
    }
    public void spread() { // Handles spreading infection
        // This probably needs a bit of explaining:
        // We start by checking if our state is INFECTED, otherwise we wouldn't be able to spread anything.
        // After that, we loop through the list of Persons at our position, ending the search if we reach the end of the list, or someone who is not NORMAL.
        // This works because the list is sorted by state, so all the NORMAL (infectable) Persons are at the start.
        // For each Person, if we hit the random chance, we set them to be infected.
        if (state() == INFECTED) for (Person p = s.position[x][y]; p != null && p.state() == NORMAL; p = p.next) if (Math.random() < s.INFECTION_CHANCE) p.infected = true;
    }

    public char update() { // Handles updating states
        if (infected) { // If we have been infected
            infected = false; // Reset the flag
            state = 1; // Set our state to 1 (start of infected)
            s.infections ++; // Increment the infections tally
        } else if (state > 0) { // Otherwise, if we are not NORMAL
            state ++; // Increment our state
            if (state > s.IMMUNITY_COOLDOWN) state = 0; // If we have reached the end of our immunity, reset to NORMAL
        }
        return state(); // Returns our state, for tallying by the simulation
    }
}
