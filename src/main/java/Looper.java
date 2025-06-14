import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class Looper implements Iterator<Person> { // Loops through each person and does tasks

    // Used to keep track of its position
    private boolean going = false;
    private boolean horizontal = true;
    private int i = 0;
    private Person current;

    public void reset() { // Resets its pointer to the correct position
        current = Main.row[0];
    }

    public Tick go() { // Main tasks. Uses Iterator methods
        forEachRemaining(Person::move); // Changes positions
        forEachRemaining(Person::spread); // Spreads infections
        AtomicInteger normal = new AtomicInteger(), infected = new AtomicInteger(), immune = new AtomicInteger(); // Initialises tallies
        forEachRemaining(person -> (switch (person.update()) { // Updates states. The function returns the person's state, so it is used to tally.
            case NORMAL -> normal;
            case INFECTED -> infected;
            case IMMUNE -> immune;
        }).getAndIncrement());
        if (infected.get() == 0) Main.TICKS = 1; // If none are infected, end the simulation
        return new Tick(normal.get(), infected.get(), immune.get()); // Returns a new tally to be added to the history
    }

    @Override
    public boolean hasNext() { // Returns whether there are still people left to check. Also adjusts the pointer, because its more convenient to do it here
        if (going) current = current.pointer; // If this isn't the first time, shift the pointer down the list
        else going = true;
        while (going && current == null) { // If there is still more to check, and we have a null pointer
            i++; // Increment the index
            if (i >= (horizontal ? Main.HEIGHT : Main.WIDTH)) { // If the index is past our current array's length
                i = 0; // Reset the index
                horizontal = !horizontal; // Switch the array
                going = horizontal; // If we're horizontal, we've checked everything
            }
            current = (horizontal ? Main.row : Main.column)[i]; // Gets the next pointer
        }
        return going;
    }

    @Override
    public Person next() {
        return current;
    }
}
