package poppyfanboy.tetrisgame.graphics;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import javax.imageio.ImageIO;

import poppyfanboy.tetrisgame.entities.BlockColor;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Rotation;

import static java.lang.Math.sqrt;

/**
 * A class that loads the resources.
 */
public class Assets implements AutoCloseable {
    private static final double strokeScale = 1 / 32.0;

    private int renderBlockWidth, outputBlockWidth;
    // how many samples of the same block with different lighting
    // applied should be generated
    private int lightingSamplesCount = 16;
    // maps colors to the arrays of buffered images which are basically
    // the animations of light source going around the gem
    // (from the -PI angle and up not including PI)
    private EnumMap<BlockColor, BufferedImage[]> renderedGems
            = new EnumMap<>(BlockColor.class);

    private final BufferedImage wallBlock;

    public Assets(int renderBlockWidth, int outputBlockWidth,
            int lightingSamplesCount) throws IOException {
        final SpriteSheet spriteSheet
                = new SpriteSheet(loadImage("/textures/sheet.png"));
        wallBlock = spriteSheet.gridCrop(0, 0, 16, 16);

        this.renderBlockWidth = renderBlockWidth;
        this.lightingSamplesCount = lightingSamplesCount;

        double step = 2 * Math.PI / lightingSamplesCount;
        for (BlockColor color : BlockColor.values()) {
            BufferedImage[] sprites
                = new BufferedImage[lightingSamplesCount];
            for (int i = 0; i < lightingSamplesCount; i++) {
                sprites[i] = generateBlock(
                    renderBlockWidth, renderBlockWidth,
                    outputBlockWidth, outputBlockWidth,
                    -Math.PI + i * step, color);
            }
            renderedGems.put(color, sprites);
        }
    }

    /**
     * Returns the closest colored block sprite with light rendered at
     * {@code &lq;= rotation} angle.
     */
    public BufferedImage getColoredBlockLeft(double lightAngle,
            BlockColor blockColor) {
        lightAngle = Rotation.normalizeAngle(lightAngle);
        int index = (int) (lightingSamplesCount * (lightAngle + Math.PI)
                / (2 * Math.PI));
        return renderedGems.get(blockColor)[index % lightingSamplesCount];
    }

    /**
     * Returns the closest colored block sprite with light rendered at
     * {@code &gq;= rotation} angle.
     */
    public BufferedImage getColoredBlockRight(double lightAngle,
            BlockColor blockColor) {
        lightAngle = Rotation.normalizeAngle(lightAngle);
        int index = (int) Math.ceil(lightingSamplesCount
                * (lightAngle + Math.PI) / (2 * Math.PI));
        return renderedGems.get(blockColor)[index % lightingSamplesCount];
    }

    /**
     * Returns how many identical sprites are rendered with lighting
     * applied at different angles.
     */
    public int getLightingSamplesCount() {
        return lightingSamplesCount;
    }

    public BufferedImage getWallBlock() {
        return wallBlock;
    }

    @Override
    public void close() {
        // dispose the resources
    }

    private static BufferedImage generateBlock(
            int renderWidth, int renderHeight, int width, int height,
            double rotationAngle, BlockColor blockColor) {
        // transparent image preset
        BufferedImage block = new BufferedImage(renderWidth, renderHeight,
                BufferedImage.TYPE_INT_ARGB_PRE);

        DoubleVector[] normalizedContour = {
                DoubleVector.dVect(0, 0.25), DoubleVector.dVect(0.25, 0),
                DoubleVector.dVect(0.75, 0), DoubleVector.dVect(1, 0.25),
                DoubleVector.dVect(1, 0.75), DoubleVector.dVect(0.75, 1),
                DoubleVector.dVect(0.25, 1), DoubleVector.dVect(0, 0.75)
        };
        DoubleVector[] outerContour = getPolygon(renderWidth, renderHeight,
                normalizedContour, 1.0, renderWidth / 16.0);
        DoubleVector[] innerContour = getPolygon(renderWidth, renderHeight,
                normalizedContour, 0.625, renderWidth / 16.0);

        final int verticesCount = outerContour.length;
        double[] innerContourX = DoubleVector.getX(innerContour);
        double[] innerContourY = DoubleVector.getY(innerContour);
        double[] outerContourX = DoubleVector.getX(outerContour);
        double[] outerContourY = DoubleVector.getY(outerContour);

        // base polygon
        Graphics2D g2d = block.createGraphics();
        g2d.setColor(blockColor.getColor());
        g2d.fillPolygon(DoubleVector.getIntX(outerContour),
                DoubleVector.getIntY(outerContour), outerContour.length);

        DoubleVector vectorToLight
                = new DoubleVector(6, 4).normalize();
        // side faces
        for (int i = 0; i < verticesCount; i++) {
            DoubleVector sideNormal = new DoubleVector(
                outerContourX[(i + 1) % verticesCount] - outerContourX[i],
                outerContourY[(i + 1) % verticesCount] - outerContourY[i])
                .normalize().rotate(rotationAngle);

            float lightness = 1 - (float) Math.max(0,
                    DoubleVector.dotProduct(vectorToLight, sideNormal) * 0.9);
            g2d.setColor(blockColor.setSaturation(lightness));

            g2d.fillPolygon(
                    new int[] {
                        (int) innerContourX[i],
                        (int) outerContourX[i],
                        (int) outerContourX[(i + 1) % verticesCount],
                        (int) innerContourX[(i + 1) % verticesCount]
                    },
                    new int[] {
                        (int) innerContourY[i],
                        (int) outerContourY[i],
                        (int) outerContourY[(i + 1) % verticesCount],
                        (int) innerContourY[(i + 1) % verticesCount]
                    }, 4);
        }

        // side strokes
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER));
        g2d.setColor(blockColor.setSaturation(0.01F));
        for (int i = 0; i < verticesCount; i++) {
            // tangent of the stroke
            double k = (outerContourY[i] - innerContourY[i])
                    / (outerContourX[i] - innerContourX[i]);
            // how much should you step to the left and to the right
            double step = renderWidth * strokeScale * k / sqrt(1 + k * k);
            g2d.fillPolygon(
                    new int[] {
                        (int) (innerContourX[i] - step),
                        (int) outerContourX[i],
                        (int) (innerContourX[i] + step)
                    },
                    new int[] {
                        (int) (innerContourY[i] - step * (-1 / k)),
                        (int) outerContourY[i],
                        (int) (innerContourY[i] + step * (-1 / k))
                    }, 3);
        }

        // inner polygon
        g2d.setColor(blockColor.getColor());
        g2d.setStroke(new BasicStroke(1));
        g2d.fillPolygon(DoubleVector.getIntX(innerContour),
                DoubleVector.getIntY(innerContour), innerContour.length);

        // inner stroke
        g2d.setStroke(new BasicStroke((float) (renderWidth * strokeScale)));
        g2d.setColor(blockColor.setSaturation(0.01F));
        g2d.drawPolygon(DoubleVector.getIntX(innerContour),
                DoubleVector.getIntY(innerContour), outerContour.length);

        BufferedImage resizedBlock = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D resizedBlockGraphics
                = (Graphics2D) resizedBlock.getGraphics();
        resizedBlockGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        resizedBlockGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        resizedBlockGraphics
                .drawImage(block, 0, 0, width, height, null);
        return resizedBlock;
    }

    /**
     * Creates a polygon shaped like a gem within the box
     * (0, 0, width, height).
     * @param   shape normalized (that is they are 0..1) coordinates of
     *          the polygon.
     * @param   scale shrinks the gem so that it still remains in the
     *          center of the box. (Valid values are from 0 to 1.)
     * @param   margin how much additional space should be between the
     *          gems if they were put side-to-side.
     */
    private static DoubleVector[] getPolygon(int width, int height,
            DoubleVector[] shape, double scale, double margin) {
        assert scale >= 0 && scale <= 1;

        DoubleVector[] polygon = new DoubleVector[shape.length];
        for (int i = 0; i < shape.length; i++) {
            final double shortenedWidth = width - margin;
            final double shortenedHeight = height - margin;

            double x
                = shape[i].getX() * shortenedWidth * scale
                + (1 - scale) * shortenedWidth / 2
                + margin / 2;
            double y
                = shape[i].getY() * shortenedHeight * scale
                + (1 - scale) * shortenedHeight / 2
                + margin / 2;
            polygon[i] = new DoubleVector(x, y);
        }
        return polygon;
    }

    private static BufferedImage loadImage(String path)
            throws IOException {
        return ImageIO.read(Assets.class.getResource(path));
    }
}
