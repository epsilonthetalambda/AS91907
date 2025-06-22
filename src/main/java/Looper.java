import java.util.function.Consumer;

public class Looper { // Loops through each person and does tasks
    private final int[] count = new int[3];

    public Tick go() { // Main tasks. Uses Iterator methods
        forEachRemaining(Person::move); // Changes positions
        Main.finishMovement();
        forEachRemaining(Person::spread); // Spreads infections
        for (int i = 0; i < 3; i++) count[i] = 0;
        forEachRemaining(person ->
            count[switch (person.update()) { // Updates states. The function returns the person's state, so it is used to tally.
                case NORMAL -> 0;
                case INFECTED -> 1;
                case IMMUNE -> 2;
            }] ++
        );
        if (count[1] == 0) Main.TICKS = 1; // If none are infected, end the simulation
        return new Tick(count); // Returns a new tally to be added to the history
    }

    private void forEachRemaining(Consumer<? super Person> action) {
        for (int x = 0; x < Main.WIDTH; x++) {
            for (int y = 0; y < Main.HEIGHT; y++) {
                Person p = Main.position[x][y];
                while (p != null) {
                    Person next = p.next;
                    action.accept(p);
                    p = next;
                }
            }
        }
    }
}
