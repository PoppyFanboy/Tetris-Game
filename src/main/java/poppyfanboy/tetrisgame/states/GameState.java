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

    private int level = 1;
    // how many ticks past since last active shape drop
    private int lastDropCounter = 0;
    // how many ticks does a single soft drop last at the current level
    private int softDropDuration = Game.TICKS_PER_SECOND;
    private int forcedDropDuration = Game.TICKS_PER_SECOND / 4;

    private boolean forcedDrop = false;

    private GameField gameField;
    private Random random = new Random();

    public GameState(Game game, int blockWidth) {
        super(game);
        gameField = new GameField(this, new DoubleVector(20, 20),
                GameField.DEFAULT_WIDTH, GameField.DEFAULT_HEIGHT);
        this.blockWidth = blockWidth;

        // test code
        Shape activeShape = Shape.getRandomShapeEvenlyColored(random,
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

    public int getUserControlAnimationDuration() {
        return softDropDuration / 2;
    }

    public int getRotateAnimationDuration() {
        return softDropDuration / 4;
    }

    public int getSoftDropAnimationDuration() {
        return softDropDuration;
    }

    public int getForcedDropAnimationDuration() {
        return forcedDropDuration;
    }

    @Override
    public void tick() {
        gameField.tick();
        lastDropCounter++;
        if (lastDropCounter >= softDropDuration && !forcedDrop
                || lastDropCounter >= forcedDropDuration && forcedDrop) {
            if (!gameField.activeShapeSoftDrop()) {
                Shape newActiveShape
                        = Shape.getRandomShapeEvenlyColored(random,
                        this, Rotation.INITIAL, new IntVector(0, 0),
                        gameField, TetrisShapeType.class);
                gameField.spawnNewActiveShape(newActiveShape);
            }
            lastDropCounter = 0;
        }
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
                    case ARROW_DOWN:
                        if (!forcedDrop) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / softDropDuration)
                                * forcedDropDuration);
                        }
                        forcedDrop = true;
                        gameField.activeShapeSetForcedDrop(true);
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
            if (key.getValue() == KeyState.RELEASED) {
                switch (key.getKey()) {
                    case ARROW_DOWN:
                        if (forcedDrop) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / forcedDropDuration)
                                * softDropDuration);
                        }
                        forcedDrop = false;
                        gameField.activeShapeSetForcedDrop(false);
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
