import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Simulation extends Thread {
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

    public Simulation(double infectionChance, int infectionDuration, int immunityDuration, int width, int height, int normalCount, int infectionCount, int immunityCount, int ticks) {
        super();
        // Initialisation of parameters
        WIDTH = width;
        HEIGHT = height;
        INFECTION_COOLDOWN = infectionDuration;
        IMMUNITY_COOLDOWN = INFECTION_COOLDOWN + immunityDuration;
        INFECTION_CHANCE = infectionChance;
        TICKS = ticks;
        // Initialisation of cells
        position = new Person[WIDTH][HEIGHT];
        movement = new Person[WIDTH][HEIGHT];
        for (int i = 0; i < normalCount; i++) new Person(this, 0);
        for (int i = 0; i < infectionCount; i++) new Person(this, (int) (Math.random() * infectionDuration) + 1);
        for (int i = 0; i < immunityCount; i++) new Person(this, (int) (Math.random() * immunityDuration) + infectionDuration + 1);

        visualisation = new Render(this, "Simulation", WIDTH, HEIGHT) { // Creates the visualisation
            private int gridW, gridH; // The size of each cell in the grid, min of 1

            @Override
            public boolean needNewImage() {
                int diffW = gridW ^ Math.max(w / s.WIDTH, 1);
                int diffH = gridH ^ Math.max(h / s.HEIGHT, 1);
                if (diffW == 0 && diffH == 0) return false;
                gridW = gridW ^ diffW;
                gridH = gridH ^ diffH;
                return true;
            }

            @Override
            public void newImage() {
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
        };

        finishMovement();

        // Creates the main GUI for the simulation
        main = new JFrame("Simulation");
        main.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!running) main.dispose();
                running = false;
            }
        });
        // Button to toggle visibility of the visualisation
        JButton toggleVisualisation = new JButton("Visualisation");
        toggleVisualisation.addActionListener(l -> visualisation.toggle());
        main.add(toggleVisualisation);
        main.pack();
        main.setVisible(true);

        history = new ArrayList<>(Math.max(ticks, 0) + 1); // Initialises history with enough initial capacity, unless endless
        history.add(new Tick(normalCount, infectionCount, immunityCount)); // Adds the initial state
    }

    @Override
    public void run() {
        final int[] count = new int[3];

        for (; running && TICKS != 0; TICKS--) {
            forEachRemaining(Person::spread); // Spreads infections
            for (int i = 0; i < 3; i++) count[i] = 0;
            forEachRemaining(person ->
                    count[switch (person.update()) { // Updates states. The function returns the person's state, so it is used to tally.
                        case NORMAL -> 0;
                        case INFECTED -> 1;
                        case IMMUNE -> 2;
                    }] ++
            );
            forEachRemaining(Person::move); // Changes positions
            finishMovement();
            if (count[1] == 0) break; // If none are infected, end the simulation
            history.add(new Tick(count));
        }

        done();
    }

    private void done() {
        visualisation.dispose();
        if (!running) main.dispose(); // If running is false here, the simulation was manually closed
        else {
            running = false; // Makes the window now terminate simulation on close
            // Creates a file chooser for saving the simulation
            JFileChooser chooser = new JFileChooser();
            FileFilter none = chooser.getFileFilter(); // Stores the "All Files" option
            chooser.removeChoosableFileFilter(chooser.getFileFilter()); // Removes filter so it is not the default
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Comma Separated Value (.csv)", "csv")); // What it is
            chooser.addChoosableFileFilter(none); // For if you want to save it as a txt or something

            main.getContentPane().removeAll(); // Resets window
            JButton button = new JButton("Save to CSV");
            main.add(button);
            button.addActionListener(l -> {
                if (chooser.showSaveDialog(main) == JFileChooser.APPROVE_OPTION) try { // showSaveDialog creates a popup, waits for user confirmation, then returns status (save approved, cancelled, errored)
                    // If user approved save, tries to write
                    FileWriter writer = new FileWriter(chooser.getSelectedFile());
                    for (Tick tick : history) writer.write(tick.toString());
                    writer.close();
                } catch (IOException ignored) {}
            });
            main.revalidate();
        }
    }

    private void finishMovement() { // Runs after movement
        position = movement; // Copies the movement reference to position
        movement = new Person[WIDTH][HEIGHT]; // Resets movement
        if (visualisation.isVisible()) visualisation.repaint(); // Repaints the visualisation
    }

    private void forEachRemaining(Consumer<? super Person> action) { // Runs an action for each Person.
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Person p = position[x][y]; // Gets the start of each list
                while (p != null) { // Loops through the list and does the action on each
                    Person next = p.next;
                    action.accept(p);
                    p = next;
                }
            }
        }
    }

    public record Tick(int normal, int infected, int immune) { // Stores the totals of one simulation tick
        public Tick(int[] count) { // Turns an array into a Tick
            this(count[0], count[1], count[2]);
        }
        @Override
        public String toString() { // Outputs a CSV line corresponding to the tick's totals
            return normal + "," + infected + ',' + immune + System.lineSeparator();
        }
    }
}
