package poppyfanboy.tetrisgame.entities;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.graphics.displayanimation.AnimatedDisplay;
import poppyfanboy.tetrisgame.graphics.displayanimation.TransitionAnimation;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Transform;

/**
 * An entity that displays the score, level and the number of lines cleared
 * in the game.
 */
public class ScoreDisplay extends Entity implements AnimatedDisplay,
        GameField.ScoreSubscriber {
    public static final int DEFAULT_WIDTH = 7, DEFAULT_HEIGHT = 4;

    private final GameState gameState;
    private final int widthInBlocks, heightInBlocks;

    private DoubleVector coords;

    private int score, level, clearedLinesCount;
    private int nextScore = -1;
    private double transitionProgress = 0;

    public ScoreDisplay(GameState gameState, DoubleVector coords,
            int widthInBlocks, int heightInBlocks) {
        this.gameState = gameState;
        this.coords = coords;
        this.widthInBlocks = widthInBlocks;
        this.heightInBlocks = heightInBlocks;
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
        Graphics2D g = (Graphics2D) gOriginal.create();
        Assets assets = gameState.getAssets();
        final int blockWidth = gameState.getResolution().getBlockWidth();

        g.setTransform(getGlobalTransform().tScale(blockWidth).getTransform());
        g.drawImage(assets.getSprite(Assets.SpriteType.SCORE_DISPLAY),
                0, 0, null);

        g.setFont(new Font(Assets.FONT_NAME, Font.PLAIN,
                gameState.getResolution().getFontSize()));
        g.setColor(Assets.FONT_COLOR);

        int screenScore = nextScore == -1
                ? score
                : score + (int) (transitionProgress * (nextScore - score));

        drawLines(g, 0, "", String.format("SCORE:%06d", screenScore),
                "", String.format("LINES:%d", clearedLinesCount),
                "", String.format("LEVEL:%d", level));
        g.dispose();
    }

    @Override
    public Entity getParentEntity() {
        return null;
    }

    @Override
    public void updateScore(int score, int clearedLinesCount, int level) {
        nextScore = score;
        this.level = level;
        if (clearedLinesCount != this.clearedLinesCount) {
            this.clearedLinesCount = clearedLinesCount;
            startTransitionAnimation();
        }
    }

    private void drawLines(Graphics g, int lineIndex, String... lines) {
        // width/height of any glyph
        final int glyphWidth = gameState.getResolution().getFontPixelSize();
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], glyphWidth,
                    (lineIndex + i + 1) * glyphWidth);
        }
    }

    @Override
    public void setTransitionProgress(double progress) {
        if (nextScore != -1) {
            transitionProgress = progress;
        }
    }

    public void startTransitionAnimation() {
        gameState.getAnimationManager().addAnimation(this,
                DisplayAnimationType.TRANSITION,
                new TransitionAnimation(25),
                reason -> {
                    if (!reason.interrupted()) {
                        score = nextScore;
                        nextScore = -1;
                        transitionProgress = 0;
                    }
                });
    }
}
