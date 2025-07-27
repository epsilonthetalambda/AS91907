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
import java.util.Arrays;
import java.util.function.Consumer;

public class Simulation extends Thread {
    // Simulation parameters
    public final int ID; // Unique within this instance of the program
    public final int WIDTH; // Width of the simulation
    public final int HEIGHT; // Height of the simulation
    public final double INFECTION_CHANCE; // Chance for each infected person to infect a normal person
    public final int INFECTION_COOLDOWN; // Number of ticks since infection that a person can infect others
    public final int IMMUNITY_COOLDOWN; // Number of ticks since infection that a person cannot get reinfected
    private int TICKS; // Number of ticks remaining

    public Color[] COLOR = new Color[]{
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            new Color(243, 243, 243)
    };

    public Person[][] position; // Stores the People according to their positions
    public Person[][] movement; // People move here, sorting themselves, then position references this.

    private final JFrame main; // The main window, allowing for toggling of visualisations, and termination.
    private final Render[] renders; // Stores the toggleable renders
    private final JLabel tickCounter;
    private final JDialog colourChooser;
    private final JColorChooser[] choosers;
    private int TICK_SPEED;

    public final ArrayList<int[]> history; // Stores the total counts of each population each tick.
    public boolean running = true; // Whether to continue running as usual

    public Simulation(double infectionChance, int infectionDuration, int immunityDuration, int width, int height, int[] startingCount, int ticks, int tickSpeed) {
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
        TICK_SPEED = tickSpeed;
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

        renders = new Render[] {
                initialiseVisualisation(),
                initialisePie(Arrays.stream(startingCount).sum())
        };

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

        JMenuItem[] renderButtons = new JMenuItem[2];

        for (int i = 0; i < 2; i++) {
            renderButtons[i] = new JMenuItem(switch (i) {
                case 0 -> "Visualisaton";
                case 1 -> "Pie";
                default -> throw new IllegalStateException();
            });
            final int I = i;
            renderButtons[i].addActionListener(l -> renders[I].toggle());
            bar.add(renderButtons[i]);
        }

        tickCounter = new JLabel();
        tickCounter.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(tickCounter);
        Main.BlankIntPanel speed = new Main.BlankIntPanel(main, "Tick Speed (ms)", TICK_SPEED, 0, 0);
        speed.addActionListener(l -> {
            Integer s = speed.read();
            if (s != null) TICK_SPEED = s;
        });
        main.pack();
        main.setVisible(true);

        colourChooser = new JDialog(main);
        colourChooser.setLayout(new GridLayout(2,2));
        choosers = new JColorChooser[4];
        for (char i = 0; i < 4; i++) {
            final char I = i;
            choosers[i] = chooser(
                    switch (i) {
                        case Person.NORMAL -> "Normal";
                        case Person.INFECTED -> "Infected";
                        case Person.IMMUNE -> "Immune";
                        case Person.EMPTY -> "Empty";
                        default -> throw new IllegalStateException();
                    },
                    COLOR[i],
                    l -> {
                        COLOR[I] = choosers[I].getColor();
                        choosers[I].setBackground(COLOR[I]);
                    }
            );
            colourChooser.add(choosers[i]);
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
                        case Person.NORMAL -> 0;
                        case Person.INFECTED -> 1;
                        case Person.IMMUNE -> 2;
                        default -> throw new IllegalStateException();
                    }] ++
            );

            forEachRemaining(Person::move); // Changes positions
            finishMovement();
            if (count[1] == 0) break; // If none are infected, end the simulation
            history.add(count);
            System.out.println(System.currentTimeMillis() - prevMillis);
            while (System.currentTimeMillis() < prevMillis + TICK_SPEED) onSpinWait();
            prevMillis = System.currentTimeMillis();
        }

        done();
    }

    private void done() {
        for (Render r : renders) r.dispose();
        main.setJMenuBar(null);
        running = false; // Makes the window now terminate simulation on close
        // Creates a file chooser for saving the simulation
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("simulation" + ID + ".csv"));

        main.getContentPane().removeAll(); // Resets window
        main.setLayout(new GridLayout(1,1));
        JButton button = new JButton("Save to CSV");
        main.add(button);
        button.addActionListener(l -> {
            if (chooser.showSaveDialog(main) == JFileChooser.APPROVE_OPTION) try { // showSaveDialog creates a popup, waits for user confirmation, then returns status (save approved, cancelled, errored)
                // If user approved save, tries to write
                FileWriter writer = new FileWriter(chooser.getSelectedFile());
                for (int[] tick : history) writer.write(
                        tick[0] + "," + tick[1] + "," +tick[2] + System.lineSeparator()
                );
                writer.close();
                Desktop.getDesktop().open(chooser.getSelectedFile().getParentFile());
            } catch (IOException ignored) {}
        });
        main.revalidate();
    }

    private void finishMovement() { // Runs after movement
        position = movement; // Copies the movement reference to position
        movement = new Person[WIDTH][HEIGHT]; // Resets movement
        for (Render r : renders) if (r.isVisible()) r.repaint();
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
                                g.setColor(COLOR[renderedState(position[x][y])]);
                                g.fillRect(x * gridW, y * gridH, gridW, gridH);  // Draws the cell
                            }
                        }
                    }
                    private char renderedState(Person pointer) { // Given the start of a list, returns the state that should be rendered
                        /*  PRIORITY:
                        *   1. INFECTED
                        *   2. NORMAL
                        *   3. IMMUNE
                        *   4. EMPTY  */
                        if (pointer == null) return Person.EMPTY; // If no people, return null (EMPTY)
                        char rendered = pointer.state(); // Initialises the output value with the first element's state
                        boolean going = true;
                        for (; going && pointer != null; pointer = pointer.next) { // Loops through all Persons at that tile
                            switch (pointer.state()) {
                                case Person.INFECTED:
                                    rendered = Person.INFECTED;
                                case Person.IMMUNE:
                                    going = false;
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
                        int[] t = history.getLast();
                        start = 0;

                        for (int i = 0; i < 3; i++) fill(t[i], COLOR[i]);
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
        c.setBackground(initial);
        c.setBorder(BorderFactory.createTitledBorder(title));
        c.setPreviewPanel(new JPanel());
        c.getSelectionModel().addChangeListener(change);
        return c;
    }
}
