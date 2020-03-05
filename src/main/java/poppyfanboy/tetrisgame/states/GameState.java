package poppyfanboy.tetrisgame.states;

import java.awt.Graphics2D;
import java.util.Random;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.util.DoubleVector;

public class GameState extends State {
    private int blockWidth;

    private GameField gameField;
    private Random random = new Random();

    public GameState(Game game, int blockWidth) {
        super(game);
        gameField = new GameField(this, new DoubleVector(20, 20),
                GameField.DEFAULT_WIDTH, GameField.DEFAULT_HEIGHT,
                random);
        this.blockWidth = blockWidth;
        // provide inputs for the game field
        game.getKeyManager().addListener(gameField);

        // test code
        gameField.start();
    }

    public Assets getAssets() {
        return getGame().getAssets();
    }

    public int getBlockWidth() {
        return blockWidth;
    }

    @Override
    public void tick() {
        gameField.tick();
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        gameField.render(g, interpolation);
    }
}
