package poppyfanboy.tetrisgame.states;

import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Random;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.entities.Shape;
import poppyfanboy.tetrisgame.entities.shapetypes.TetrisShapeType;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.input.InputKey;
import poppyfanboy.tetrisgame.input.KeyState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.Rotation;

public class GameState extends State {
    private int blockWidth;
    private int defaultAnimationDuration;

    private GameField gameField;
    private Random random = new Random();

    public GameState(Game game, int blockWidth,
                     int defaultAnimationDuration) {
        super(game);
        gameField = new GameField(this, new DoubleVector(20, 20),
                GameField.DEFAULT_WIDTH, GameField.DEFAULT_HEIGHT);
        this.blockWidth = blockWidth;
        this.defaultAnimationDuration = defaultAnimationDuration;

        // test code
        Shape activeShape = Shape.getRandomShapeRandomlyColored(random,
                this, Rotation.INITIAL, new IntVector(0, 0), gameField,
                TetrisShapeType.class);
        gameField.spawnNewActiveShape(activeShape);
    }

    public Assets getAssets() {
        return getGame().getAssets();
    }

    public int getBlockWidth() {
        return blockWidth;
    }

    public int getAnimationDuration() {
        return defaultAnimationDuration;
    }

    @Override
    public void tick() {
        gameField.tick();
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        gameField.render(g, interpolation);
    }

    @Override
    public void control(EnumMap<InputKey, KeyState> inputs) {
        IntVector shift = new IntVector(0, 0);
        Rotation rotationDirection = Rotation.INITIAL;

        for (EnumMap.Entry<InputKey, KeyState> key : inputs.entrySet()) {
            if (key.getValue() == KeyState.PRESSED) {
                switch (key.getKey()) {
                    case ARROW_UP:
                        shift = shift.add(0, -1);
                        break;
                    case ARROW_DOWN:
                        shift = shift.add(0, 1);
                        break;
                    case ARROW_LEFT:
                        shift = shift.add(-1, 0);
                        break;
                    case ARROW_RIGHT:
                        shift = shift.add(1, 0);
                        break;
                    case W:
                        rotationDirection
                            = rotationDirection.add(Rotation.LEFT);
                        break;
                    case S:
                        rotationDirection
                            = rotationDirection.add(Rotation.RIGHT);
                        break;
                }
            }
        }
        if (!shift.equals(new IntVector(0, 0))) {
            gameField.shiftActiveShape(shift);
        }
        if (rotationDirection.equals(Rotation.LEFT)) {
            gameField.rotateActiveShapeLeft();
        }
        if (rotationDirection.equals(Rotation.RIGHT)) {
            gameField.rotateActiveShapeRight();
        }
    }
}
