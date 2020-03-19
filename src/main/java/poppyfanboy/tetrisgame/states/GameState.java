package poppyfanboy.tetrisgame.states;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Random;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.util.DoubleVector;

public class GameState extends State {
    private Assets assets;
    private GameField gameField;
    private Random random = new Random();

    public GameState(Game game) throws IOException {
        super(game);
        assets = new Assets(game.getResolution(), GameField.DEFAULT_WIDTH,
                GameField.DEFAULT_HEIGHT);

        int blockWidth = game.getResolution().getBlockWidth();
        gameField = new GameField(this,
                new DoubleVector(10 * blockWidth, 3 * blockWidth),
                GameField.DEFAULT_WIDTH, GameField.DEFAULT_HEIGHT,
                random);
        // provide inputs for the game field
        game.getKeyManager().addListener(gameField);

        // test code
        gameField.start();
    }

    public Assets getAssets() {
        return assets;
    }

    public Resolution getResolution() {
        return getGame().getResolution();
    }

    public int getBlockWidth() {
        return getResolution().getBlockWidth();
    }

    @Override
    public void tick() {
        gameField.tick();
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        int blockWidth = getBlockWidth();
        g.drawImage(assets.getSprite(Assets.SpriteType.BACKGROUND), 0, 0, null);
        g.drawImage(assets.getSprite(Assets.SpriteType.LOGO),
                13 * blockWidth, blockWidth, null);
        g.drawImage(assets.getSprite(Assets.SpriteType.NEXT_SHAPE_DISPLAY),
                23 * blockWidth, 4 * blockWidth, null);
        g.drawImage(assets.getSprite(Assets.SpriteType.SCORE_DISPLAY),
                23 * blockWidth, 10 * blockWidth, null);

        gameField.render(g, interpolation);
    }
}
