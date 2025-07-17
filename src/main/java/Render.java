import javax.swing.JFrame;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

public abstract class Render extends Container { // Displays a dialog with custom graphics using Image
    public int w, h; // Stores width and height
    public Simulation s;
    public Image image; // Stores the image
    private final JFrame window;
    public Render(Simulation s, String title, int w, int h) {
        this.s = s;
        this.w = w;
        this.h = h;
        if (needNewImage()) newImage(); // Generates a new image
        window = new JFrame(title + s.ID); // Creates a parent JFrame
        window.setContentPane(this); // Makes itself the content pane
        setPreferredSize(new Dimension(w, h)); // Sets its preferred size to minimum still visible
        window.pack();
        window.setVisible(true);
    }

    @Override
    public void repaint() {
        if (isVisible()) image.render();
        super.repaint();
    }

    @Override
    public void paint(Graphics g) {
        w = getWidth();
        h = getHeight();
        if (needNewImage()) newImage(); // Generates a new image
        g.setColor(s.EMPTY_COLOUR);
        g.fillRect(0,0,w,h);
        g.drawImage(image, (w - image.w) / 2, (h - image.h) / 2, null); // Draws the image
    }

    public abstract boolean needNewImage(); // Returns whether the image needs to be recreated

    public abstract void newImage(); // Where the image is initialised, allowing inherited abstraction
    public void toggle() { // Toggles visibility of the render
        window.setVisible(!window.isVisible());
    }
    public void dispose() { // Closes the render
        window.dispose();
    }
}
