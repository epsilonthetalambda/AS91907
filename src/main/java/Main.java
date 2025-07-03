import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.GridLayout;

public class Main {
    public static void main(String[] args) {
        // Creates the main GUI
        JFrame window = new JFrame("Customise Simulation");
        window.setLayout(new GridLayout(1,3));
        JTextField width = new JTextField("width"); // Input width
        window.add(width);
        JTextField height = new JTextField("height"); // Input height
        window.add(height);
        JButton start = new JButton("Start"); // Start simulation
        start.addActionListener(l -> {
            try { // If any parses fail, button does nothing
                new Simulation(
                        Integer.parseInt(width.getText()),
                        Integer.parseInt(height.getText()),
                        65536,
                        16,
                        0.5,
                        8,
                        8,
                        65536
                ).execute();
            } catch (NumberFormatException ignored) {}
        });
        window.add(start);
        window.pack();
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}
