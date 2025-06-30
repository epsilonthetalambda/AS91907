import javax.swing.JFrame;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

public abstract class Render extends Container { // Displays a dialog with custom graphics using Image
    public int w, h; // Stores width and height
    public Image image; // Stores the image
    public Render(String title, int initialW, int initialH) { // Creates a parent JDialog and attaches it to the main window
        w = initialW;
        h = initialH;
        image = newImage();
        JFrame window = new JFrame(title);
        window.setContentPane(this);
        setPreferredSize(new Dimension(w, h));
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
        int newW = getWidth(), newH = getHeight();
        if (w != newW || h != newH) {
            w = newW;
            h = newH;
            image = newImage();
        } // Generates a new image
        g.drawImage(image, (getWidth() - image.w) / 2, (getHeight() - image.h) / 2, null); // Draws the image
    }

    public abstract Image newImage(); // Where the image is initialised, allowing inherited abstraction
}
