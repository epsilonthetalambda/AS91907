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
    public static int WIDTH = 400; // Width of the simulation
    public static int HEIGHT = 125; // Height of the simulation
    public static int PEOPLE = 900000; // Number of simulated people
    public static int INFECTED = 1; // Number of infected people
    public static double INFECTION_CHANCE = 0.65; // Chance for each infected person to infect a normal person
    public static int INFECTION_COOLDOWN = 8; // Number of ticks since infection that a person can infect others
    public static int IMMUNITY_COOLDOWN = INFECTION_COOLDOWN + 20; // Number of ticks since infection that a person cannot get reinfected
    public static int TICKS = 10000; // Number of ticks the simulation runs for. < 0 is considered endless

    public static Person[] row; // Stores horizontally moving people
    public static Person[] column; // Stores vertically moving people

    private static final Looper looper = new Looper(); // Used to iterate through each person

    public static void main(String[] args) {

        row = new Person[HEIGHT];
        column = new Person[WIDTH];

        simulation = new Render(window) {
            @Override
            public Image newImage() {
                return new Image(w, h) {
                    @Override
                    public void render() {
                        g.setColor(Color.BLACK);
                        g.drawRect(w/3, h/3, w/3, h/3);
                    }
                };
            }
        };

        for (int i = 0; i < PEOPLE; i++) { // Generates the people
            new Person(i < INFECTED);
        }

        history = new ArrayList<>(Math.max(TICKS, 0) + 1); // Initialises history with enough initial capacity, unless endless
        history.add(new Tick(PEOPLE - INFECTED, INFECTED, 0)); // Adds the initial state
        looper.reset(); // Resets the looper
        for (; TICKS != 0; TICKS--) {
            history.add(looper.go()); // Runs the looper's main functions
            window.refresh(); // Refreshes the window
        }
        for (Person p : row) {
            while (p != null) {
                System.out.print(p.pos());
                System.out.print(",");
                p = p.next;
            }
            System.out.println();
        }
        for (Person p : column) {
            while (p != null) {
                System.out.print(p.pos());
                System.out.print(",");
                p = p.next;
            }
            System.out.println();
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

    public static void sortLists() {
        boolean horizontal = true;
        do {
            for (int i = 0; i < (horizontal ? HEIGHT : WIDTH); i++) {
                Person head = (horizontal ? row : column)[i];
                int s;
                Person p = head;
                for (s = 0; p != null; s++) {
                    p = p.next;
                }
                (horizontal ? row : column)[i] = mergeSort(head, s);
            }
            horizontal = !horizontal;
        } while (!horizontal);
    }

    private static Person mergeSort(Person head, int size) {
        if (size <= 1) return head;
        int i;
        Person p1 = head, p2 = head;
        for (i = 0; i < size/2 - 1; i++) {
            p2 = p2.next;
        }
        i ++;
        p2 = p2.popNext();

        p1 = mergeSort(p1, i);
        p2 = mergeSort(p2, size - i);
        if (Person.ordered(p1, p2)) {
            head = p1;
            p1 = p1.next;
        } else {
            head = p2;
            p2 = p2.next;
        }
        Person p3 = head;
        while (p1 != null || p2 != null) {
            if (Person.ordered(p1, p2)) {
                p3.next = p1;
                p1 = p1.next;
            } else {
                p3.next = p2;
                p2 = p2.next;
            }
            p3 = p3.next;
        }
        return head;
    }
    public static Person search(Person start, int pos) {
        if (start == null) return null;
        Person end = start, middle = start;
        while (end.next != null) {
            end = end.next;
        }
        do {
            if (middle.pos() >= pos) {
                end = middle;
            } else start = middle;
            middle = middle(start, end);
        } while (middle != start);
        if (middle.pos() != pos) {
            middle = middle.next;
        }
        return middle;
    }
    private static Person middle(Person start, Person end) {
        Person fast = start, slow = start;
        boolean step = false;
        while (fast != end) {
            fast = fast.next;
            if (step) slow = slow.next;
            step = !step;
        }
        return slow;
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
