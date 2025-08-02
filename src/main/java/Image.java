/*
 *  AS91907.Image
 *  Last Updated: 10/07/2025
 *  Purpose: An Image that stores a custom drawing function, for use in Renders.
 */

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public abstract class Image extends BufferedImage { // Used to create custom graphics in Render
    public final int w, h; // Width and height of the image
    public final Graphics g; // Stores its graphics as a final reference
    public Image(int w, int h) {
        super(w, h, TYPE_INT_ARGB); // Initialises the buffered image
        this.w = w; this.h = h;
        g = getGraphics();
        render();
    }
    public abstract void render(); // Where the update logic goes
}
