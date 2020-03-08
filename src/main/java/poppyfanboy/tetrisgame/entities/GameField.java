package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.shapetypes.ShapeType;
import poppyfanboy.tetrisgame.entities.shapetypes.TetrisShapeType;
import poppyfanboy.tetrisgame.graphics.animation.HVLinearAnimation;
import poppyfanboy.tetrisgame.input.Controllable;
import poppyfanboy.tetrisgame.input.InputKey;
import poppyfanboy.tetrisgame.input.KeyState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;
import poppyfanboy.tetrisgame.util.Util;

import static java.lang.Math.abs;
import static poppyfanboy.tetrisgame.entities.GameField.ActiveShapeState.*;
import static poppyfanboy.tetrisgame.entities.GameField.GameFieldState.*;
import static poppyfanboy.tetrisgame.util.IntVector.iVect;

/**
 * A game field entity. Wraps several block entities and a single shape
 * entity that can be moved and rotated.
 */
public class GameField extends Entity implements TileField, Controllable {
    public static int DEFAULT_WIDTH = 10, DEFAULT_HEIGHT = 20;
    public static IntVector SPAWN_COORDINATES = iVect(2, 0);

    // the game state to which this game field belongs
    private GameState gameState;
    private EnumMap<InputKey, KeyState> lastInputs;

    // do something with the possible null pointer exception errors
    private Shape activeShape;
    // sort blocks by Y coordinate from the top to the bottom
    // (in a row-major order) (maps coordinates to the block entities)
    private NavigableMap<IntVector, Block> fallenBlocks
            = new TreeMap<>(IntVector.Y_ORDER);

    private DoubleVector coords;
    private double rotationAngle;

    private int widthInBlocks, heightInBlocks;
    private Entity parentEntity;

    // used to generate new falling shapes
    private final Random random;


    // stuff related to game logic
    private int level = 1;
    // how many ticks past since last active shape drop
    private int lastDropCounter = 0;

    private int softDropDuration = Game.TICKS_PER_SECOND / 4;
    private int forcedDropDuration = Game.TICKS_PER_SECOND / 16;
    private int userControlAnimationDuration = softDropDuration;

    private ActiveShapeState activeShapeState = NOT_SPAWNED;
    private GameFieldState gameFieldState = STOPPED;

    private ArrayList<Block> removedBlocks = new ArrayList<>();
    private ArrayList<Block> droppedBlocks = new ArrayList<>();
    private ArrayList<IntVector> droppedBlocksOldKeys = new ArrayList<>();

    /**
     * Creates an empty instance of a game field.
     *
     * @param   coords coordinates of the game field relative to the
     *          parent entity.
     */
    public GameField(GameState gameState, DoubleVector coords,
            int widthInBlocks, int heightInBlocks, Entity parentEntity,
            Random random) {
        this.gameState = gameState;
        this.widthInBlocks = widthInBlocks;
        this.heightInBlocks = heightInBlocks;
        this.parentEntity = parentEntity;
        this.random = random;

        this.coords = coords;
        this.rotationAngle = 0;
    }

    /**
     * Creates a game field with no parent entity.
     */
    public GameField(GameState gameState, DoubleVector coords,
            int widthInBlocks, int heightInBlocks, Random random) {
        this(gameState, coords, widthInBlocks, heightInBlocks, null,
                random);
    }

    public void start() {
        gameFieldState = SHAPE_FALLING;
        activeShapeState = SOFT_DROPPING;

        ShapeType randomType
                = Util.getRandomInstance(random, TetrisShapeType.class);
        randomType = TetrisShapeType.I_SHAPE;
        BlockColor randomColor
                = Util.getRandomInstance(random, BlockColor.class);
        BlockColor[] colors
                = Shape.generateColorsArray(randomType, randomColor);
        spawnNewActiveShape(randomType, SPAWN_COORDINATES, Rotation.INITIAL,
                colors);
    }

    /**
     * Try to spawn the specified shape on a game field.
     *
     * This method can fail in case the inserted shape overlaps some
     * of the blocks on the field or it is out of the field bounds.
     */
    private boolean spawnNewActiveShape(ShapeType shapeType,
            IntVector tileCoords, Rotation rotation,
            BlockColor[] blockColors) {
        if (!Shape.fits(shapeType, tileCoords, rotation, this)) {
            return false;
        }
        activeShape = new Shape(gameState, shapeType, rotation,
                tileCoords, blockColors, this);
        return true;
    }

    /**
     * There is no need in checking the whole set of the blocks, since
     * when the shape falls it can make at most 4 filled rows, so
     * we can restrict the searching area to 4 specific rows
     * (from startY to endY, boundaries included)
     */
    private void removeFilledRows(int startY, int endY) {
        assert startY <= endY;

        final int width = getWidthInBlocks();
        IntVector startCoords = new IntVector(0, startY);
        IntVector endCoords = new IntVector(width - 1, endY);
        // the iterator returns the blocks from top the top
        // ones down to the bottom ones
        Collection<Block> removalCandidates
                = fallenBlocks.subMap(startCoords, true, endCoords, true)
                .values();
        int currentRow = startY - 1;

        // line indices are sorted from the top to the bottom
        ArrayList<Integer> clearedLinesIndices = new ArrayList<>();
        removedBlocks = new ArrayList<>();
        // current same-row-blocks-streak
        ArrayList<Block> currentRowBlocks = new ArrayList<>();
        for (Block block : removalCandidates) {
            if (currentRow != block.getTileCoords().getY()) {
                if (currentRowBlocks.size() == width) {
                    removedBlocks.addAll(currentRowBlocks);
                    clearedLinesIndices.add(currentRow);
                }
                currentRowBlocks.clear();
                currentRowBlocks.add(block);
                currentRow = block.getTileCoords().getY();
            } else {
                currentRowBlocks.add(block);
            }
        }
        // the last row
        if (currentRowBlocks.size() == width) {
            removedBlocks.addAll(currentRowBlocks);
            clearedLinesIndices.add(currentRow);
        }

        // move down rows that were above the cleared rows
        if (clearedLinesIndices.size() != 0) {
            final int bottomLine = clearedLinesIndices
                    .get(clearedLinesIndices.size() - 1) - 1;
            droppedBlocks = new ArrayList<>();
            Collection<Block> blocksFallingDown = fallenBlocks.subMap(
                    new IntVector(0, 0), true,
                    new IntVector(width - 1, bottomLine), true).values();
            // these has not yet been removed from the fallenBlocks mapping
            blocksFallingDown.removeAll(removedBlocks);

            int removedLinesLeft = clearedLinesIndices.size();
            final int removedLinesCount = clearedLinesIndices.size();
            droppedBlocksOldKeys = new ArrayList<>();
            for (Block block : blocksFallingDown) {
                // skip through the cleared lines to the one that is right
                // under the current block
                while (block.getTileCoords().getY()
                        > clearedLinesIndices.get(removedLinesCount - removedLinesLeft)) {
                    removedLinesLeft--;
                    if (removedLinesLeft == 0) {
                        break;
                    }
                }
                if (removedLinesLeft == 0) {
                    break;
                }
                droppedBlocks.add(block);
                droppedBlocksOldKeys.add(block.getTileCoords());
                block.tileShift(iVect(0, removedLinesLeft));
            }
        }
    }

    @Override
    public int getWidthInBlocks() {
        return widthInBlocks;
    }

    @Override
    public int getHeightInBlocks() {
        return heightInBlocks;
    }

    @Override
    public Collection<? extends TileFieldObject> getObjects() {
        ArrayList<TileFieldObject> objects
                = new ArrayList<>(fallenBlocks.values());
        if (activeShape != null) {
            objects.add(activeShape);
        }
        return objects;
    }

    @Override
    public Entity getParentEntity() {
        return parentEntity;
    }

    @Override
    public Transform getLocalTransform() {
        int blockWidth = gameState.getBlockWidth();
        int width = widthInBlocks * blockWidth;
        int height = heightInBlocks * blockWidth;
        DoubleVector rotationPivot = coords.add(width / 2.0, height / 2.0);

        return new Transform(coords)
            .combine(Transform.getRotation(rotationAngle, rotationPivot));
    }

    @Override
    public Transform getAdditionalTransform(Entity entity) {
        if (entity == activeShape) {
            // fit the shape into the game field
            final int blockWidth = gameState.getBlockWidth();
            DoubleVector[] convexHull = activeShape.getConvexHull();

            int width = getWidthInBlocks() * blockWidth;
            int height = getHeightInBlocks() * blockWidth;
            // (x1, y1) - upper left corner of the game field
            // (x2, y2) - bottom right corner of the game field
            double x1 = 0, y1 = 0;
            double x2 = x1 + width, y2 = y1 + height;

            // additional shifts to try to put the shape inside the game
            // field frame
            int fitDX = 0, fitDY = 0;
            for (DoubleVector point : convexHull) {
                if (point.getX() < x1
                        && abs(x1 - point.getX()) > abs(fitDX)) {
                    fitDX = (int) (x1 - point.getX());
                }
                if (point.getX() > x2
                        && abs(x2 - point.getX()) > abs(fitDX)) {
                    fitDX = (int) (x2 - point.getX());
                }
                if (point.getY() < y1
                        && abs(y1 - point.getY()) > abs(fitDY)) {
                    fitDY = (int) (y1 - point.getY());
                }
                if (point.getY() > y2
                        && abs(y2 - point.getY()) > abs(fitDY)) {
                    fitDY = (int) (y2 - point.getY());
                }
            }
            return new Transform(new DoubleVector(fitDX, fitDY));
        } else {
            return new Transform();
        }
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        Transform globalTransform = getGlobalTransform();
        DoubleVector coords
                = globalTransform.apply(new DoubleVector(0, 0));

        g.drawRect((int) coords.getX() - 1, (int) coords.getY() - 1,
                widthInBlocks * gameState.getBlockWidth() + 2,
                heightInBlocks * gameState.getBlockWidth() + 2);
        if (activeShape != null) {
            activeShape.render(g, interpolation);
        }
        for (Block block : fallenBlocks.values()) {
            block.render(g, interpolation);
        }
    }

    @Override
    public DoubleVector[] getVertices() {
        Transform globalTransform = getGlobalTransform();
        return globalTransform.apply(new DoubleVector[] {
            DoubleVector.dVect(0, 0),
            DoubleVector.dVect(0, heightInBlocks),
            DoubleVector.dVect(widthInBlocks, heightInBlocks),
            DoubleVector.dVect(widthInBlocks, 0)});
    }

    @Override
    public void tick() {
        if (activeShape != null) {
            activeShape.tick();
        }
        for (Block block : fallenBlocks.values()) {
            block.tick();
        }

        if (gameFieldState == DROPPING_BLOCKS) {
            boolean animationsFinished = true;
            for (Block block : fallenBlocks.values()) {
                if (block.getTimeTillAnimationFinishes() > 0) {
                    animationsFinished = false;
                    break;
                }
            }
            if (animationsFinished) {
                gameFieldState = READY_TO_SPAWN_NEW_SHAPE;
            }
        }
        if (!activeShapeState.fell()) {
            lastDropCounter++;
        }

        if (lastDropCounter >= softDropDuration
                && activeShapeState == SOFT_DROPPING
                || lastDropCounter >= forcedDropDuration
                && activeShapeState == FORCED_DROPPING) {
            if (Shape.fits(activeShape, activeShape.getShapeType(),
                    activeShape.getTileCoords().add(iVect(0, 1)),
                    activeShape.getRotation(), this)) {
                activeShape.tileShift(iVect(0, 1));
                int duration = activeShapeState == FORCED_DROPPING
                        ? forcedDropDuration
                        : softDropDuration;
                activeShape.addDropAnimation(duration);
                lastDropCounter = 0;
            } else {
                activeShapeState = FELL;
            }
        }

        if (activeShapeState == FELL
                && activeShape.getTimeTillAnimationFinishes() == 0) {
            final int startY = activeShape.getTileCoords().getY();
            // lock the active shape
            for (Block block : activeShape.getBlocks(this)) {
                fallenBlocks.put(block.getTileCoords(), block);
            }
            activeShape = null;
            activeShapeState = LOCKED;


            removeFilledRows(startY, startY + 3);
            if (!removedBlocks.isEmpty()) {
                for (Block block : removedBlocks) {
                    fallenBlocks.remove(block.getTileCoords());
                }
                removedBlocks.clear();

                for (Block block : droppedBlocks) {
                    block.addDropAnimation();
                }
                // update the mappings in the fallenBlocks mapping
                // (make sure to update the blocks from the bottom to the top,
                // otherwise you may override some present blocks' coordinates)
                for (int i = droppedBlocksOldKeys.size() - 1; i >= 0; i--) {
                    Block block = droppedBlocks.get(i);
                    fallenBlocks.remove(droppedBlocksOldKeys.get(i));
                    fallenBlocks.put(block.getTileCoords(), block);
                }
                droppedBlocks.clear();
                droppedBlocksOldKeys.clear();
                gameFieldState = DROPPING_BLOCKS;
            } else {
                gameFieldState = READY_TO_SPAWN_NEW_SHAPE;
            }
        }

        if (gameFieldState == READY_TO_SPAWN_NEW_SHAPE) {
            activeShapeState = SOFT_DROPPING;
            if (lastInputs != null) {
                KeyState downKeyState = lastInputs.get(InputKey.ARROW_DOWN);
                if (downKeyState == KeyState.HELD
                        || downKeyState == KeyState.PRESSED) {
                    activeShapeState = FORCED_DROPPING;
                }
            }
            gameFieldState = SHAPE_FALLING;

            ShapeType randomType
                    = Util.getRandomInstance(random, TetrisShapeType.class);
            randomType = TetrisShapeType.I_SHAPE;
            BlockColor randomColor
                    = Util.getRandomInstance(random, BlockColor.class);
            BlockColor[] colors
                    = Shape.generateColorsArray(randomType, randomColor);
            spawnNewActiveShape(randomType, SPAWN_COORDINATES,
                    Rotation.INITIAL, colors);
        }
    }

    @Override
    public void control(EnumMap<InputKey, KeyState> inputs) {
        lastInputs = inputs;

        int xShift = 0;
        Rotation rotationDirection = Rotation.INITIAL;

        for (EnumMap.Entry<InputKey, KeyState> key : inputs.entrySet()) {
            if (key.getValue() == KeyState.PRESSED) {
                switch (key.getKey()) {
                    case ARROW_DOWN:
                        if (activeShapeState.fell()) {
                            break;
                        }
                        if (activeShapeState == SOFT_DROPPING) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / softDropDuration)
                                * forcedDropDuration);
                        }
                        activeShapeState = FORCED_DROPPING;
                        HVLinearAnimation currentDropAnimation =
                                activeShape.getDropAnimation();
                        if (currentDropAnimation != null
                                && !currentDropAnimation.finished()) {
                            currentDropAnimation
                                    .changeDuration(forcedDropDuration);
                        }
                        break;
                    case ARROW_LEFT:
                        xShift--;
                        break;
                    case ARROW_RIGHT:
                        xShift++;
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
                        if (activeShapeState.fell()) {
                            break;
                        }
                        if (activeShapeState == FORCED_DROPPING) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / forcedDropDuration)
                                * softDropDuration);
                        }
                        activeShapeState = SOFT_DROPPING;
                        HVLinearAnimation currentDropAnimation =
                                activeShape.getDropAnimation();
                        if (currentDropAnimation != null
                                && !currentDropAnimation.finished()) {
                            currentDropAnimation
                                    .changeDuration(softDropDuration);
                        }
                        break;
                }
            }
        }
        // for now just block controls when the shape falls completely
        // on the ground; later change it in a way that hitting control
        // buttons in this state, when the old shape fell but a new
        // one has not yet spawned, would change the initial position
        // of a newly spawned shape
        if (xShift != 0 && !activeShapeState.fell()) {
            if (activeShape != null && Shape.fits(activeShape,
                    activeShape.getShapeType(),
                    activeShape.getTileCoords().add(iVect(xShift, 0)),
                    activeShape.getRotation(), this)) {
                activeShape.tileShift(iVect(xShift, 0));
                activeShape.addUserControlAnimation(
                        userControlAnimationDuration);
            }
        }
        if (activeShape != null && !activeShapeState.fell()
                && (rotationDirection.equals(Rotation.LEFT)
                || rotationDirection.equals(Rotation.RIGHT))) {
            Rotation newRotation
                    = activeShape.getRotation().add(rotationDirection);
            if (Shape.fits(activeShape, activeShape.getShapeType(),
                    activeShape.getTileCoords(), newRotation, this)) {
                activeShape.rotate(rotationDirection);
                activeShape.addRotationAnimation(
                        rotationDirection, userControlAnimationDuration);
            } else {
                IntVector[] wallKicks = rotationDirection == Rotation.RIGHT
                        ? activeShape.getRightWallKicks()
                        : activeShape.getLeftWallKicks();
                for (IntVector shift : wallKicks) {
                    if (Shape.fits(activeShape, activeShape.getShapeType(),
                            activeShape.getTileCoords().add(shift),
                            newRotation, this)) {
                        // rotate and wall kick
                        activeShape.rotate(rotationDirection);
                        activeShape.tileShift(shift);

                        activeShape.addMovementAnimation(
                                userControlAnimationDuration);
                        activeShape.addRotationAnimation(rotationDirection,
                                userControlAnimationDuration);
                        // wait until the wall kick animation finishes
                        lastDropCounter = 0;
                        break;
                    }
                }
            }
        }
    }

    public enum ActiveShapeState {
        NOT_SPAWNED, SOFT_DROPPING, FORCED_DROPPING, FELL, LOCKED;

        public boolean fell() {
            return this == FELL || this == LOCKED;
        }
    };

    public enum GameFieldState {
        STOPPED, PAUSED, SHAPE_FALLING, CLEARING_FILLED_LINES,
        DROPPING_BLOCKS, READY_TO_SPAWN_NEW_SHAPE
    };
}
