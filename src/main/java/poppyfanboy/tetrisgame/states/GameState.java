package poppyfanboy.tetrisgame.states;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Random;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.AnimationManager;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.entities.NextShapeDisplay;
import poppyfanboy.tetrisgame.entities.ScoreDisplay;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.util.DoubleVector;

public class GameState extends State {
    private Assets assets;
    // core entities
    private GameField gameField;
    private NextShapeDisplay nextShapeDisplay;
    private ScoreDisplay scoreDisplay;

    private AnimationManager animationManager = new AnimationManager();
    private Random random = new Random();

    public GameState(Game game) throws IOException {
        super(game);
        assets = new Assets(game.getResolution(), GameField.DEFAULT_WIDTH,
                GameField.DEFAULT_HEIGHT);

        gameField = new GameField(this, new DoubleVector(11, 4),
                GameField.DEFAULT_WIDTH, GameField.DEFAULT_HEIGHT, random);
        // provide inputs for the game field
        game.getKeyManager().addListener(gameField);

        nextShapeDisplay = new NextShapeDisplay(this, new DoubleVector(23, 4),
                NextShapeDisplay.DEFAULT_WIDTH,
                NextShapeDisplay.DEFAULT_HEIGHT);
        animationManager.addDisplay(nextShapeDisplay);
        gameField.setNextShapeDisplay(nextShapeDisplay);

        scoreDisplay = new ScoreDisplay(this, new DoubleVector(23, 10),
                ScoreDisplay.DEFAULT_WIDTH, ScoreDisplay.DEFAULT_HEIGHT);
        animationManager.addDisplay(scoreDisplay);
        gameField.setScoreDisplay(scoreDisplay);

        // test code
        gameField.start();
    }

    public Assets getAssets() {
        return assets;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public Resolution getResolution() {
        return getGame().getResolution();
    }

    @Override
    public void tick() {
        animationManager.tick();
        gameField.tick();
        nextShapeDisplay.tick();
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        animationManager.perform(interpolation);

        final int blockWidth = getResolution().getBlockWidth();
        g.drawImage(assets.getSprite(Assets.SpriteType.BACKGROUND), 0, 0, null);
        g.drawImage(assets.getSprite(Assets.SpriteType.LOGO),
                13 * blockWidth, 1 * blockWidth, null);

        gameField.render(g, interpolation);
        nextShapeDisplay.render(g, interpolation);
        scoreDisplay.render(g, interpolation);
    }
}
