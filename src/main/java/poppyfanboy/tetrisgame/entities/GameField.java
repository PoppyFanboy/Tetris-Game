package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import java.util.Random;
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

import static poppyfanboy.tetrisgame.util.IntVector.iVect;

/**
 * A game field entity. Wraps several block entities and a single shape
 * entity that can be moved and rotated.
 */
public class GameField extends Entity implements TileField, Controllable {
    public static int DEFAULT_WIDTH = 10, DEFAULT_HEIGHT = 20;

    // the game state to which this game field belongs
    private GameState gameState;

    private Shape activeShape;
    // probably replace with tree set later
    private List<Block> fallenBlocks = new LinkedList<>();

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
                TetrisShapeType.class);
        spawnNewActiveShape(activeShape);
    }

    public void pause() {
        // implementation
    }

    public void stop() {
        // implementation
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
            fallenBlocks.addAll(Arrays.asList(activeShape.getBlocks()));
        }
        activeShape = newShape;
        return true;
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
        for (Block block : field.fallenBlocks) {
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
    private boolean activeShapeSoftDrop() {
        if (activeShape == null) {
            return false;
        }
        if (tryPut(activeShape, iVect(0, 1), this)) {
            activeShape.softDrop();
            return true;
        }
        return false;
    }

    private void activeShapeSetForcedDrop(boolean option) {
        activeShape.setForcedDrop(option);
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
                    return true;
                }
            }
            return false;
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
        ArrayList<TileFieldObject> objects = new ArrayList<>(fallenBlocks);
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
    public Transform getGlobalTransform() {
        if (parentEntity != null) {
            return parentEntity.getGlobalTransform()
                    .combine(getLocalTransform());
        } else {
            return getLocalTransform();
        }
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
        for (Block block : fallenBlocks) {
            block.render(g, interpolation);
        }
    }

    @Override
    public DoubleVector[] getVertices() {
        Transform globalTransform = getGlobalTransform();
        return globalTransform.apply(new DoubleVector[] {
            DoubleVector.dVect(0, 0), DoubleVector.dVect(0, heightInBlocks),
            DoubleVector.dVect(widthInBlocks, heightInBlocks),
            DoubleVector.dVect(widthInBlocks, 0)});
    }

    @Override
    public void tick() {
        if (activeShape != null) {
            activeShape.tick();
        }
        for (Block block : fallenBlocks) {
            block.tick();
        }
        lastDropCounter++;
        if (lastDropCounter >= softDropDuration && !forcedDrop
                || lastDropCounter >= forcedDropDuration && forcedDrop) {
            if (!activeShapeSoftDrop()) {
                Shape newActiveShape
                        = Shape.getRandomShapeEvenlyColored(random,
                        gameState, Rotation.INITIAL, new IntVector(0, 0),
                        this, TetrisShapeType.class);
                this.spawnNewActiveShape(newActiveShape);
                newActiveShape.setForcedDrop(forcedDrop);
            }
            lastDropCounter = 0;
        }
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
                        activeShapeSetForcedDrop(true);
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
                        activeShapeSetForcedDrop(false);
                        break;
                }
            }
        }
        if (!shift.equals(new IntVector(0, 0))) {
            shiftActiveShape(shift);
        }

        if (rotationDirection.equals(Rotation.LEFT)
                || rotationDirection.equals(Rotation.RIGHT)) {
            rotateActiveShape(rotationDirection);
        }
    }
}
