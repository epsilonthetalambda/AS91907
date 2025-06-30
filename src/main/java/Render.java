import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.*;

public abstract class Render extends Container { // Displays a dialog with custom graphics using Image
    public int w = 1, h = 1; // Stores width and height
    private Image image; // Stores the image
    public Render(JFrame owner) { // Creates a parent JDialog and attaches it to the main window
        image = newImage();
        JDialog window = new JDialog(owner);
        window.setContentPane(this);
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));
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
        int newW = Math.max(getWidth()/Main.WIDTH, 1), newH = Math.max(getHeight()/Main.HEIGHT, 1);
        if (w != newW || h != newH) {
            w = newW;
            h = newH;
            image = newImage();
        } // Generates a new image
        g.drawImage(image, 0, 0, null); // Draws the image
    }

    public abstract Image newImage(); // Where the image is initialised, allowing inherited abstraction
}
