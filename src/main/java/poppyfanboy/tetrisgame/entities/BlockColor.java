package poppyfanboy.tetrisgame.entities;

import java.awt.Color;
import static java.awt.Color.decode;

/**
 * Color of a solid block of the shape. It might be the case that
 * colors of the separate blocks matter in terms of the game mechanics,
 * so I've limited the set of possible colors for the blocks.
 *
 * Also doing so allows you to pre-render all the blocks and animations
 * for them since you don't have to do it for every single RGB color.
 *
 * The corresponding RGB colors are also provided, though it is not
 * necessary to use exactly these values.
 */
public enum BlockColor {
    RED, ORANGE, YELLOW, GREEN, BLUE, DARK_BLUE, PURPLE, PINK;

    /**
     * Get the actual color (RGB).
     */
    public Color getColor() {
        switch (this) {
            case RED:
                return decode("#ff1e1e");
            case ORANGE:
                return decode("#ff831e");
            case YELLOW:
                return decode("#caff1e");
            case GREEN:
                return decode("#1eff6c");
            case BLUE:
                return decode("#1e90ff");
            case DARK_BLUE:
                return decode("#1e24ff");
            case PURPLE:
                return decode("#b21eff");
            case PINK:
                return decode("#ff1ec6");
            default:
                return decode("#000000");
        }
    }

    /**
     * Returns this color but with the changed saturation.
     */
    public Color setSaturation(float saturation) {
        Color baseColor = getColor();
        float[] hsbValues = new float[3];
        Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(),
                baseColor.getBlue(), hsbValues);
        return new Color(
            Color.HSBtoRGB(hsbValues[0], saturation, hsbValues[2]));
    }
}
