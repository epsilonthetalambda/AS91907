/*
 *  AS91907.Main
 *  Last Updated: 01/08/2025
 *  Purpose: A static class that creates a window for initialising simulations.
 */

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.colorchooser.AbstractColorChooserPanel;

public class Main {
    // Labels for different kinds of states in the simulation.
    public static final char NORMAL = 0;
    public static final char INFECTED = 1;
    public static final char IMMUNE = 2;
    public static final char EMPTY = 3;

    // Labels for the different panes
    private static final char COLOURS = 0;
    private static final char PARAMETERS = 1;
    private static final char LAUNCH = 2;

    public static int sims = 0; // Number of simulations that have been launched, so each one has a different ID
    public static Color[] COLOUR = new Color[]{ // Colours of different States in the simulation
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            new Color(0xf3f3f3) // Colour of the top of an unfocused window in Windows 11
    };

    private static final Container[] panes = new Container[3]; // Separate panes for the contents of the main window to be added to
    private static final JMenuItem[] tabs = new JMenuItem[3]; // Tabs in the menu bar to switch panes and highlight errors


    public static void main(String[] args) {
        // Creates the main window for launching simulations
        JFrame window = new JFrame("Simulation Launcher");

        // Creates the menu bar for the window
        JMenuBar bar = new JMenuBar();
        window.setJMenuBar(bar);

        // Initialises the different panes
        for (int i = 0; i < panes.length; i++) {
            panes[i] = new Container(); // Initialises
            panes[i].setLayout(switch (i) { // Sets the number of rows and columns of each pane
                case COLOURS -> new GridLayout(2,2);
                case PARAMETERS -> new GridLayout(2,4);
                case LAUNCH -> new GridLayout(1,3);
                default -> throw new IllegalStateException();
            });
        }

        // Initialises the different panes
        for (char i : new char[]{COLOURS, PARAMETERS, LAUNCH}) { // Using foreach to make i semi-final, for action listener
            tabs[i] = new JMenuItem(switch (i) { // Initialises with name depending on the specific tab
                case COLOURS -> "Colours";
                case PARAMETERS -> "Parameters";
                case LAUNCH -> "Launch";
                default -> throw new IllegalStateException();
            });
            // Allows the tab to colour its background, and sets it to white
            tabs[i].setOpaque(true);
            tabs[i].setBackground(Color.WHITE);

            // Adds an action listener to change the current content pane to its corresponding pane
            tabs[i].addActionListener(l -> {
                window.setContentPane(panes[i]);
                window.revalidate();
            });

            bar.add(tabs[i]); // Adds itself to the menu bar
        }

        // Initialises each colour chooser, allowing you to change the colours of each state
        for (char i : new char[]{NORMAL, INFECTED, IMMUNE, EMPTY}) { // Using foreach to make i semi-final, for action listener
            JColorChooser c = new JColorChooser(COLOUR[i]); // Initialises with corresponding colour
            for (AbstractColorChooserPanel p : c.getChooserPanels()) p.setBackground(COLOUR[i]); // Sets all the chooser panels' backgrounds to its corresponding colour
            c.setBorder(BorderFactory.createTitledBorder(switch (i) { // Titles the colour chooser with the corresponding name
                case NORMAL -> "Normal";
                case INFECTED -> "Infected";
                case IMMUNE -> "Immune";
                case EMPTY -> "Empty";
                default -> throw new IllegalStateException();
            }));
            c.setPreviewPanel(new JPanel()); // Removes the preview panel
            c.getSelectionModel().addChangeListener(l -> { // Adds a change listener for when the colour is changed
                COLOUR[i] = c.getColor(); // Sets the corresponding colour to its value
                for (AbstractColorChooserPanel p : c.getChooserPanels()) p.setBackground(COLOUR[i]); // Updates all the backgrounds' colour
            });
            panes[COLOURS].add(c); // Adds itself to the colours pane
        }

        //noinspection rawtypes
        Panel[] panels = new Panel[10]; // Initialises all the input panels
        // Inputs in the parameters tab
        panels[0] = new IntPanel(PARAMETERS, "Width", 256, 1);
        panels[1] = new IntPanel(PARAMETERS, "Height", 256, 1);
        panels[2] = new IntPanel(PARAMETERS, "Infection Duration", 16, 1);
        panels[3] = new IntPanel(PARAMETERS, "Immunity Duration", 32, 0);
        panels[4] = new DoublePanel(PARAMETERS, "Infection Chance", 0.75, 0, 1);
        panels[5] = new IntPanel(PARAMETERS, "Normal Count", 65535, 0);
        panels[6] = new IntPanel(PARAMETERS, "Infection Count", 1, 1);
        panels[7] = new IntPanel(PARAMETERS, "Immunity Count", 0, 0);
        // Inputs in the launch tab
        panels[8] = new BlankIntPanel(LAUNCH, "Ticks", null, -1,-1);
        panels[9] = new BlankIntPanel(LAUNCH, "Tick Speed", null, 0, 0);

        JButton launch = new JButton("Launch"); // Creates a launch button
        launch.addActionListener(l -> { // Adds an action listener:
            for (JMenuItem t: tabs) t.setBackground(Color.WHITE); // Resets the tabs' colours
            Number[] values = new Number[panels.length]; // Creates a values array
            boolean valid = true; // Whether all the input was valid
            for (int i = 0; i < panels.length; i++) { // For each panel
                values[i] = panels[i].read(); // Reads its value
                if (values[i] == null) valid = false; // If the value was rejected, the input was invalid
            }
            if (valid) new Simulation( // If valid, launch a simulation
                    (int) values[0],
                    (int) values[1],
                    (int) values[2],
                    (int) values[3],
                    (double) values[4],
                    new int[]{
                            (int) values[5],
                            (int) values[6],
                            (int) values[7]
                    },
                    (int) values[8],
                    (int) values[9]
            ).start();
            else window.repaint(); // Otherwise, update window to make highlights visible
        });
        panes[LAUNCH].add(launch); // Adds the launch button to the launch tab
        window.setContentPane(panes[LAUNCH]); // Defaults the window to the launch tab
        window.setSize(1260,563); // Smallest size that fits the colour choosers
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    /*
     *  AS91907.Main.Panel
     *  Last Updated: 01/08/2025
     *  Purpose: A modified JTextField designed for outputting numerical values. Has a labelled border, and handles highlighting itself and its tab when the input is invalid.
     */
    private abstract static class Panel <T extends Number> extends JTextField {
        private static final GridLayout fill = new GridLayout(1,1); // GridLayout to ensure that the JTextField fills the frame
        private final char pane; // The index of the panel's pane
        private Panel(char pane, String title, Number content) {
            super((content == null) ? "" : String.valueOf(content)); // If input is null, start empty, otherwise start with the given value
            this.pane = pane;
            setBorder(BorderFactory.createTitledBorder(title)); // Creates the title and border
            setLayout(fill); // Makes sure the JTextField fills the panel
            panes[pane].add(this); // Adds itself to given container
        }
        abstract T get() throws NumberFormatException; // Returns the number value of the JTextField
        private T read() { // Attempts to read the JTextField, highlights and returns null if fails
            try {
                T output = get(); // Gets the output
                // If successful:
                setBackground(Color.WHITE); // Clears the background
                return output; // Returns the value
            } catch (NumberFormatException e) { // If unsuccessful:
                setBackground(Color.RED); // Highlights the panel for the user to see the issue
                tabs[pane].setBackground(Color.RED); // Highlights the tab for the user to click
                return null;
            }
        }
    }

    /*
     *  AS91907.Main.IntPanel
     *  Last Updated: 17/07/2025
     *  Purpose: Panel designed for integer values. It will reject values that are smaller than the given minimum value.
     */
    private static class IntPanel extends Panel <Integer> { // Panel designed for integers. Has a minimum
        private final int min; // Minimum value the panel will accept
        private IntPanel(char pane, String title, Integer content, int min) {
            super(pane, title, content);
            this.min = min;
        }
        @Override
        Integer get() throws NumberFormatException {
            int value = Integer.parseInt(getText()); // Gets the value, throws if invalid
            if (value < min) throw new NumberFormatException(); // If value is below the minimum, also throw
            return value; // Otherwise, return
        }
    }

    /*
     *  AS91907.Main.BlankIntPanel
     *  Last Updated: 24/07/2025
     *  Purpose: IntPanel, but with the added feature that when the input is left blank, it will return the given automatic value instead of rejecting it.
     */
    private static class BlankIntPanel extends IntPanel { // IntPanel, but returns auto value instead of null when input is blank
        private final int auto; // Default return value
        private BlankIntPanel(char pane, String title, Integer content, int min, int auto) {
            super(pane, title, content, min);
            this.auto = auto;
        }
        @Override
        Integer get() throws NumberFormatException {
            if (getText().isBlank()) return auto; // If blank, return the default value
            return super.get(); // Otherwise, try to parse
        }
    }

    /*
     *  AS91907.Main.DoublePanel
     *  Last Updated: 01/08/2025
     *  Purpose: Panel designed for double values. It will reject values that are not between the given minimum and maximum values.
     */
    private static class DoublePanel extends Panel <Double> { // Panel designed for doubles. Has a minimum and maximum
        private final double min, max; // Minimum and maximum accepted values
        private DoublePanel(char pane, String title, Double content, double min, double max) {
            super(pane, title, content);
            this.min = min;
            this.max = max;
        }
        @Override
        Double get() throws NumberFormatException {
            double value = Double.parseDouble(getText()); // Parses the value, throws if invalid
            if (value < min || max < value) throw new NumberFormatException(); // If value outside range, throw
            return value; // Otherwise, return
        }
    }
}
