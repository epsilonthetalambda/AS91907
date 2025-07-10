import java.awt.Graphics;
import java.awt.image.BufferedImage;

public abstract class Image extends BufferedImage { // Used to create custom graphics in Render
    public final Simulation s;
    public final int w, h;
    public final Graphics g; // Stores its graphics
    public Image(Simulation s, int w, int h) {
        super(w, h, TYPE_INT_ARGB);
        this.s = s; this.w = w; this.h = h;
        g = getGraphics();
        render();
    }
    public abstract void render(); // Where the update logic goes
}
