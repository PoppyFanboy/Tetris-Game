package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Objects;
import static poppyfanboy.tetrisgame.entities.GameField.GameFieldState.SHAPE_SOFT_DROP;
import poppyfanboy.tetrisgame.graphics.AnimationEndHandler;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.entities.shapetypes.ShapeType;
import poppyfanboy.tetrisgame.graphics.animation2D.HVLinearAnimation;
import poppyfanboy.tetrisgame.graphics.animation2D.MoveAnimation;
import poppyfanboy.tetrisgame.graphics.animation2D.RotationAnimation;
import poppyfanboy.tetrisgame.graphics.animation2D.Animated2D;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;

/**
 * In a nutshell this is just a bunch of glued blocks, that can be rotated
 * and moved around the game field.
 */
public class Shape extends Entity implements TileFieldObject, Animated2D {
    private GameState gameState;

    // tile field related
    private Rotation rotation;
    private ShapeType shapeType;
    private IntVector tileCoords;
    private Block[] blocks;

    // visual representation
    private double rotationAngle;
    private DoubleVector coords;
    private Entity parentEntity;
    private double opacity = 1.0;
    private double scale = 1.0;

    /**
     * @param   blockColors colors of the solid blocks of the shape.
     *          They are specified in a row-major order in terms of
     *          the boolean[][] matrix that corresponds to the initial
     *          rotation of this shape type.
     */
    public Shape(GameState gameState, ShapeType shapeType,
            Rotation rotation, IntVector tileCoords,
            BlockColor[] blockColors, Entity parentEntity) {
        this.gameState = gameState;
        this.shapeType = shapeType;
        this.rotation = rotation;
        this.tileCoords = tileCoords;
        this.parentEntity = parentEntity;

        final int blockWidth = gameState.getBlockWidth();
        coords = tileCoords.times(blockWidth).toDouble();
        rotationAngle = rotation.getAngle();

        DoubleVector rotationPivot
                = tileCoords.add(shapeType.getRotationPivot());

        ArrayList<Block> blocks = new ArrayList<>();

        final int frameSize = shapeType.getFrameSize();
        int solidBlockIndex = 0;
        for (int x = 0; x < frameSize; x++) {
            for (int y = 0; y < frameSize; y++) {
                if (shapeType.isSolid(x, y, rotation)) {
                    blocks.add(new Block(gameState, tileCoords.add(x, y),
                        rotationPivot, blockColors[solidBlockIndex], this,
                        new DoubleVector(x * blockWidth, y * blockWidth)));
                    solidBlockIndex++;
                }
            }
        }
        this.blocks = blocks.toArray(new Block[0]);
    }

    /**
     * Create a shape with all its solid blocks painted with {@code color}
     * color.
     */
    public Shape(GameState gameState, ShapeType shapeType,
            Rotation rotation, IntVector tileCoords, BlockColor blockColor,
            Entity parentEntity) {
        this(gameState, shapeType, rotation, tileCoords,
                generateColorsArray(shapeType, blockColor), parentEntity);
    }

    /**
     * Retrieve the block entities of which the shape consists.
     */
    public Block[] getBlocks(GameField gameField) {
        Block[] blocksCopy = new Block[blocks.length];
        final int blockWidth = gameState.getBlockWidth();
        for (int i = 0; i < blocksCopy.length; i++) {
            DoubleVector newRefCoords
                    = blocks[i].getTileCoords().times(blockWidth)
                    .toDouble();
            blocksCopy[i] = new Block(blocks[i], gameField, newRefCoords);
        }
        return blocksCopy;
    }


    public void rotate(Rotation rotationDirection) {
        if (rotationDirection != Rotation.RIGHT
                && rotationDirection != Rotation.LEFT) {
            throw new IllegalArgumentException(String.format(
                "Rotation direction is expected to be either left or"
                + " right. Got: %s", rotationDirection));
        }
        for (Block block : blocks) {
            block.rotate(rotationDirection);
        }
        this.rotation = rotation.add(rotationDirection);
    }

    /**
     * Returns an array of wall kicks (shifts) that can be
     * applied after the clockwise rotation of this shape.
     */
    public IntVector[] getRightWallKicks() {
        return shapeType.getRightWallKicks(this.rotation);
    }

    public IntVector[] getLeftWallKicks() {
        return shapeType.getLeftWallKicks(this.rotation);
    }

    public Rotation getRotation() {
        return rotation;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public void startDropAnimation(int duration, AnimationEndHandler callback) {
        final int blockWidth = gameState.getBlockWidth();
        HVLinearAnimation animation = HVLinearAnimation.getVerticalAnimation(
                coords.getY(), tileCoords.getY() * blockWidth, duration,
                blockWidth);
        gameState.getAnimationManager().addAnimation(this,
                ActiveShapeAnimationType.DROP,
                animation, callback);
    }

    public void startDropAnimation(int duration) {
        startDropAnimation(duration, null);
    }

    public void startUserControlAnimation(int duration) {
        final int blockWidth = gameState.getBlockWidth();
        HVLinearAnimation animation = HVLinearAnimation.getHorizontalAnimation(
                coords.getX(), tileCoords.getX() * blockWidth,
                duration, blockWidth);
        gameState.getAnimationManager().addAnimation(this,
                ActiveShapeAnimationType.LEFT_RIGHT,
                animation);
    }

    public void startRotationAnimation(double angleShift, boolean isClockwise,
            int duration) {
        RotationAnimation animation = new RotationAnimation(rotationAngle,
                rotationAngle + angleShift, isClockwise, duration, Math.PI / 2);
        gameState.getAnimationManager().addAnimation(this,
                ActiveShapeAnimationType.ROTATION,
                animation);
    }

    public void startWallKickAnimation(int duration,
            AnimationEndHandler callback) {
        final int blockWidth = gameState.getBlockWidth();
        MoveAnimation animation = new MoveAnimation(coords,
                tileCoords.times(blockWidth).toDouble(), duration, blockWidth);
        gameState.getAnimationManager().addAnimation(this,
                ActiveShapeAnimationType.WALL_KICK, animation, callback);
    }

    @Override
    public IntVector getTileCoords() {
        return tileCoords;
    }

    @Override
    public void tileMove(IntVector newTileCoords) {
        IntVector shift = newTileCoords.subtract(tileCoords);
        tileCoords = newTileCoords;
        for (Block block : blocks) {
            block.tileShift(shift);
        }
    }

    @Override
    public boolean checkCollision(IntVector collisionTile) {
        final int frameSize = shapeType.getFrameSize();
        // coordinates of the collision relatively to the tile coordinates
        // of this shape
        final IntVector relativeCollisionTile
            = collisionTile.subtract(tileCoords);
        final int x = relativeCollisionTile.getX();
        final int y = relativeCollisionTile.getY();

        if (x >= 0 && y >= 0 && x < frameSize && y < frameSize) {
            return shapeType.isSolid(x, y, rotation);
        }
        return false;
    }

    @Override
    public void tick() {
        for (Block block : blocks) {
            block.tick();
        }
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        for (Block block : blocks) {
            block.setOpacity(opacity);
            block.setScale(scale);
            block.render(g, interpolation);
        }
    }

    @Override
    public DoubleVector[] getVertices() {
        List<DoubleVector> points = new ArrayList<>();
        for (Block block : blocks) {
            points.addAll(Arrays.asList(block.getVertices()));
        }
        return points.toArray(new DoubleVector[0]);
    }

    @Override
    public Entity getParentEntity() {
        return parentEntity;
    }

    @Override
    public Transform getLocalTransform() {
        final int blockWidth = gameState.getBlockWidth();

        DoubleVector rotationPivot
            = coords.add(shapeType.getRotationPivot().times(blockWidth));
        return new Transform(coords)
            .combine(Transform.getRotation(rotationAngle, rotationPivot));
    }

    @Override
    public boolean needsAdditionalTransform() {
        return true;
    }

    @Override
    public void setCoords(DoubleVector newCoords) {
        coords = newCoords;
    }

    @Override
    public DoubleVector getCoords() {
        return coords;
    }

    @Override
    public void setRotationAngle(double newRotationAngle) {
        rotationAngle = newRotationAngle;
    }

    @Override
    public double getRotationAngle() {
        return rotationAngle;
    }

    @Override
    public void setOpacity(double newOpacity) {
        if (newOpacity < 0 || newOpacity > 1.0) {
            throw new IllegalArgumentException(String.format(
                    "The value of the opacity must lie within the"
                     + " [0, 1] interval. Got: newOpacity = %f.",
                     newOpacity));
        }
        opacity = newOpacity;
    }

    @Override
    public void setScale(double newScale) {
        if (newScale < 0) {
            throw new IllegalArgumentException(String.format(
                    "The scale value must be non-negative."
                    + " Got: newScale = %f.", newScale));
        }
        scale = newScale;
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public DoubleVector[] getConvexHull() {
        final int blockWidth = gameState.getBlockWidth();

        DoubleVector[] convexHull = shapeType.getConvexHull();
        for (int i = 0; i < convexHull.length; i++) {
            convexHull[i] = getLocalTransform()
                    .apply(convexHull[i].times(blockWidth));
        }
        return convexHull;
    }

    @Override
    public String toString() {
        return String.format("[%s shape type, %s rotated, coords: %s, "
                + " blocks: %s]", shapeType, rotation,
                tileCoords, Arrays.toString(blocks));
    }

    /**
     * Checks, if the shape with the specified parameters fits into
     * the specified tile field.
     * @param   excludedObject this object is excluded when checking for
     *          collisions with other tile field objects. Set this
     *          argument to {@code null} if no object needs to be
     *          excluded.
     */
    public static boolean fits(Shape excludedObject, ShapeType shapeType,
            IntVector tileCoords, Rotation rotation, TileField tileField) {
        final int frameSize = shapeType.getFrameSize();
        // (x1, y1) - upper-left boundary (x2, y2) - bottom-right boundary
        // (x1, y1) is included into the boundary, (x2, y2) is excluded
        final int x1 = tileField.getStartingIndex().getX();
        final int y1 = tileField.getStartingIndex().getY();
        final int x2 = x1 + tileField.getWidthInBlocks();
        final int y2 = y1 + tileField.getHeightInBlocks();

        for (int x = 0; x < frameSize; x++) {
            for (int y = 0; y < frameSize; y++) {
                if (shapeType.isSolid(x, y, rotation)) {
                    IntVector blockCoords = tileCoords.add(x, y);
                    // check, if the object fits into the game field
                    // boundaries
                    if (blockCoords.getX() < x1 || blockCoords.getY() < y1
                            || blockCoords.getX() >= x2
                            || blockCoords.getY() >= y2) {
                        return false;
                    }
                    // check, if it interferes with any other objects
                    // on the field
                    for (TileFieldObject object : tileField.getObjects()) {
                        if (object != excludedObject
                                && object.checkCollision(blockCoords)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean fits(ShapeType shapeType, IntVector tileCoords,
            Rotation rotation, TileField tileField) {
        return fits(null, shapeType, tileCoords, rotation, tileField);
    }

    /**
     * Generate a colors array for the given shape type where each
     * block is colored in `blockColor`.
     */
    public static BlockColor[] generateColorsArray(ShapeType shapeType,
            BlockColor blockColor) {
        final int solidBlocksCount = shapeType.getSolidBlocksNumber();
        BlockColor[] blockColors = new BlockColor[solidBlocksCount];
        for (int i = 0; i < solidBlocksCount; i++) {
            blockColors[i] = blockColor;
        }
        return blockColors;
    }
}
