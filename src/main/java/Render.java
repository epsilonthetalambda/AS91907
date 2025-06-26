import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

public abstract class Render extends Container { // Displays a dialog with custom graphics using Image
    public int w = 0, h = 0; // Stores width and height
    public Image image; // Stores the image
    public Render(JFrame owner) { // Creates a parent JDialog and attaches it to the main window
        JDialog window = new JDialog(owner);
        window.setContentPane(this);
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));
        window.pack();
        w = 1; h = 1;
        image = newImage();
        window.setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        if (getWidth()/Main.WIDTH != w || getWidth()/Main.WIDTH != h) {
            w = Math.max(getWidth()/Main.WIDTH, 1);
            h = Math.max(getHeight()/Main.HEIGHT, 1);
            image = newImage();
        } // Generates a new image
        super.paint(g);
        g.drawImage(image, 0, 0, null); // Draws the image
    }

    public abstract Image newImage(); // Where the image is initialised, allowing inherited abstraction

    public void render() { // Updates the image without recreating it
        System.out.println(Main.TICKS);
        image.render();
    }
}
