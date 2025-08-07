/*
 *  AS91907.Simulation
 *  Last Updated: 02/08/2025
 *  Purpose: Runs a simulation with the given parameters on a new thread. Allows viewing of a visualisation and pie chart, and facilitates writing simulation details to a CSV file when the simulation ends.
 */

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.*;

public class Simulation extends Thread {
    // Labels for the different renders in the simulation
    private static final char VISUALISATION = 0;
    private static final char PIE = 1;

    // Simulation parameters
    public final int ID; // Unique within this instance of the program
    public final int WIDTH, HEIGHT; // Size of the simulation
    public final double INFECTION_CHANCE; // Chance for each infected person to infect a normal person
    public final int INFECTION_COOLDOWN, IMMUNITY_COOLDOWN; // Highest state (internal) for each respective stage
    private final int TICK_SPEED; // Minimum number of milliseconds per tick (default 0)
    private int ticks; // Number of ticks remaining

    public Person[][] position; // Stores the People according to their positions
    public Person[][] movement; // People move here, sorting themselves, then gets reassigned to position.

    private final JFrame main; // The main window, allowing for toggling of visualisations, and termination.
    private final Render[] renders; // Stores the toggleable renders
    private final JLabel tickCounter;

    public int infections; // Number of infections during simulation

    private final ArrayList<int[]> history; // Stores the total counts of each population each tick.
    private boolean running = true; // Whether to continue running as usual

    public Simulation(int width, int height, int infectionDuration, int immunityDuration, double infectionChance, int[] startingCount, int ticks, int tickSpeed) {
        super();
        // Initialisation of parameters
        ID = Main.sims;
        Main.sims ++;
        WIDTH = width;
        HEIGHT = height;
        INFECTION_COOLDOWN = infectionDuration;
        IMMUNITY_COOLDOWN = INFECTION_COOLDOWN + immunityDuration;
        INFECTION_CHANCE = infectionChance;
        this.ticks = ticks;
        TICK_SPEED = tickSpeed;
        infections = startingCount[Main.INFECTED];
        // Initialisation of cells
        position = new Person[WIDTH][HEIGHT];
        movement = new Person[WIDTH][HEIGHT];
        for (char i = 0; i < 3; i++) {
            for (int j = 0; j < startingCount[i]; j++) {
                new Person(this, i);
            }
        }

        history = new ArrayList<>(Math.max(ticks, 0) + 1); // Initialises history with enough initial capacity, unless endless
        history.add(startingCount); // Adds the initial state

        // Counts the total amount of Persons in the simulation
        int total = 0;
        for (int i : startingCount) total += i;
        // Creates the Renders, storing them in an array
        renders = new Render[] {
                initialiseVisualisation(),
                initialisePie(total)
        };

        // Creates the main GUI for the simulation
        main = new JFrame("Simulation " + ID);
        main.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { // On close button pressed:
                if (running) running = false; // If the simulation is running, stop it early
                else main.dispose(); // If the simulation is not running, close the simulation.
            }
        });
        // Creates a JMenuBar
        JMenuBar bar = new JMenuBar();
        main.setJMenuBar(bar);

        // Creates menu items to toggle visibility of the renders
        for (char i : new char[]{VISUALISATION, PIE}) { // Using foreach to make i semi-final, for action listener
            JMenuItem renderButton = new JMenuItem(switch (i) {
                case VISUALISATION -> "Visualisation";
                case PIE -> "Pie";
                default -> throw new IllegalStateException();
            });
            renderButton.setAccelerator(KeyStroke.getKeyStroke((char) ('1' + i), Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
            renderButton.addActionListener(l -> renders[i].toggle());
            bar.add(renderButton);
        }

        // Creates a label to display the number of ticks remaining.
        tickCounter = new JLabel("Running endlessly"); // This text will be written over, unless it is running endlessly
        tickCounter.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(tickCounter);

        main.pack();
        main.setVisible(true);

        finishMovement(); // Gets everything prepared for the simulation to start
    }

    @Override
    public void run() { // What runs the simulation
        long prevMillis; // Stores the time of the previous tick
        for (; running && ticks != 0; ticks--) { // While we are still running and there are still more ticks left
            prevMillis = System.currentTimeMillis(); // Stores the time when the tick started
            forEachRemaining(Person::spread); // Spreads infections for each Person
            forEachRemaining(Person::move); // Changes positions of each Person
            finishMovement(); // Resets position and movement

            // Counts the total quantity of each State, storing it in history
            final int[] count = new int[3]; // Initialises the count
            forEachRemaining(person -> // This runs for each Person
                    // Runs the Person's update function. Increments the int in count with the corresponding index to update's return value, which is the Person's current state.
                    count[switch (person.update()) {
                        case Main.NORMAL -> 0;
                        case Main.INFECTED -> 1;
                        case Main.IMMUNE -> 2;
                        default -> throw new IllegalStateException();
                    }] ++
            );
            history.add(count); // Add the new item to the history
            if (count[1] == 0) break; // If none are infected, end the simulation
            updateVisuals(); // Update the renders and counter
            while (System.currentTimeMillis() < prevMillis + TICK_SPEED) onSpinWait(); // Pauses the thread while it hasn't been enough time to start the next tick
        }

        done(); // Finishes the simulation
    }

    private void done() { // Handles the end of the simulation, removing the renders and allowing for saving to CSV
        for (Render r : renders) r.dispose(); // Removes all renders
        main.setJMenuBar(null); // Removes the menu bar
        main.getContentPane().removeAll(); // Removes the tick counter
        running = false; // Makes the window now terminate simulation on close
        // Creates a file chooser for saving the simulation
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("simulation" + ID + ".csv")); // Path defaults to simulation[ID].csv in the default directory
        // Creates a button to save to CSV
        JButton button = new JButton("Save to CSV");
        button.addActionListener(l -> { // On button press
            if (chooser.showSaveDialog(main) == JFileChooser.APPROVE_OPTION) try { // showSaveDialog creates a popup, waits for user confirmation, then returns status (CANCEL, APPROVE, ERROR)
                // If user approved save, tries to write
                FileWriter writer = new FileWriter(chooser.getSelectedFile()); // Gets the selected file
                // Writes the titles of each column
                writer.write("Current Normal,Current Infected,Current Immune,Normal Change,Infected Change,Immune Change,Total Infected" + System.lineSeparator());
                // Writes the first row (starting values, no changes, total infected)
                int[] tick = history.getFirst();
                writer.write(
                        tick[0] + "," + tick[1] + "," +tick[2] +
                        ",0,0,0," +
                                infections +
                        System.lineSeparator());
                // Writes each remaining tick
                for (int i = 1; i < history.size(); i++) { // For each other tick in the history
                    int[] nextTick = history.get(i); // Gets the tick
                    writer.write( // Writes the current and changed values
                            nextTick[0] + "," + nextTick[1] + "," + nextTick[2] + "," +
                            (nextTick[0] - tick[0]) + "," + (nextTick[1] - tick[1]) + "," + (nextTick[2] - tick[2]) +
                            System.lineSeparator()
                    );
                    tick = nextTick; // Reassigns tick, for the next loop
                }
                writer.close(); // Closes the writer
                Desktop.getDesktop().open(chooser.getSelectedFile().getParentFile()); // Opens the directory in the system's file manager
            } catch (IOException ignored) {} // Do nothing if write failed
        });
        main.add(button);
        button.requestFocusInWindow();
        main.revalidate();
    }

    private void finishMovement() { // Runs after movement, resetting the position and movement arrays
        position = movement; // Copies the movement reference to position
        movement = new Person[WIDTH][HEIGHT]; // Resets movement
    }
    private void updateVisuals() { // Runs after updates, updating visuals of the renders and the tick counter
        for (Render r : renders) if (r.isVisible()) r.repaint(); // Repaints each visible render
        if (ticks > 0) tickCounter.setText(ticks + " ticks left."); // If simulation has a maximum tick count, update the tick counter
    }
    private void forEachRemaining(Consumer<Person> action) { // Runs an action for each Person. Borrowed from java.util.Iterator.
        // For each list in position:
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

    private Render initialiseVisualisation() { // Allows you to see the cells in real time
        return new Render(this, "Visualisation ", WIDTH, HEIGHT) { // Creates the visualisation
            private int gridW, gridH; // The size of each cell in the grid, min of 1

            @Override
            public boolean needNewImage() { // For the visualisation, the size of each cell is floored to an integer value. This means that the size of the total image will be the size of the render, floor divided by the simulation's size.
                // Calculates what the size of each cell should be right now
                int newW = Math.max(w / s.WIDTH, 1);
                int newH = Math.max(h / s.HEIGHT, 1);
                if (newW == gridW && newH == gridH) return false; // If the size is the same, do nothing
                // Otherwise, update the grid sizes, and return true
                gridW = newW;
                gridH = newH;
                return true;
            }

            @Override
            public void newImage() {
                image = new Image(gridW * s.WIDTH, gridH * s.HEIGHT) { // Create a new Image
                    @Override
                    public void render() {
                        // For each cell
                        for (int x = 0; x < s.WIDTH; x++) {
                            for (int y = 0; y < s.HEIGHT; y++) {
                                g.setColor(Main.COLOUR[renderedState(position[x][y])]); // Sets the colour to what should be rendered, based on the composition of Persons in the cell
                                g.fillRect(x * gridW, y * gridH, gridW, gridH);  // Draws the cell
                            }
                        }
                    }
                    private char renderedState(Person pointer) { // Given the start of a list, returns the state that should be rendered
                        // PRIORITY OF RENDERING:
                        // 1. INFECTED
                        // 2. NORMAL
                        // 3. IMMUNE
                        // 4. EMPTY
                        if (pointer == null) return Main.EMPTY; // If no people, return EMPTY

                        // Logic:
                        // The list is sorted by state, in the order NORMAL, INFECTED, IMMUNE.
                        // We start with the output being the first state, and if it is not NORMAL, the switch will make us return it.
                        // This works because if the first Person is INFECTED, it's the highest priority so we return it, and if the first Person is IMMUNE, then there are no NORMAL or INFECTED people, so we should return it.
                        // If the first person is NORMAL, we loop through the list, and if we hit a Person who isn't NORMAL, we run the same code.
                        // The difference is that in this case, rendered == NORMAL, so while the INFECTED case will still return INFECTED, an IMMUNE case will return NORMAL as desired.
                        // If no INFECTED or IMMUNE Persons are found, we will reach the end of the loop and return NORMAL.

                        char rendered = pointer.state(); // Initialises the output value with the first element's state
                        boolean going = true; // Whether we should still be looking
                        for (; going && pointer != null; pointer = pointer.next) { // Loops through all Persons at that tile
                            switch (pointer.state()) {
                                case Main.INFECTED: // If INFECTED
                                    rendered = Main.INFECTED; // Render an INFECTED cell
                                case Main.IMMUNE: // If IMMUNE
                                    going = false; // End the loop
                            }
                        }
                        return rendered;
                    }
                };
            }
        };
    }

    private Render initialisePie(int t) { // Displays the proportion of states using a pie chart
        return new Render(this, "Pie Chart ", 120, 120) { // Creates the visualisation
            private final int total = t; // Stores the total amount of Persons
            private int minSize = 120; // Stores the current minimum between the width and height, which determines the width and height of the image

            @Override
            public boolean needNewImage() {
                int newMinSize = Math.max(Math.min(w, h), 1); // Gets the new minimum size
                if (newMinSize == minSize) return false; // If the minimum size is correct, do nothing
                // Otherwise, update the minimum size, and return true
                minSize = newMinSize;
                return true;
            }

            @Override
            public void newImage() {
                image = new Image(minSize, minSize) { // Create a new Image
                    private double start; // The current position of the arc
                    @Override
                    public void render() {
                        int[] t = history.getLast(); // Gets the current proportions of states
                        start = 0; // Resets the start

                        for (int i = 0; i < t.length; i++) fill(t[i], Main.COLOUR[i]); // Fills an arc for each state
                    }

                    private void fill(int amount, Color colour) {
                        double arc = (double) 360 * amount / total; // Gets the arc length
                        g.setColor(colour); // Sets the colour
                        g.fillArc(0, 0, w, h, (int) Math.round(start), (int) Math.round(arc)); // Fills an arc at the position and size corresponding to the closest degree to our start and arc
                        start += arc; // Increments the starting position for the next state
                    }
                };
            }
        };
    }
}
