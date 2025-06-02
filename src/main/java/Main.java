import javax.swing.*;
import java.awt.*;

public class Main {
    static final Window window = new Window(); // The window where statistics and customisation happens
    private static final Simulation simulation = new Simulation(); // The window where the simulation is rendered
    private static Graphics graphics; // The graphics of the simulation
    private static final int[][] stat = new int[3][2]; // The current stats. Rows of NORMAL, INFECTED, IMMUNE, columns of CHANGE, TOTAL

    // Simulation parameters
    private static int WIDTH = 100;
    private static int HEIGHT = 100;
    private static int PEOPLE = 30;
    private static int INFECTED = 1;
    private static int INFECTION_COOLDOWN = 10;
    private static int IMMUNITY_COOLDOWN = 10;

    public static void main(String[] args) {
        window.setVisible(true);
        simulation.setVisible(true);
        graphics = simulation.getGraphics();

        for (int i = 0; i < PEOPLE; i++) { // Generates the people
            new Person(WIDTH,HEIGHT,(i < INFECTED));
        }
        window.update();
    }

    private static class Person { // Stores a person in the simulation
        // Position
        private int x;
        private int y;

        private int state = 0; // Current state. Corresponds to stat[][], defaults to NORMAL
        private int cooldown = 0; // How many ticks until the state is updated

        private Person(int x, int y, boolean infected) {
            this.x = x;
            this.y = y;
            stat[0][0] ++;
            if (infected) {
                changeState();
            }
        }
        private void update() {
            if (state != 0) {
                cooldown --;
                if (cooldown == 0) {
                    changeState();
                }
            }
        }
        // Called whenever the state needs to be updated
        private void changeState() {
            stat[state][0] --; // The current state lost one member
            state = (state + 1) % 3; // Shifts the state
            stat[state][0] ++; // The new state gained one member
            if (state == 1) { // Sets the cooldown for infected
                cooldown = INFECTION_COOLDOWN;
            } else if (state == 2) { // Sets the cooldown for immune
                cooldown = IMMUNITY_COOLDOWN;
            }
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

        private void update() { // Updates stat[][] and statLabel[][]
            for (int i = 0; i < statLabel.length; i++) {
                statLabel[i][0].setText(Integer.toString(stat[i][0]));
                stat[i][1] += stat[i][0];
                statLabel[i][1].setText(Integer.toString(stat[i][1]));
                stat[i][0] = 0;
            }
        }
    }

    private static class Simulation extends JDialog { // The dialog where you can watch the simulation running
        private Simulation() {
            super(window);
        }
    }
}
