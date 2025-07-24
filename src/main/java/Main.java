import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.function.Consumer;

public class Main {
    public static int sims = 0;
    public static void main(String[] args) {
        // Creates the main GUI, allowing for launching of simulation instances
        JFrame window = new JFrame("Simulation Launcher");
        Panel[] panels = new Panel[10];
        Container[] rows = new Container[] {
                new Container(),
                new Container(),
                new JButton("Start")
        };

        panels[0] = new Panel<Double>(rows[0], "Infection Chance", 0.75) {
            @Override
            Double get() throws NumberFormatException { // Gets the double value of the text, throws if outside its range
                double value = Double.parseDouble(getText());
                if (value < 0 || 1 < value) throw new NumberFormatException(); // No point adding description as it is always caught
                return value;
            }
        };
        panels[1] = new IntPanel(rows[0], "Infection Duration", 16, 1);
        panels[2] = new IntPanel(rows[0], "Immunity Duration", 32, 0);
        panels[3] = new IntPanel(rows[0], "Width", 256, 1);
        panels[4] = new IntPanel(rows[0], "Height", 256, 1);
        panels[5] = new IntPanel(rows[1], "Normal Count", 65535, 0);
        panels[6] = new IntPanel(rows[1], "Infection Count", 1, 1);
        panels[7] = new IntPanel(rows[1], "Immunity Count", 0, 0);
        panels[8] = new BlankIntPanel(rows[1], "Ticks", 1048576, -1,-1);
        panels[9] = new BlankIntPanel(rows[1], "Tick Speed (ms)", 0, 0, 0);

        window.setLayout(new GridLayout(3, 1));
        for (Container row : rows) {
            window.add(row);
            if (row instanceof JButton) {
                ((JButton) row).addActionListener(l -> {
                    Number[] values = new Number[10];
                    boolean valid = true;
                    for (int i = 0; i < 10; i++) {
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
                            (int) values[8],
                            (int) values[9]
                    ).start();
                });
            } else row.setLayout(new GridLayout(1,5));
        }
        window.pack();
        System.out.println(window.getSize());
        System.out.println(window.getContentPane().getSize());
        window.setSize(611,168); // Title length of Panels don't affect their preferredSize, so pack can't be used and I just hardcoded it
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    private abstract static class Panel <T extends Number> extends JTextField { // Wraps a JTextField in a titled border, handles reading number values
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

    public static class IntPanel extends Panel <Integer> { // Panel designed for integers
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

    public static class BlankIntPanel extends IntPanel { // IntPanel, but returns given value instead of null when input is blank
        private final int blankValue;
        BlankIntPanel(Container source, String title, int auto, int min, int blankValue) {
            super(source, title, auto, min);
            this.blankValue = blankValue;
        }
        @Override
        Integer get() throws NumberFormatException { // Do I need to explain this
            if (getText().isBlank()) return blankValue;
            return super.get();
        }
    }
}
