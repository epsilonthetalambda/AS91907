import java.awt.Graphics;
import java.awt.image.BufferedImage;

public abstract class Image extends BufferedImage { // Used to create custom graphics in Render
    public final Simulation s;
    public final Graphics g; // Stores its graphics
    public Image(Simulation s, int width, int height) {
        super(width, height, TYPE_INT_ARGB);
        this.s = s;
        g = getGraphics();
        render();
    }
    public abstract void render(); // Where the update logic goes
}
