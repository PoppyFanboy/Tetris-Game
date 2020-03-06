package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.shapetypes.TetrisShapeType;
import poppyfanboy.tetrisgame.input.Controllable;
import poppyfanboy.tetrisgame.input.InputKey;
import poppyfanboy.tetrisgame.input.KeyState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;

import static java.lang.Math.abs;
import static poppyfanboy.tetrisgame.util.IntVector.iVect;

/**
 * A game field entity. Wraps several block entities and a single shape
 * entity that can be moved and rotated.
 */
public class GameField extends Entity implements TileField, Controllable {
    public static int DEFAULT_WIDTH = 10, DEFAULT_HEIGHT = 20;

    // the game state to which this game field belongs
    private GameState gameState;

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

    // how much time left until the next shape should be spawned
    // after the previous one completely fell
    private int appearanceDelayTimer = -1;
    private boolean shapeFallen = false;

    private int softDropDuration = Game.TICKS_PER_SECOND / 4;
    private int forcedDropDuration = Game.TICKS_PER_SECOND / 16;

    private boolean forcedDrop = false;

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
        Shape activeShape = Shape.getRandomShapeEvenlyColored(random,
                gameState, Rotation.INITIAL, new IntVector(0, 0), this,
                this, TetrisShapeType.class);
        spawnNewActiveShape(activeShape);
    }

    public int getRotateAnimationDuration() {
        return softDropDuration / 4;
    }

    public int getForcedDropAnimationDuration() {
        return forcedDropDuration;
    }

    public int getSoftDropAnimationDuration() {
        return softDropDuration;
    }

    public int getUserControlAnimationDuration() {
        return softDropDuration / 4;
    }

    /**
     * Try to spawn the specified shape on a game field. The position at
     * which the shape is spawned, is specified in the shape entity itself.
     *
     * This method can fail in case the inserted shape overlaps some
     * of the blocks on the field or it is out of the field bounds.
     */
    private boolean spawnNewActiveShape(Shape newShape) {
        if (!tryPut(newShape, this)) {
            return false;
        }
        // glue the previously active shape to the field
        if (activeShape != null) {
            for (Block block : activeShape.getBlocks(this)) {
               fallenBlocks.put(block.getTileCoords(), block);
            }
        }
        activeShape = newShape;
        return true;
    }

    /**
     * Tries to move the active shape to the specified position.
     *
     * @return  {@code false} in case it fails or there is no
     *          currently active shape.
     */
    private boolean moveActiveShape(IntVector newCoordinates) {
        IntVector shiftDirection
            = newCoordinates.subtract(activeShape.getTileCoords());
        return shiftActiveShape(shiftDirection);
    }

    /**
     * Drops the active shape by one block down. The difference with the
     * {@link GameField#shiftActiveShape(IntVector)} method is that
     * the latter is made to handle user inputs and usually the animations
     * for those are a bit faster.
     */
    private boolean activeShapeDrop() {
        if (activeShape == null) {
            return false;
        }
        if (tryPut(activeShape, iVect(0, 1), this)) {
            activeShape.drop();
            return true;
        }
        return false;
    }

    /**
     * Tries to shift the active shape in the specified direction.
     *
     * @return  {@code false} in case it fails or there is no
     *          currently active shape.
     */
    private boolean shiftActiveShape(IntVector shiftDirection) {
        if (activeShape == null) {
            return false;
        }
        if (tryPut(activeShape, shiftDirection, this)) {
            activeShape.tileShift(shiftDirection);
            return true;
        }
        return false;
    }

    private boolean rotateActiveShape(Rotation rotationDirection) {
        if (rotationDirection != Rotation.RIGHT
                && rotationDirection != Rotation.LEFT) {
            return false;
        }
        if (activeShape == null) {
            return false;
        }
        Rotation newRotation
            = activeShape.getRotation().add(rotationDirection);
        if (tryPut(activeShape, newRotation, this)) {
            activeShape.rotate(rotationDirection);
            return true;
        } else {
            IntVector[] wallKicks = rotationDirection == Rotation.RIGHT
                ? activeShape.getRightWallKicks()
                : activeShape.getLeftWallKicks();
            for (IntVector shift : wallKicks) {
                if (tryPut(activeShape, shift, newRotation, this)) {
                    // rotate and wall kick
                    activeShape.rotate(rotationDirection);
                    activeShape.tileShift(shift);
                    activeShape.interruptDropAnimation();
                    lastDropCounter = Math.max(softDropDuration, forcedDropDuration);
                    return true;
                }
            }
            return false;
        }
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
        // * the iterator returns the blocks in the ascending order
        Collection<Block> removalCandidates
                = fallenBlocks.subMap(startCoords, true, endCoords, true)
                .values();
        int currentRow = startY - 1;

        // line indices are sorted from the top to the bottom
        ArrayList<Integer> clearedLinesIndices = new ArrayList<>();
        ArrayList<Block> removedBlocks = new ArrayList<>();
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
        removalCandidates.removeAll(removedBlocks);

        // move down rows that were above the cleared rows
        if (clearedLinesIndices.size() != 0) {
            Collection<Block> blocksFallingDown = fallenBlocks.subMap(
                    new IntVector(0, 0), true,
                    new IntVector(width - 1, clearedLinesIndices.get(clearedLinesIndices.size() - 1)), false).values();
            int closestRemovedRowIndex = clearedLinesIndices.size() - 1;

            ArrayList<IntVector> oldKeys = new ArrayList<>();
            // ArrayList<IntVector> newKeys = new ArrayList<>();
            for (Block block : blocksFallingDown) {
                while (block.getTileCoords().getY()
                        > clearedLinesIndices.get(closestRemovedRowIndex)) {
                    closestRemovedRowIndex--;
                }
                // update the coordinates in the mapping
                IntVector oldCoords = block.getTileCoords();
                IntVector newCoords = block.getTileCoords().add(0, closestRemovedRowIndex + 1);
                oldKeys.add(oldCoords);
                // newKeys.add(newCoords);

                // block.tileShift(new IntVector(0, closestRemovedRowIndex + 1));
                block.dropDown(closestRemovedRowIndex + 1);
            }

            // update the fallenBlocks set
            // (make sure to update the blocks from the bottom to the top,
            // otherwise you may override some present blocks' coordinates)
            for (int i = oldKeys.size() - 1; i >= 0; i--) {
                Block block = fallenBlocks.get(oldKeys.get(i));
                fallenBlocks.remove(oldKeys.get(i));
                fallenBlocks.put(block.getTileCoords(), block);
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
        lastDropCounter++;

        if (shapeFallen && appearanceDelayTimer > 0) {
            appearanceDelayTimer--;
        }
        if (appearanceDelayTimer == 0) {
            appearanceDelayTimer = -1;
            shapeFallen = false;

            final int startY = activeShape.getTileCoords().getY();
            Shape newActiveShape
                    = Shape.getRandomShapeEvenlyColored(random,
                    gameState, Rotation.INITIAL, new IntVector(0, 0),
                    this, this, TetrisShapeType.class);
            this.spawnNewActiveShape(newActiveShape);
            newActiveShape.setForcedDrop(forcedDrop);

            // the old active shape first needs to be broken into blocks
            removeFilledRows(startY, startY + 3);
        }

        if (lastDropCounter >= softDropDuration && !forcedDrop
                || lastDropCounter >= forcedDropDuration && forcedDrop) {
            if (activeShapeDrop()) {
                lastDropCounter = 0;
            } else {
                appearanceDelayTimer
                        = activeShape.getTimeTillAnimationFinishes();
                shapeFallen = true;
            }
        }
    }

    @Override
    public void control(EnumMap<InputKey, KeyState> inputs) {
        int xShift = 0;
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
                        activeShape.setForcedDrop(true);
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
                        if (forcedDrop) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / forcedDropDuration)
                                * softDropDuration);
                        }
                        forcedDrop = false;
                        activeShape.setForcedDrop(false);
                        break;
                }
            }
        }
        // for now just block controls when the shape falls completely
        // on the ground; later change it in a way that hitting control
        // buttons in this state, when the old shape fell but a new
        // one has not yet spawned, would change the initial position
        // of a newly spawned shape
        if (xShift != 0 && !shapeFallen) {
            if (activeShape != null
                    && tryPut(activeShape, new IntVector(xShift, 0), this)) {
                activeShape.userControl(xShift);
            }
        }
        if (!shapeFallen && (rotationDirection.equals(Rotation.LEFT)
                || rotationDirection.equals(Rotation.RIGHT))) {
            rotateActiveShape(rotationDirection);
        }
    }

    // shifts the shape, puts it into the specified rotation and tries
    // to insert it into this new position without actually mutating
    // the entity itself
    private static boolean tryPut(Shape shape, IntVector shiftDirection,
                                  Rotation rotation, GameField field) {
        // check for out of bounds indices
        if (!shape.shiftedBoundsCheck(shiftDirection, rotation,
                field.widthInBlocks, field.heightInBlocks)) {
            return false;
        }
        // check for collisions
        for (Block block : field.fallenBlocks.values()) {
            if (shape.checkShiftedCollision(block.getTileCoords(),
                    shiftDirection, rotation)) {
                return false;
            }
        }
        return true;
    }

    // does not rotate the shape
    private static boolean tryPut(Shape shape, IntVector shiftDirection,
                                  GameField field) {
        return tryPut(shape, shiftDirection, shape.getRotation(), field);
    }

    // does not shift the shape
    private static boolean tryPut(Shape shape, Rotation rotation,
                                  GameField field) {
        return tryPut(shape, new IntVector(0, 0), rotation, field);
    }

    // tries to put the shape as it is
    private static boolean tryPut(Shape shape, GameField field) {
        return
                tryPut(shape, new IntVector(0, 0), shape.getRotation(), field);
    }
}
