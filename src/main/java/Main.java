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
    public static void main(String[] args) {
        // Creates the main GUI, allowing for launching of simulation instances
        JFrame window = new JFrame("Customise Simulation");
        window.setLayout(new GridLayout(2, 5));
        Panel[] panels = new Panel[9];
        panels[0] = new Panel<Double>(window, "Infection Chance", 0.5) {
            @Override
            Double get() throws NumberFormatException { // Gets the double value of the text, throws if outside its range
                double value = Double.parseDouble(field.getText());
                if (0 <= value && value <= 1) return value;
                throw new NumberFormatException(); // No point adding description as it is always caught
            }
        };
        panels[1] = new IntPanel(window, "Infection Duration", 10);
        panels[2] = new IntPanel(window, "Immunity Duration", 10);
        panels[3] = new IntPanel(window, "Width", 100);
        panels[4] = new IntPanel(window, "Height", 100);
        panels[5] = new IntPanel(window, "Normal Count", 10000);
        panels[6] = new IntPanel(window, "Infection Count", 100);
        panels[7] = new IntPanel(window, "Immunity Count", 1000);
        panels[8] = new Panel<Integer>(window, "Ticks", 100000) {
            @Override
            Integer get() throws NumberFormatException { // Same as intPanel, but returns -1 if blank
                String text = field.getText();
                if (text.isBlank()) return -1;
                else return Integer.parseInt(text);
            }
        };
        JButton start = new JButton("Start"); // Start simulation
        start.addActionListener(l -> {
            for (Panel p : panels) p.setBackground(Color.WHITE);
            try { // If any parses fail, button does nothing
                new Simulation(
                        (Double ) panels[0].read(),
                        (Integer) panels[1].read(),
                        (Integer) panels[2].read(),
                        (Integer) panels[3].read(),
                        (Integer) panels[4].read(),
                        (Integer) panels[5].read(),
                        (Integer) panels[6].read(),
                        (Integer) panels[7].read(),
                        (Integer) panels[8].read()

                ).execute();
            } catch (NumberFormatException ignored) {
                System.out.println(window.getSize());
            }
        });
        window.add(start);
        window.setSize(611,125); // Title length of Panels don't affect their preferredSize, so pack can't be used and I just hardcoded it
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    private abstract static class Panel <T extends Number> extends JPanel { // Wraps a JTextField in a titled border, handles reading
        final JTextField field;
        private Panel(Container source, String title, Number content) {
            setBorder(BorderFactory.createTitledBorder(title)); // Fancy border
            setLayout(new GridLayout(1,1)); // Makes sure the JTextField fills the panel
            field = new JTextField(String.valueOf(content)); // Initialises with given default value
            add(field); // Adds the field
            source.add(this); // Adds itself to given container
        }
        abstract T get() throws NumberFormatException; // Reads the number value of the JTextField
        public T read() throws NumberFormatException {
            try {
                return get(); // Tries to return the value
            } catch (NumberFormatException e) { // If failed
                setBackground(Color.RED); // Highlights the panel for the user to see the issue
                throw e; // Throws
            }
        }
    }

    public static class IntPanel extends Panel <Integer> { // Panel designed for ints
        private IntPanel(Container source, String title, int content) {
            super(source, title, content);
        }
        @Override
        Integer get() throws NumberFormatException { // Do I need to explain this
            return Integer.parseInt(field.getText());
        }
    }
}
