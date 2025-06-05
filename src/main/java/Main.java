import javax.swing.*;
import java.awt.*;

public class Main {
    static final Window window = new Window(); // The window where statistics and customisation happens
    private static final Simulation simulation = new Simulation(); // The window where the simulation is rendered
    private static Graphics graphics; // The graphics of the simulation

    // Simulation parameters
    private static int WIDTH = 100;
    private static int HEIGHT = 100;
    private static int PEOPLE = 30;
    private static int INFECTED = 1;
    private static double INFECTION_CHANCE = 0.5;
    private static int INFECTION_COOLDOWN = 10;
    private static int IMMUNITY_COOLDOWN = 10;

    private static Person[] row;
    private static Person[] column;

    public static void main(String[] args) {
        window.setVisible(true);
        simulation.setVisible(true);
        graphics = simulation.getGraphics();

        row = new Person[HEIGHT];
        column = new Person[WIDTH];

        for (int i = 0; i < PEOPLE; i++) { // Generates the people
            if (Math.random() < 0.5) {
                new Person(true, HEIGHT, WIDTH, (i < INFECTED) ? 1 : 0);
            } else {
                new Person(false, WIDTH, HEIGHT, (i < INFECTED) ? 1 : 0);
            }
        }
    }

    private static class Person { // Stores a person in the simulation
        // Position
        private final boolean horizontal;
        private final int lane;
        private int pos;
        private boolean forwards;
        private int state; // Current state. == 0 -> normal, <= INFECTION_COOLDOWN -> infected, else -> immune, wraps back to 0
        private boolean infected = false;
        private final Person pointer;
        private Person(boolean horizontal, int lane, int pos, int state) {
            this.horizontal = horizontal;
            this.lane = lane;
            this.pos = pos;
            this.pointer = (horizontal ? row : column)[lane];
            (horizontal ? row : column)[lane] = this;
            forwards = (Math.random() < 0.5);
            this.state = state;
        }
        private void move() {
            if (pos == 0) {
                forwards = true;
            } else if (pos == (horizontal ? WIDTH : HEIGHT) - 1) {
                forwards = false;
            }
            if (forwards) {
                pos ++;
            } else {
                pos --;
            }
        }
        private void preInfection() {
            if (state != 0) {
                if (state <= INFECTION_COOLDOWN) infect();
                state ++;
            }
        }
        private void infect() {
            Person person = (horizontal ? row : column)[lane];
            while (person != null) {
                if (person.state == 0 && pos == person.pos && Math.random() < INFECTION_CHANCE) person.infected = true;
                person = person.pointer;
            }
            person = (horizontal ? column : row)[pos];
            while (person != null) {
                if (person.state == 0 && lane == person.pos && Math.random() < INFECTION_CHANCE) person.infected = true;
                person = person.pointer;
            }
        }
        private void postInfection() {
            if (infected) state = 1;
            else if (state > INFECTION_COOLDOWN + IMMUNITY_COOLDOWN) state = 0;
        }
    }

    private static class Window extends JFrame { // The window where you can view statistics and change
        private final JLabel[][] statLabel = new JLabel[3][2]; // Mirrors stat[][], but for display purposes
        private Window() {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            for (int i = 0; i < 6; i++) { // Initialises the stat labels
                JLabel l = new JLabel();
                l.setHorizontalAlignment(SwingConstants.CENTER);
                statLabel[i % 3][i % 2] = l;
            }

            // Adds the labels to the window

            setLayout(new GridLayout(4,3));

            addLabel("Category");
            addLabel("Change");
            addLabel("Total");

            addLabel("Normal");
            add(statLabel[0][0]);
            add(statLabel[0][1]);

            addLabel("Infected");
            add(statLabel[1][0]);
            add(statLabel[1][1]);

            addLabel("Immune");
            add(statLabel[2][0]);
            add(statLabel[2][1]);
        }

        private void addLabel(String text) { // Adds a centred label
            JLabel l = new JLabel(text);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            add(l);
        }
    }

    private static class Simulation extends JDialog { // The dialog where you can watch the simulation running
        private Simulation() {
            super(window);
        }
    }
}
