import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;

public class Main {
    public static int sims = 0;
    public static void main(String[] args) {
        // Creates the main GUI, allowing for launching of simulation instances
        JFrame window = new JFrame("Simulation Launcher");
        window.setLayout(new GridLayout(2, 5));
        Panel[] panels = new Panel[9];
        panels[0] = new Panel<Double>(window, "Infection Chance", 0.9) {
            @Override
            Double get() throws NumberFormatException { // Gets the double value of the text, throws if outside its range
                double value = Double.parseDouble(getText());
                if (value < 0 || 1 < value) throw new NumberFormatException(); // No point adding description as it is always caught
                return value;
            }
        };
        panels[1] = new IntPanel(window, "Infection Duration", 10, 1);
        panels[2] = new IntPanel(window, "Immunity Duration", 10, 0);
        panels[3] = new IntPanel(window, "Width", 100, 1);
        panels[4] = new IntPanel(window, "Height", 100, 1);
        panels[5] = new IntPanel(window, "Normal Count", 10000, 0);
        panels[6] = new IntPanel(window, "Infection Count", 100, 1);
        panels[7] = new IntPanel(window, "Immunity Count", 1000, 0);
        panels[8] = new Panel<Integer>(window, "Ticks", 100000) {
            @Override
            Integer get() throws NumberFormatException { // Same as intPanel, but returns -1 if blank
                String text = getText();
                if (text.isBlank()) return -1;
                int value = Integer.parseInt(text);
                if (value <= 0) throw new NumberFormatException();
                return value;
            }
        };
        JButton start = new JButton("Start"); // Start simulation
        start.addActionListener(l -> {
            Number[] values = new Number[9];
            boolean valid = true;
            for (int i = 0; i < 9; i++) {
                values[i] = panels[i].read();
                if (values[i] == null) valid = false;
            }
            if (valid) new Simulation(
                    (double) values[0],
                    (int) values[1],
                    (int) values[2],
                    (int) values[3],
                    (int) values[4],
                    (int) values[5],
                    (int) values[6],
                    (int) values[7],
                    (int) values[8]
            ).start();
        });
        window.add(start);
        window.setSize(611,125); // Title length of Panels don't affect their preferredSize, so pack can't be used and I just hardcoded it
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    private abstract static class Panel <T extends Number> extends JTextField { // Wraps a JTextField in a titled border, handles reading
        private Panel(Container source, String title, Number content) {
            super(String.valueOf(content));
            setBorder(BorderFactory.createTitledBorder(title)); // Fancy border
            setLayout(new GridLayout(1,1)); // Makes sure the JTextField fills the panel
            source.add(this); // Adds itself to given container
        }
        abstract T get() throws NumberFormatException; // Reads the number value of the JTextField
        public T read() {
            try {
                T output = get();
                setBackground(Color.WHITE);
                return output; // Tries to return the value
            } catch (NumberFormatException e) { // If failed
                setBackground(Color.RED); // Highlights the panel for the user to see the issue
                return null;
            }
        }
    }

    public static class IntPanel extends Panel <Integer> { // Panel designed for ints
        private final int min;
        IntPanel(Container source, String title, int auto, int min) {
            super(source, title, auto);
            this.min = min;
        }
        @Override
        Integer get() throws NumberFormatException { // Do I need to explain this
            int value = Integer.parseInt(getText());
            if (value < min) throw new NumberFormatException();
            return value;
        }
    }
}
