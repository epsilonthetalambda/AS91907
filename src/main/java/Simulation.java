import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Simulation extends Thread {
    // Simulation parameters
    public final int ID; // Unique within this instance of the program
    public final int WIDTH; // Width of the simulation
    public final int HEIGHT; // Height of the simulation
    public final double INFECTION_CHANCE; // Chance for each infected person to infect a normal person
    public final int INFECTION_COOLDOWN; // Number of ticks since infection that a person can infect others
    public final int IMMUNITY_COOLDOWN; // Number of ticks since infection that a person cannot get reinfected
    public int TICKS; // Number of ticks remaining

    public Color EMPTY_COLOUR = Color.BLACK;
    public Color NORMAL_COLOUR = Color.GREEN;
    public Color INFECTED_COLOUR = Color.RED;
    public Color IMMUNE_COLOUR = Color.BLUE;

    public Person[][] position; // Stores the People according to their positions
    public Person[][] movement; // People move here, sorting themselves, then position references this.

    private final JFrame main; // The main window, allowing for toggling of visualisations, and termination.
    private final Render visualisation; // Displays each cell's occupants in real time
    private final Render pie; // Displays the proportion of different states as a pie chart
    private final JLabel tickCounter;
    private final JDialog colourChooser;
    private final JColorChooser[] choosers;

    public final ArrayList<Tick> history; // Stores the total counts of each population each tick.
    private int delay = 0;

    public boolean running = true; // Whether to continue running as usual

    public Simulation(double infectionChance, int infectionDuration, int immunityDuration, int width, int height, int normalCount, int infectionCount, int immunityCount, int ticks) {
        super();
        // Initialisation of parameters
        ID = Main.sims;
        Main.sims ++;
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


        history = new ArrayList<>(Math.max(ticks, 0) + 1); // Initialises history with enough initial capacity, unless endless
        history.add(new Tick(normalCount, infectionCount, immunityCount)); // Adds the initial state

        visualisation = initialiseVisualisation();

        pie = initialisePie(normalCount + infectionCount + immunityCount);


        // Creates the main GUI for the simulation
        main = new JFrame("Simulation " + ID);
        main.setLayout(new GridLayout(3,1));
        main.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!running) main.dispose();
                running = false;
            }
        });
        JMenuBar bar = new JMenuBar();
        main.setJMenuBar(bar);

        JMenuItem visualisation = new JMenuItem("Visualisation");
        visualisation.addActionListener(l -> this.visualisation.toggle());
        bar.add(visualisation);

        JMenuItem pie = new JMenuItem("Pie Chart");
        pie.addActionListener(l -> this.pie.toggle());
        bar.add(pie);

        tickCounter = new JLabel();
        tickCounter.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(tickCounter);
        Main.IntPanel delay = new Main.IntPanel(main, "Tick Delay (ms)", 0, 0);
        delay.addActionListener(l -> {
            try {
                this.delay = delay.read();
            } catch (NumberFormatException ignored) {}
        });
        main.pack();
        main.setVisible(true);

        colourChooser = new JDialog(main);
        colourChooser.setLayout(new GridLayout(2,2));
        choosers = new JColorChooser[4];
        choosers[0] = chooser("Empty", EMPTY_COLOUR, l -> EMPTY_COLOUR = choosers[0].getColor());
        choosers[1] = chooser("Normal", NORMAL_COLOUR, l -> NORMAL_COLOUR = choosers[1].getColor());
        choosers[2] = chooser("Infected", INFECTED_COLOUR, l -> INFECTED_COLOUR = choosers[2].getColor());
        choosers[3] = chooser("Immune", IMMUNE_COLOUR, l -> IMMUNE_COLOUR = choosers[3].getColor());

        for (JColorChooser c : choosers) {
            colourChooser.add(c);
        }

        colourChooser.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        colourChooser.pack();

        JButton changeColours = new JButton("Change Colours");
        changeColours.addActionListener(l -> colourChooser.setVisible(true));
        main.add(changeColours);

        finishMovement();
    }

    @Override
    public void run() {
        final int[] count = new int[3];

        long prevMillis = System.currentTimeMillis();
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
            while (System.currentTimeMillis() < prevMillis + delay) onSpinWait();
            prevMillis = System.currentTimeMillis();
        }

        done();
    }

    private void done() {
        visualisation.dispose();
        pie.dispose();
        if (!running) main.dispose(); // If running is false here, the simulation was manually closed
        else {
            running = false; // Makes the window now terminate simulation on close
            // Creates a file chooser for saving the simulation
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("simulation" + ID + ".csv"));

            main.getContentPane().removeAll(); // Resets window
            JButton button = new JButton("Save to CSV");
            main.add(button);
            button.addActionListener(l -> {
                if (chooser.showSaveDialog(main) == JFileChooser.APPROVE_OPTION) try { // showSaveDialog creates a popup, waits for user confirmation, then returns status (save approved, cancelled, errored)
                    // If user approved save, tries to write
                    FileWriter writer = new FileWriter(chooser.getSelectedFile());
                    for (Tick tick : history) writer.write(tick.toString());
                    writer.close();
                    Desktop.getDesktop().open(chooser.getSelectedFile().getParentFile());
                } catch (IOException ignored) {}
            });
            main.revalidate();
        }
    }

    private void finishMovement() { // Runs after movement
        position = movement; // Copies the movement reference to position
        movement = new Person[WIDTH][HEIGHT]; // Resets movement
        visualisation.repaint(); // Repaints the visualisation
        pie.repaint(); // Repaints the visualisation
        if (TICKS > 0) tickCounter.setText(TICKS + " ticks left.");
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

    private Render initialiseVisualisation() { // Allows you to see the cells in real time
        return new Render(this, "Visualisation ", WIDTH, HEIGHT) { // Creates the visualisation
            private int gridW, gridH; // The size of each cell in the grid, min of 1

            @Override
            public boolean needNewImage() {
                int newW = Math.max(w / s.WIDTH, 1);
                int newH = Math.max(h / s.HEIGHT, 1);
                if (newW == gridW && newH == gridH) return false;
                gridW = newW;
                gridH = newH;
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
                                    case null -> s.EMPTY_COLOUR;
                                    case NORMAL -> s.NORMAL_COLOUR;
                                    case INFECTED -> s.INFECTED_COLOUR;
                                    case IMMUNE -> s.IMMUNE_COLOUR;
                                });
                                g.fillRect(x * gridW, y * gridH, gridW, gridH);  // Draws the cell
                            }
                        }
                    }
                };
            }
        };
    }

    private Render initialisePie(int t) { // Displays the proportion of states using a pie chart
        return new Render(this, "Pie Chart ", 120, 120) { // Creates the visualisation
            private final int total = t;
            private int minSize = 1;

            @Override
            public boolean needNewImage() {
                int newMinSize = Math.max(Math.min(w, h), 1);
                if (newMinSize != minSize) {
                    minSize = newMinSize;
                    return true;
                }
                return false;
            }

            @Override
            public void newImage() {
                image = new Image(s, minSize, minSize) { // Create a new Image
                    private double start;
                    @Override
                    public void render() {
                        Tick t = history.getLast();
                        start = 0;

                        fill(t.normal, NORMAL_COLOUR);
                        fill(t.infected, INFECTED_COLOUR);
                        fill(t.immune, IMMUNE_COLOUR);
                    }

                    private void fill(int amount, Color colour) {
                        double arc = (double) 360 * amount / total;
                        g.setColor(colour);
                        g.fillArc(0, 0, w, h, (int) Math.round(start), (int) Math.round(arc));
                        start += arc;
                    }
                };
            }
        };
    }
    private JColorChooser chooser(String title, Color initial, ChangeListener change) {
        JColorChooser c = new JColorChooser(initial);
        c.setBorder(BorderFactory.createTitledBorder(title));
        c.setPreviewPanel(new JPanel());
        c.getSelectionModel().addChangeListener(change);
        return c;
    }
}
