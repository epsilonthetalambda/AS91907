import java.awt.Graphics;
import java.awt.image.BufferedImage;

public abstract class Image extends BufferedImage { // Used to create custom graphics in Render
    public final Graphics g; // Stores its graphics
    public final int w; // Stores its width
    public final int h; // Stores its height
    public Image(int width, int height) {
        super(width * Main.WIDTH, height * Main.HEIGHT, TYPE_INT_ARGB);
        w = width;
        h = height;
        g = getGraphics();
        render();
    }
    public abstract void render(); // Where the update logic goes
}
