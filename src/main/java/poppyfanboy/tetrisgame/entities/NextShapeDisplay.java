package poppyfanboy.tetrisgame.entities;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import java.util.Random;
import java.util.function.Function;

import poppyfanboy.tetrisgame.entities.shapetypes.ShapeType;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.graphics.displayanimation.TransitionAnimation;
import poppyfanboy.tetrisgame.graphics.displayanimation.AnimatedDisplay;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Transform;

public class NextShapeDisplay extends Entity implements AnimatedDisplay {
    public static final int DEFAULT_WIDTH = 4, DEFAULT_HEIGHT = 5;

    private final GameState gameState;
    private final int widthInBlocks, heightInBlocks;

    private DoubleVector coords;
    private ShapeType nextShape;

    // transition animations related
    private double transitionProgress = 0;
    private Function<Double, Double> distortion = Math::sin;
    /**
     * {@code progress = 0...0.5} - distortion starts from the bottom and
     * proceeds to the top of the screen.
     * {@code 0.5...1.0} - distortion is removed starting from the bottom.
     */
    private double distortionProgress = 0.0;
    private double distortionIntensity;

    private double noiseDensity = 0.0;

    private Random random = new Random();

    // original images
    private BufferedImage currentImage, nextImage;

    public NextShapeDisplay(GameState gameState, DoubleVector coords,
            int widthInBlocks, int heightInBlocks) {
        this.gameState = gameState;
        this.coords = coords;
        this.widthInBlocks = widthInBlocks;
        this.heightInBlocks = heightInBlocks;
        currentImage = generateNextShapeImage(null);
        currentImage = generateNextShapeImage(null);
        distortionIntensity = gameState.getResolution().getBlockWidth() / 6.0;
    }

    public void setNextShape(ShapeType newNextShape) {
        nextShape = newNextShape;
        nextImage = generateNextShapeImage(newNextShape);
    }

    @Override
    public Transform getLocalTransform() {
        return new Transform(coords);
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(Graphics2D gOriginal, double interpolation) {
        // perform actions on the context clone, so that the original one
        // will not get messed up
        Graphics2D g = (Graphics2D) gOriginal.create();
        Assets assets = gameState.getAssets();

        // render the frame of the display
        final int blockWidth = gameState.getResolution().getBlockWidth();
        g.setTransform(getGlobalTransform().tScale(blockWidth).getTransform());
        g.drawImage(assets.getSprite(Assets.SpriteType.NEXT_SHAPE_DISPLAY),
                0, 0, null);

        // render text on the display
        generateProcessedImage(g);
        g.dispose();
    }

    @Override
    public Entity getParentEntity() {
        return null;
    }

    @Override
    public void setTransitionProgress(double progress) {
        progress = Math.min(Math.max(progress, 0.0), 1.0);
        transitionProgress = progress;
        distortionProgress = progress;
        noiseDensity = -0.4 * progress * progress + 0.4 * progress;
    }

    public void startTransitionAnimation() {
        gameState.getAnimationManager().addAnimation(this,
                DisplayAnimationType.TRANSITION,
                new TransitionAnimation(8),
                reason -> {
                    if (!reason.interrupted()) {
                        currentImage = nextImage;
                        transitionProgress = 0;
                        distortionProgress = 0;
                        noiseDensity = 0;
                    }
                });
    }

    private void generateProcessedImage(Graphics2D gOriginal) {
        Graphics2D g = (Graphics2D) gOriginal.create();
        final int blockWidth = gameState.getResolution().getBlockWidth();
        final int pixelWidth = gameState.getResolution().getFontPixelSize() / 8;
        final int imageHeight = heightInBlocks * blockWidth;
        final int glyphWidth = gameState.getResolution().getFontPixelSize();

        // just draw the current image if there is no active transition
        if (transitionProgress == 0) {
            g.drawImage(currentImage, 0, 0, null);
            return;
        }

        g.setClip(glyphWidth / 2, glyphWidth / 2,
                widthInBlocks * blockWidth - glyphWidth,
                heightInBlocks * blockWidth - glyphWidth);

        BufferedImage image = new BufferedImage(widthInBlocks * blockWidth,
                heightInBlocks * blockWidth, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D gImage = (Graphics2D) image.getGraphics();
        gImage.drawImage(currentImage, 0,
                (int) (-imageHeight * transitionProgress), null);

        if (nextImage != null) {
            gImage.drawImage(nextImage, 0,
                    (int) (-imageHeight * transitionProgress) + imageHeight,
                    null);
        }

        // noise
        if (noiseDensity != 0) {
            gImage.setColor(Assets.FONT_COLOR);
            for (int x = 0; x < image.getWidth() / pixelWidth; x++) {
                for (int y = 0; y < image.getHeight() / pixelWidth; y++) {
                    if (random.nextDouble() < noiseDensity) {
                        gImage.fillRect(x * pixelWidth, y * pixelWidth,
                                pixelWidth, pixelWidth);
                    }
                }
            }
        }

        drawDistortedImage(g, image, 0, 0, pixelWidth);
        g.dispose();
    }

    private void drawDistortedImage(Graphics2D g, BufferedImage image,
            int x, int y, int distortionHeight) {
        final int rowsCount = (int) Math.ceil(
                (double) image.getHeight() / distortionHeight);
        int distortionStart
                = (int) (Math.max(1 - 2 * distortionProgress, 0.0) * rowsCount);
        int distortionEnd = (int) ((distortionProgress < 0.5
                        ? 1.0
                        : 2 * (1 - distortionProgress)) * rowsCount);

        for (int i = 0; i < rowsCount; i++) {
            // the closer to the end of an interval, the smaller coefficient is
            int baseIntensity = Math.min(
                    Math.abs(i - distortionEnd), Math.abs(i - distortionStart));

            int distortedX = i >= distortionStart && i <= distortionEnd
                    ? (int) (x + baseIntensity * distortionIntensity
                            * distortion.apply(i / 5.0 + distortionProgress))
                    : x;
            g.drawImage(image.getSubimage(0, i * distortionHeight,
                        image.getWidth(), distortionHeight),
                    distortedX, y + i * distortionHeight, null);
        }
    }

    /**
     * Generates an image that will be shown on the display.
     */
    private BufferedImage generateNextShapeImage(ShapeType nextShape) {
        final int blockWidth = gameState.getResolution().getBlockWidth();
        BufferedImage image = new BufferedImage(widthInBlocks * blockWidth,
                heightInBlocks * blockWidth, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = (Graphics2D) image.getGraphics();
        final int glyphWidth = gameState.getResolution().getFontPixelSize();
        g.setTransform(
                AffineTransform.getTranslateInstance(glyphWidth, glyphWidth));

        g.setFont(new Font(Assets.FONT_NAME,
                Font.PLAIN, gameState.getResolution().getFontSize()));
        g.setColor(Assets.FONT_COLOR);

        drawLines(g, 0, "NEXT:");
        if (nextShape != null) {
            drawLines(g, 2, nextShape.toString().split("\n"));
        }

        int pixelWidth = gameState.getResolution().getFontPixelSize() / 8;
        g.setStroke(new BasicStroke(pixelWidth,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                2 * pixelWidth, new float[]{ 4 * pixelWidth }, 2 * pixelWidth));
        g.drawRect(0, 2 * glyphWidth, 5 * glyphWidth, 5 * glyphWidth);

        return image;
    }

    private void drawLines(Graphics g, int lineIndex, String... lines) {
        // width/height of any glyph
        final int glyphWidth = gameState.getResolution().getFontPixelSize();
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 0, (lineIndex + i + 1) * glyphWidth);
        }
    }
}
