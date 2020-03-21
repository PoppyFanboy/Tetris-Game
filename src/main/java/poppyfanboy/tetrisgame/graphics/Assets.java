package poppyfanboy.tetrisgame.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import javax.imageio.ImageIO;

import poppyfanboy.tetrisgame.entities.BlockColor;
import poppyfanboy.tetrisgame.states.Resolution;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Util;

import static java.lang.Math.sqrt;
import static poppyfanboy.tetrisgame.util.IntVector.iVect;

/**
 * A class that loads the resources for the game with the specified resolution.
 * In case the resolution is changed, assets must be reloaded from scratch.
 */
public class Assets implements AutoCloseable {
    // resolution.blockWidth * STROKE_SCALE is the width of the shiny gem edges
    private final double STROKE_SCALE;
    // how many samples of the same block with different lighting
    // applied should be generated
    public static final int LIGHTING_SAMPLES_COUNT = 16;
    // resolution for which the blocks (gems) are rendered. eventually they
    // will be scaled down anyways, but rendering them at higher resolution
    // will give smoother scaled result because of the antialiasing
    private static final int RENDER_BLOCK_WIDTH = 128;

    private static final int SPRITE_SHEET_GRID_WIDTH = 16;
    private static final String SPRITE_SHEET_PATH = "/textures/sheet.png";

    public static final String FONT_PATH = "/fonts/Pixel-Font-8x8-PF.ttf";
    public static final String FONT_NAME = "Pixel-Font-8x8-PF";
    public static final Color FONT_COLOR = new Color(139, 134, 152);


    private final Resolution resolution;
    // maps colors to the arrays of buffered images which are basically
    // the animations of light source going around the gem
    // (from the -PI angle and up not including PI)
    private EnumMap<BlockColor, BufferedImage[]> renderedGems
            = new EnumMap<>(BlockColor.class);

    private EnumMap<SpriteSheetEntry, BufferedImage> spriteSheetEntries
            = new EnumMap<>(SpriteSheetEntry.class);

    private EnumMap<SpriteType, BufferedImage> sprites
            = new EnumMap<>(SpriteType.class);

    public Assets(Resolution resolution, int gameFieldWidth,
            int gameFieldHeight) throws IOException {
        STROKE_SCALE = 1 / (resolution.getBlockWidth() / 4.0 * 3);
        this.resolution = resolution;
        // load entries from the sprite sheet
        final SpriteSheet spriteSheet = new SpriteSheet(
                loadImage(SPRITE_SHEET_PATH), SPRITE_SHEET_GRID_WIDTH);
        for (SpriteSheetEntry spriteType : SpriteSheetEntry.values()) {
            spriteSheetEntries.put(spriteType, spriteSheet.crop(
                spriteType.coords, spriteType.width, spriteType.height,
                (double) resolution.getBlockWidth() / SPRITE_SHEET_GRID_WIDTH));
        }

        // generate gems
        double step = 2 * Math.PI / LIGHTING_SAMPLES_COUNT;
        for (BlockColor color : BlockColor.values()) {
            BufferedImage[] sprites
                = new BufferedImage[LIGHTING_SAMPLES_COUNT];
            for (int i = 0; i < LIGHTING_SAMPLES_COUNT; i++) {
                sprites[i] = generateBlock(
                    RENDER_BLOCK_WIDTH, RENDER_BLOCK_WIDTH,
                    resolution.getBlockWidth(), resolution.getBlockWidth(),
                    -Math.PI + i * step, color);
            }
            renderedGems.put(color, sprites);
        }

        try {
            GraphicsEnvironment ge
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT,
                    new File(Assets.class.getResource(FONT_PATH).getFile())));
        } catch (FontFormatException ex) {
            // use placeholder font
        }

        // generate sprites
        sprites.put(SpriteType.BACKGROUND, generateBackground());
        sprites.put(SpriteType.BRICK_WALL,
                generateBrickWall(gameFieldWidth, gameFieldHeight));
        sprites.put(SpriteType.GAME_FIELD_FRAME,
                generateGameFieldFrame(gameFieldWidth, gameFieldHeight));
        sprites.put(SpriteType.LOGO, generateLogo());
        sprites.put(SpriteType.NEXT_SHAPE_DISPLAY, generateNextShapeDisplay());
        sprites.put(SpriteType.SCORE_DISPLAY, generateScoreDisplay());
    }

    /**
     * Returns the closest colored block sprite with light rendered at
     * {@code &lq;= rotation} angle.
     */
    public BufferedImage getColoredBlockLeft(double lightAngle,
            BlockColor blockColor) {
        lightAngle = Rotation.normalizeAngle(lightAngle);
        int index = (int) (LIGHTING_SAMPLES_COUNT * (lightAngle + Math.PI)
                / (2 * Math.PI));
        return renderedGems.get(blockColor)[index % LIGHTING_SAMPLES_COUNT];
    }

    /**
     * Returns the closest colored block sprite with light rendered at
     * {@code &gq;= rotation} angle.
     */
    public BufferedImage getColoredBlockRight(double lightAngle,
            BlockColor blockColor) {
        lightAngle = Rotation.normalizeAngle(lightAngle);
        int index = (int) Math.ceil(LIGHTING_SAMPLES_COUNT
                * (lightAngle + Math.PI) / (2 * Math.PI));
        return renderedGems.get(blockColor)[index % LIGHTING_SAMPLES_COUNT];
    }

    public BufferedImage getSpriteSheetEntry(SpriteSheetEntry spriteType) {
        return spriteSheetEntries.get(spriteType);
    }

    public BufferedImage getSprite(SpriteType spriteType) {
        return sprites.get(spriteType);
    }

    @Override
    public void close() {
        // dispose the resources
    }

    private BufferedImage generateBlock(
            int renderWidth, int renderHeight, int width, int height,
            double rotationAngle, BlockColor blockColor) {
        // transparent image preset
        BufferedImage block = new BufferedImage(width, height,
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        AffineTransform transform = new AffineTransform();
        transform.scale((double) width / renderWidth,
                (double) height / renderHeight);
        g2d.setTransform(transform);


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
        g2d.setColor(blockColor.setSaturation(0.15F));
        for (int i = 0; i < verticesCount; i++) {
            // tangent of the stroke
            double k = (outerContourY[i] - innerContourY[i])
                    / (outerContourX[i] - innerContourX[i]);
            // how much should you step to the left and to the right
            double step = renderWidth * STROKE_SCALE * k / sqrt(1 + k * k);
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
        g2d.setStroke(new BasicStroke((float) (renderWidth * STROKE_SCALE)));
        g2d.setColor(blockColor.setSaturation(0.15F));
        g2d.drawPolygon(DoubleVector.getIntX(innerContour),
                DoubleVector.getIntY(innerContour), outerContour.length);

        return block;
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

    private BufferedImage generateBackground() {
        BufferedImage tile
                = spriteSheetEntries.get(SpriteSheetEntry.BACKGROUND_TILE);

        BufferedImage background = new BufferedImage(resolution.getWidth(),
            resolution.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = background.getGraphics();

        int blockWidth = resolution.getBlockWidth();
        for (int x = 0; x < resolution.getTileWidth(); x++) {
            for (int y = 0; y < resolution.getTileHeight(); y++) {
                g.drawImage(tile, x * blockWidth, y * blockWidth, null);
            }
        }
        return background;
    }

    private BufferedImage generateBrickWall(int width, int height) {
        int blockWidth = resolution.getBlockWidth();
        BufferedImage brickWall = new BufferedImage(width * blockWidth,
                height * blockWidth, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = brickWall.getGraphics();

        // upper-left corner
        g.drawImage(getSpriteSheetEntry(SpriteSheetEntry.WALL_TILE_00),
                0, 0, null);
        // upper-right corner
        g.drawImage(getSpriteSheetEntry(SpriteSheetEntry.WALL_TILE_20),
                (width - 1) * blockWidth, 0, null);
        // remaining upper blocks
        BufferedImage upper
                = getSpriteSheetEntry(SpriteSheetEntry.WALL_TILE_10);
        for (int i = 1; i < width - 1; i++) {
            g.drawImage(upper, i * blockWidth, 0, null);
        }
        // blocks on the left side
        BufferedImage left = getSpriteSheetEntry(SpriteSheetEntry.WALL_TILE_01);
        for (int i = 1; i < height; i++) {
            g.drawImage(left, 0, i * blockWidth, null);
        }
        // blocks on the right side
        BufferedImage right
                = getSpriteSheetEntry(SpriteSheetEntry.WALL_TILE_21);
        for (int i = 1; i < height; i++) {
            g.drawImage(right, (width - 1) * blockWidth, i * blockWidth, null);
        }
        // all remaining blocks (in the middle)
        BufferedImage tile
                = getSpriteSheetEntry(SpriteSheetEntry.WALL_TILE_11);
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height; y++) {
                g.drawImage(tile, x * blockWidth, y * blockWidth, null);
            }
        }
        return brickWall;
    }

    private BufferedImage generateGameFieldFrame(int width, int height) {
        int blockWidth = resolution.getBlockWidth();

        BufferedImage left = getSpriteSheetEntry(
                SpriteSheetEntry.GAME_FIELD_FRAME_TILE_LEFT);

        BufferedImage frame = Util.fillTiles(width + 2, height + 2, blockWidth,
                null, left, null);
        Graphics g = frame.getGraphics();

        // corners
        BufferedImage upperLeftCorner = getSpriteSheetEntry(
                SpriteSheetEntry.GAME_FIELD_FRAME_TILE_CORNER);
        BufferedImage upperRightCorner
                = Util.mirrorImage(upperLeftCorner, false, true);
        BufferedImage bottomRightCorner
                = Util.mirrorImage(upperLeftCorner, true, true);
        BufferedImage bottomLeftCorner
                = Util.mirrorImage(upperLeftCorner, true, false);

        g.drawImage(upperLeftCorner, 0, 0, null);
        g.drawImage(upperRightCorner, width * blockWidth, 0, null);
        g.drawImage(bottomRightCorner, width * blockWidth,
                height * blockWidth, null);
        g.drawImage(bottomLeftCorner, 0, height * blockWidth, null);

        return frame;
    }

    private BufferedImage generateLogo() {
        int blockWidth = resolution.getBlockWidth();
        BufferedImage logo = new BufferedImage(6 * blockWidth, 2 * blockWidth,
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = logo.getGraphics();

        // button
        BufferedImage leftSide
                = getSpriteSheetEntry(SpriteSheetEntry.BUTTON_SIDE);
        BufferedImage rightSide
                = Util.mirrorImage(leftSide, false, true);
        BufferedImage center
                = getSpriteSheetEntry(SpriteSheetEntry.BUTTON_CENTER);
        g.drawImage(leftSide, 0, 0, null);
        // center part
        for (int i = 1; i < 5; i++) {
            g.drawImage(center, i * blockWidth, 0, null);
        }
        g.drawImage(rightSide, 5 * blockWidth, 0, null);

        // text
        BufferedImage logoText
                = getSpriteSheetEntry(SpriteSheetEntry.LOGO);
        g.drawImage(logoText, 0, 0, null);
        return logo;
    }

    private BufferedImage generateNextShapeDisplay() {
        BufferedImage display = generateDisplay(4, 5);
        return display;
    }

    private BufferedImage generateScoreDisplay() {
        int blockWidth = resolution.getBlockWidth() / 2;
        BufferedImage display = generateDisplay(7, 4);
        Graphics2D g2d = (Graphics2D) display.getGraphics();

        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, resolution.getFontSize()));
        g2d.setColor(FONT_COLOR);
        g2d.drawString("SCORE:000000", blockWidth, 2 * blockWidth);
        g2d.drawString("LINES:000", blockWidth, 4 * blockWidth);
        g2d.drawString("LEVEL:0", blockWidth, 6 * blockWidth);
        return display;
    }

    private BufferedImage generateDisplay(int width, int height) {
        assert width >= 2 && height >= 2;

        int blockWidth = resolution.getBlockWidth();
        BufferedImage upperLeftCorner
                = getSpriteSheetEntry(SpriteSheetEntry.DISPLAY_TILE_CORNER);
        BufferedImage leftSide
                = getSpriteSheetEntry(SpriteSheetEntry.DISPLAY_TILE_SIDE);
        BufferedImage center
                = getSpriteSheetEntry(SpriteSheetEntry.DISPLAY_TILE_CENTER);

        return Util.fillTiles(width, height, blockWidth, upperLeftCorner,
                leftSide, center);
    }

    private static BufferedImage loadImage(String path) throws IOException {
        return ImageIO.read(Assets.class.getResource(path));
    }

    public enum SpriteSheetEntry {
        BACKGROUND_TILE, LOGO,

        // (row, column)
        WALL_TILE_00, WALL_TILE_10, WALL_TILE_20,
        WALL_TILE_01, WALL_TILE_11, WALL_TILE_21,

        BUTTON_SIDE, BUTTON_CENTER,

        GAME_FIELD_FRAME_TILE_CORNER, GAME_FIELD_FRAME_TILE_LEFT,

        DISPLAY_TILE_CORNER, DISPLAY_TILE_SIDE, DISPLAY_TILE_CENTER;

        private static final IntVector[] COORDS = {
            // background, logo
            iVect(0, 0), iVect(10, 0),
            // wall tile
            iVect(1, 0), iVect(2, 0), iVect(3, 0),
            iVect(1, 1), iVect(2, 1), iVect(3, 1),
            // button
            iVect(4, 0), iVect(5, 0),
            // frame
            iVect(6, 0), iVect(0, 1),
            // display
            iVect(8, 0), iVect(8, 1), iVect(9, 1)
        };

        public static final IntVector[] WIDTH_HEIGHTS = {
            // background, logo
            iVect(1, 1), iVect(6, 2),
            // wall tile
            iVect(1, 1), iVect(1, 1), iVect(1, 1),
            iVect(1, 1), iVect(1, 1), iVect(1, 1),
            // button
            iVect(1, 2), iVect(1, 2),
            // frame
            iVect(2, 2), iVect(1, 1),
            // display
            iVect(1, 1), iVect(1, 1), iVect(1, 1)
        };

        // width, height and coordinates are specified in terms of the grid
        // that separates the sprite sheet into separate tiles
        private IntVector coords;
        private int width, height;

        static {
            for (SpriteSheetEntry spriteType : values()) {
                spriteType.coords = COORDS[spriteType.ordinal()];
                spriteType.width = WIDTH_HEIGHTS[spriteType.ordinal()].getX();
                spriteType.height = WIDTH_HEIGHTS[spriteType.ordinal()].getY();
            }
        }
    }

    public enum SpriteType {
        BACKGROUND, LOGO, BRICK_WALL, GAME_FIELD_FRAME, NEXT_SHAPE_DISPLAY,
        SCORE_DISPLAY;
    }
}
