package poppyfanboy.tetrisgame.graphics;

import java.awt.image.BufferedImage;

/**
 * Offers convenience methods for accessing the separate sprites
 * from the sprite sheet.
 */
public class SpriteSheet {
    private BufferedImage sheet;

    public SpriteSheet(BufferedImage sheet) {
        this.sheet = sheet;
    }

    public BufferedImage crop(int x, int y, int width, int height) {
        return sheet.getSubimage(x, y, width, height);
    }

    public BufferedImage gridCrop(int col, int row, int width,
            int height) {
        return crop(col * width, row * height, width, height);
    }
}
