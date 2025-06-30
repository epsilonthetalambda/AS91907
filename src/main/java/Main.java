import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    static final Window window = new Window(); // The window where statistics and customisation happen
    static Render simulation;
    private static ArrayList<Tick> history;

    // Simulation parameters
    public static int WIDTH = 256; // Width of the simulation
    public static int HEIGHT = 256; // Height of the simulation
    public static int PEOPLE = 65536; // Number of simulated people
    public static int INFECTED = 16; // Number of infected people
    public static double INFECTION_CHANCE = 0.5; // Chance for each infected person to infect a normal person
    public static int INFECTION_COOLDOWN = 8; // Number of ticks since infection that a person can infect others
    public static int IMMUNITY_COOLDOWN = INFECTION_COOLDOWN + 8; // Number of ticks since infection that a person cannot get reinfected
    public static int TICKS = 65536; // Number of ticks the simulation runs for. < 0 is considered endless
    public static Color EMPTY_COLOR = Color.BLACK;
    public static Color NORMAL_COLOR = Color.GREEN;
    public static Color INFECTED_COLOR = Color.RED;
    public static Color IMMUNE_COLOR = Color.BLUE;

    public static Person[][] position;
    public static Person[][] movement;

    private static final Looper looper = new Looper(); // Used to iterate through each person

    public static void main(String[] args) {
        movement = new Person[WIDTH][HEIGHT];


        for (int i = 0; i < PEOPLE; i++) { // Generates the people
            new Person(i < INFECTED);
        }

        finishMovement();



        history = new ArrayList<>(Math.max(TICKS, 0) + 1); // Initialises history with enough initial capacity, unless endless
        history.add(new Tick(PEOPLE - INFECTED, INFECTED, 0)); // Adds the initial state

        simulation = new Render("Simulation", Main.WIDTH, Main.HEIGHT) {
            private int gridW, gridH;
            @Override
            public Image newImage() {
                int newGridW = w / Main.WIDTH, newGridH = h / Main.WIDTH;
                if (gridW == newGridW && gridH == newGridH) return image;
                gridW = newGridW; gridH = newGridH;
                return new Image(gridW * Main.WIDTH, gridH * Main.HEIGHT) {
                    @Override
                    public void render() {
                        for (int x = 0; x < Main.WIDTH; x++) {
                            for (int y = 0; y < Main.HEIGHT; y++) {
                                Person.State rendered = null;
                                Person p = Main.position[x][y];
                                if (p != null) {
                                    rendered = Person.State.NORMAL;
                                    for (; p != null; p = p.next) {
                                        if (p.state() != Person.State.NORMAL) {
                                            rendered = p.state();
                                            break;
                                        }
                                    }
                                }
                                g.setColor(switch (rendered) {
                                    case null -> EMPTY_COLOR;
                                    case NORMAL -> NORMAL_COLOR;
                                    case INFECTED -> INFECTED_COLOR;
                                    case IMMUNE -> IMMUNE_COLOR;
                                });
                                g.fillRect(x * gridW, y * gridH, gridW, gridH);
                            }
                        }
                    }
                };
            }
        };

        for (; TICKS != 0; TICKS--) {
            history.add(looper.go()); // Runs the looper's main functions
            window.refresh(); // Refreshes the window
        }

        try { // Writes to CSV, using Tick's custom toString() function
            FileWriter writer = new FileWriter("output.csv");
            for (Tick tick : history) {
                writer.write(tick.toString());
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void finishMovement() {
        position = movement;
        movement = new Person[WIDTH][HEIGHT];
    }

    private static class Window extends JFrame { // The window where you can view statistics. Will probably be refactored
        private final JLabel[] statLabel = new JLabel[3];
        private Window() {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            for (int i = 0; i < 3; i++) { // Initialises the stat labels
                JLabel l = new JLabel();
                l.setHorizontalAlignment(SwingConstants.CENTER);
                statLabel[i] = l;
            }

            // Adds the labels to the window
            setLayout(new GridLayout(3,2));

            addLabel("Normal");
            add(statLabel[0]);

            addLabel("Infected");
            add(statLabel[1]);

            addLabel("Immune");
            add(statLabel[2]);
            pack();

            setVisible(true);
        }

        private void addLabel(String text) { // Adds a centred label to the window
            JLabel l = new JLabel(text);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            add(l);
        }

        private void refresh() { // Updates the labels with new values
            Tick tick = history.getLast(); // Gets the current tick
            for (int i = 0; i < 3; i++) {
                statLabel[i].setText(Integer.toString(tick.get(Person.State.values()[i]))); // Updates each label
            }
            setTitle(Integer.toString(TICKS)); // For debug purposes
        }
    }
}
