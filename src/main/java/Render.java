import javax.swing.JFrame;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

public abstract class Render extends Container { // Displays a dialog with custom graphics using Image
    public int w, h; // Stores width and height
    public Simulation s;
    public Image image; // Stores the image
    private final JFrame window;
    public Render(Simulation s, String title, int initialW, int initialH) {
        this.s = s;
        w = initialW;
        h = initialH;
        newImage(); // Instantiates an image
        window = new JFrame(title); // Creates a parent JFrame
        window.setContentPane(this); // Makes itself the content pane
        setPreferredSize(new Dimension(w, h)); // Sets its preferred size to minimum still visible
        window.pack();
        window.setVisible(true);
    }

    @Override
    public void repaint() {
        image.render();
        super.repaint();
    }

    @Override
    public void paint(Graphics g) {
        int newW = getWidth(), newH = getHeight(); // Gets the current width and height
        if (w != newW || h != newH) { // If not the same, updates and calls for new Image
            w = newW;
            h = newH;
            newImage();
        } // Generates a new image
        g.drawImage(image, (getWidth() - image.getWidth()) / 2, (getHeight() - image.getHeight()) / 2, null); // Draws the image
    }

    public abstract void newImage(); // Where the image is initialised, allowing inherited abstraction
    public void toggle() { // Toggles visibility of the render
        window.setVisible(!window.isVisible());
    }
    public void dispose() { // Closes the render
        window.dispose();
    }
}
