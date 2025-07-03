import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Simulation extends SwingWorker<Void, Void> {
    // Simulation parameters
    public final int WIDTH; // Width of the simulation
    public final int HEIGHT; // Height of the simulation
    public final double INFECTION_CHANCE; // Chance for each infected person to infect a normal person
    public final int INFECTION_COOLDOWN; // Number of ticks since infection that a person can infect others
    public final int IMMUNITY_COOLDOWN; // Number of ticks since infection that a person cannot get reinfected
    public int TICKS; // Number of ticks remaining

    public Color EMPTY_COLOR = Color.BLACK;
    public Color NORMAL_COLOR = Color.GREEN;
    public Color INFECTED_COLOR = Color.RED;
    public Color IMMUNE_COLOR = Color.BLUE;

    public Person[][] position; // Stores the People according to their positions
    public Person[][] movement; // People move here, sorting themselves, then position references this.

    public final JFrame main; // The main window, allowing for toggling of visualisations, and termination.
    public final Render visualisation; // Displays each cell's occupants in real time
    private final ArrayList<Tick> history; // Stores the total counts of each population each tick.

    public boolean running = true; // Whether to continue running as usual
    public boolean output = true; // Whether to output to CSV

    public Simulation(int width, int height, int people, int infected, double infectionChance, int infectionLength, int immunityLength, int ticks) {
        super();
        // Initialisation of parameters
        WIDTH = width;
        HEIGHT = height;
        INFECTION_CHANCE = infectionChance;
        INFECTION_COOLDOWN = infectionLength;
        IMMUNITY_COOLDOWN = INFECTION_COOLDOWN + immunityLength;
        System.out.println(WIDTH);
        System.out.println(HEIGHT);
        TICKS = ticks;
        // Initialisation of cells
        position = new Person[WIDTH][HEIGHT];
        movement = new Person[WIDTH][HEIGHT];

        for (int i = 0; i < people; i++) { // Generates the people
            new Person(this, i < infected);
        }

        visualisation = new Render(this, "Simulation", WIDTH, HEIGHT) { // Creates the visualisation
            private int gridW, gridH; // The size of each cell in the grid, min of 1
            @Override
            public void newImage() {
                int newGridW = Math.max(w / s.WIDTH,1), newGridH = Math.max(h / s.HEIGHT,1); // Calculates what the grid width and height would be
                if (gridW != newGridW || gridH != newGridH) { // If the grid size is not correct
                    gridW = newGridW; gridH = newGridH; // Update
                    image = new Image(s, gridW * s.WIDTH, gridH * s.HEIGHT) { // Create a new Image
                        @Override
                        public void render() {
                            // For each cell
                            for (int x = 0; x < s.WIDTH; x++) {
                                for (int y = 0; y < s.HEIGHT; y++) {
                                    Person.State rendered = null; // Stores which state should be displayed
                                    Person p = s.position[x][y];
                                    if (p != null) { // If there is at least one Person

                                    /*  When the rendering is happening, each list is ordered NORMAL, INFECTED, IMMUNE.
                                        The order off priority for rendering is INFECTED, IMMUNE, NORMAL.
                                        We start with NORMAL, and if no other states show up, it stays.
                                        If an INFECTED shows up, we can immediately break with that state.
                                        If we get to an IMMUNE, there has not been any INFECTED, as if there had been, we would have broken. That means we can immediately break with that state.  */

                                        rendered = Person.State.NORMAL; // Defaults to NORMAL
                                        for (; p != null; p = p.next) { // Loops through all Persons at that tile
                                            if (p.state() != Person.State.NORMAL) { // If someone is not NORMAL
                                                rendered = p.state(); // Render that state
                                                break;
                                            }
                                        }
                                    }
                                    g.setColor(switch (rendered) { // Sets the colour of the cell
                                        case null -> s.EMPTY_COLOR;
                                        case NORMAL -> s.NORMAL_COLOR;
                                        case INFECTED -> s.INFECTED_COLOR;
                                        case IMMUNE -> s.IMMUNE_COLOR;
                                    });
                                    g.fillRect(x * gridW, y * gridH, gridW, gridH);  // Draws the cell
                                }
                            }
                        }
                    };
                }
            }
        };

        finishMovement();

        // Creates the main GUI for the simulation
        main = new JFrame("Simulation");
        main.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                terminate(); // Ends the program without writing
                main.dispose(); // Gets rid of itself
            }
        });
        // Button to toggle visibility of the visualisation
        JButton toggleVisualisation = new JButton("Visualisation");
        toggleVisualisation.addActionListener(l -> visualisation.toggle());
        main.add(toggleVisualisation);
        main.setVisible(true);

        history = new ArrayList<>(Math.max(ticks, 0) + 1); // Initialises history with enough initial capacity, unless endless
        history.add(new Tick(people - immunityLength, infected, 0)); // Adds the initial state
    }

    @Override
    protected Void doInBackground() {
        // Used to iterate through each person
        final int[] count = new int[3];

        for (; running && TICKS != 0; TICKS--) {
            forEachRemaining(Person::move); // Changes positions
            finishMovement();
            forEachRemaining(Person::spread); // Spreads infections
            for (int i = 0; i < 3; i++) count[i] = 0;
            forEachRemaining(person ->
                    count[switch (person.update()) { // Updates states. The function returns the person's state, so it is used to tally.
                        case NORMAL -> 0;
                        case INFECTED -> 1;
                        case IMMUNE -> 2;
                    }] ++
            );
            if (count[1] == 0) break; // If none are infected, end the simulation
            history.add(new Tick(count));
        }
        visualisation.dispose();

        if (output) try { // Writes to CSV, using Tick's custom toString() function
            FileWriter writer = new FileWriter("output.csv");
            for (Tick tick : history) {
                writer.write(tick.toString());
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void finishMovement() { // Runs after movement
        position = movement; // Copies the movement reference to position
        movement = new Person[WIDTH][HEIGHT]; // Resets movement
        if (visualisation.isVisible()) visualisation.repaint(); // Repaints the visualisation
    }

    private void forEachRemaining(Consumer<? super Person> action) { // Runs an action for each Person.
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Person p = position[x][y];
                while (p != null) {
                    Person next = p.next;
                    action.accept(p);
                    p = next;
                }
            }
        }
    }

    private void terminate() { // Terminates the program after the current tick is completed
        output = false;
        running = false;
    }
}
