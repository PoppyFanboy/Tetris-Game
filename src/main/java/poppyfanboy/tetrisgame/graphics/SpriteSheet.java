package poppyfanboy.tetrisgame.graphics;

import java.awt.image.BufferedImage;

import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.Util;

/**
 * Offers convenience methods for accessing the separate sprites from the
 * sprite sheet. For things to be more simple the whole sprite sheet is
 * tiled, and the sprites should be aligned to those tiles.
 */
public class SpriteSheet {
    private BufferedImage sheet;
    private int tilePixelWidth;

    public SpriteSheet(BufferedImage sheet, int tilePixelWidth) {
        this.sheet = sheet;
        this.tilePixelWidth = tilePixelWidth;
    }

    public BufferedImage crop(IntVector spriteCoords, int spriteWidth,
            int spriteHeight, double scale) {
        int x0 = spriteCoords.getX() * tilePixelWidth;
        int y0 = spriteCoords.getY() * tilePixelWidth;
        BufferedImage unscaled
                = sheet.getSubimage(x0, y0, spriteWidth * tilePixelWidth,
                spriteHeight * tilePixelWidth);
        return Util.scaleImage(unscaled, scale, scale, false);
    }
}
