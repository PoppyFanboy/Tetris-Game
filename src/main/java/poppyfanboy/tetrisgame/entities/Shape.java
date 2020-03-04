package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import poppyfanboy.tetrisgame.entities.shapetypes.ShapeType;
import poppyfanboy.tetrisgame.graphics.animation.MoveAnimation;
import poppyfanboy.tetrisgame.graphics.animation.RotateAnimation;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;
import poppyfanboy.tetrisgame.util.Util;

import poppyfanboy.tetrisgame.graphics.animation.Animated;

import static java.lang.Math.abs;

/**
 * In a nutshell this is just a bunch of glued blocks, that can be rotated
 * and moved around the game field.
 */
public class Shape extends Entity implements TileFieldObject, Animated {
    private GameState gameState;

    private ShapeType shapeType;
    // current rotation position of the shape (in terms of the tile field)
    private Rotation rotation;
    private IntVector tileCoords;
    private Block[] blocks;

    private GameField gameField;

    // visual representation
    private double rotationAngle;
    private DoubleVector coords;


    // each new animation cancels the previous one (the exception is
    // two consecutive rotation animations in the same direction),
    // thus there is no need for the list of present animations so far
    private MoveAnimation currentMoveAnimation;
    private RotateAnimation currentRotationAnimation;

    /**
     * @param   blockColors colors of the solid blocks of the shape.
     *          They are specified in a row-major order in terms of
     *          the boolean[][] matrix that corresponds to the initial
     *          rotation of this shape type.
     */
    public Shape(GameState gameState, ShapeType shapeType,
            Rotation rotation, IntVector tileCoords,
            BlockColor[] blockColors, GameField gameField) {
        this.gameState = gameState;
        this.shapeType = shapeType;
        this.rotation = rotation;
        this.tileCoords = tileCoords;
        this.gameField = gameField;

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
                        new DoubleVector(x * blockWidth, y * blockWidth),
                        gameField));
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
            GameField gameField) {
        this(gameState, shapeType, rotation, tileCoords,
                generateColorsArray(shapeType, blockColor), gameField);
    }

    /**
     * Generate a colors array for the given shape type where each
     * block is colored in `blockColor`.
     */
    private static BlockColor[] generateColorsArray(ShapeType shapeType,
            BlockColor blockColor) {
        final int solidBlocksCount = shapeType.getSolidBlocksNumber();
        BlockColor[] blockColors = new BlockColor[solidBlocksCount];
        for (int i = 0; i < solidBlocksCount; i++) {
            blockColors[i] = blockColor;
        }
        return blockColors;
    }

    /**
     * Retrieve the block entities of which the shape consists.
     */
    public Block[] getBlocks() {
        return blocks;
    }

    public void rotate(Rotation rotationDirection) {
        if (rotationDirection != Rotation.RIGHT
                && rotationDirection != Rotation.LEFT) {
            throw new IllegalArgumentException(String.format(
                "Rotation direction is expected to be either left or"
                + " right. Got: %s", rotationDirection));
        }
        boolean isClockwise = rotationDirection == Rotation.RIGHT;
        double newRotationAngle;

        if (currentRotationAnimation == null) {
            rotationAngle = rotation.getAngle();
            newRotationAngle = rotationAngle
                    + (isClockwise ? Math.PI / 2 : -Math.PI / 2);
        } else {
            if (currentRotationAnimation.isClockwise() != isClockwise) {
                double angleShift = Rotation.normalizeAngle(
                        rotation.add(rotationDirection).getAngle()
                                - rotationAngle);
                if (!isClockwise && angleShift > 0) {
                    angleShift -= 2 * Math.PI;
                }
                if (isClockwise && angleShift < 0) {
                    angleShift += 2 * Math.PI;
                }
                newRotationAngle = rotationAngle + angleShift;
            } else {
                newRotationAngle
                        = currentRotationAnimation.getEndAngle()
                        + (isClockwise ? Math.PI / 2 : -Math.PI / 2);
            }
        }
        addRotateAnimation(new RotateAnimation(rotationAngle,
            newRotationAngle, gameState.getAnimationDuration(),
            Math.PI / 2, isClockwise));

        for (Block block : blocks) {
            block.rotate(rotationDirection);
        }
        this.rotation = rotation.add(rotationDirection);
    }

    /**
     * Counter-clockwise rotation.
     */
    public void rotateLeft() {
        rotate(Rotation.LEFT);
    }

    /**
     * Clockwise rotation.
     */
    public void rotateRight() {
        rotate(Rotation.RIGHT);
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

    /**
     * Shifts the shape in the specified direction, puts it into the
     * specified rotation and checks, if it fits into the game field with
     * the specified width and height.
     * (Does not mutate the shape instance itself.)
     */
    public boolean shiftedBoundsCheck(IntVector shiftDirection,
            Rotation rotation, int fieldWidth, int fieldHeight) {
        final int frameSize = shapeType.getFrameSize();
        for (int x = 0; x < frameSize; x++) {
            for (int y = 0; y < frameSize; y++) {
                if (shapeType.isSolid(x, y, rotation)) {
                    IntVector shiftedTile
                        = tileCoords.add(shiftDirection).add(x, y);
                    if (shiftedTile.getY() < 0 || shiftedTile.getX() < 0
                            || shiftedTile.getY() >= fieldHeight
                            || shiftedTile.getX() >= fieldWidth) {
                        return false;
                    }
                }
            }
        }
        return true;
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

        final int blockWidth = gameState.getBlockWidth();
        this.addMoveAnimation(new MoveAnimation(coords,
            tileCoords.times(blockWidth).toDouble(),
            gameState.getAnimationDuration(), blockWidth));
    }

    @Override
    public void tileShift(IntVector shiftDirection) {
        tileCoords = tileCoords.add(shiftDirection);
        for (Block block : blocks) {
            block.tileShift(shiftDirection);
        }

        final int blockWidth = gameState.getBlockWidth();
        this.addMoveAnimation(new MoveAnimation(coords,
            tileCoords.times(blockWidth).toDouble(),
            gameState.getAnimationDuration(), blockWidth));
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

    /**
     * Shifts the shape (without mutating the shape instance itself),
     * puts it into the specified rotation and checks, if it collides
     * with the specified (row, col) point.
     */
    public boolean checkShiftedCollision(IntVector collisionTile,
            IntVector shapeShift, Rotation shapeRotation) {
        final int frameSize = shapeType.getFrameSize();
        // coordinates of the collision relatively to the tile coordinates
        // of this shape
        final IntVector relativeCollisionTile
            = collisionTile.add(shapeShift).subtract(tileCoords);
        final int x = relativeCollisionTile.getX();
        final int y = relativeCollisionTile.getY();

        if (x >= 0 && y >= 0 && x < frameSize && y < frameSize) {
            return shapeType.isSolid(x, y, shapeRotation);
        }
        return false;
    }

    @Override
    public void tick() {
        if (currentRotationAnimation != null) {
            if (currentRotationAnimation.finished()) {
                currentRotationAnimation = null;
            } else {
                currentRotationAnimation.tick();
                currentRotationAnimation.perform(this);
            }
        }
        if (currentMoveAnimation != null) {
            if (currentMoveAnimation.finished()) {
                currentMoveAnimation = null;
            } else {
                currentMoveAnimation.tick();
                currentMoveAnimation.perform(this);
            }
        }
        for (Block block : blocks) {
            block.tick();
        }
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        // interpolate shape position between the actual game ticks
        if (currentMoveAnimation != null
                && !currentMoveAnimation.finished()) {
            currentMoveAnimation.perform(this, interpolation);
        }
        if (currentRotationAnimation != null
                && !currentRotationAnimation.finished()) {
            currentRotationAnimation.perform(this, interpolation);
        }

        for (Block block : blocks) {
            block.render(g, interpolation);
        }
        // render convex hull
        /*DoubleVector[] convexHull = this.getConvexHull();
        g.setColor(BlockColor.BLUE.getColor());
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < convexHull.length; i++) {
            convexHull[i] = convexHull[i].add(fitDX, fitDY);
        }
        g.drawPolygon(DoubleVector.getIntX(convexHull),
            DoubleVector.getIntY(convexHull), convexHull.length);*/
    }

    @Override
    public DoubleVector[] getVertices() {
        List<DoubleVector> points = new ArrayList<>();
        for (Block block : getBlocks()) {
            points.addAll(Arrays.asList(block.getVertices()));
        }
        return points.toArray(new DoubleVector[0]);
    }

    @Override
    public Entity getParentEntity() {
        return gameField;
    }

    @Override
    public Transform getGlobalTransform() {
        if (gameField != null) {
            final int blockWidth = gameState.getBlockWidth();
            DoubleVector[] convexHull = shapeType.getConvexHull();
            Transform globalTransform = getLocalTransform()
                        .combine(gameField.getGlobalTransform());
            for (int i = 0; i < convexHull.length; i++) {
                convexHull[i] = globalTransform
                        .apply(convexHull[i].times(blockWidth));
            }

            int width = gameField.getWidthInBlocks() * blockWidth;
            int height = gameField.getHeightInBlocks() * blockWidth;
            // (x1, y1) - upper left corner of the game field
            // (x2, y2) - bottom right corner of the game field
            double x1
                = gameField.getGlobalTransform().getTranslation().getX();
            double y1
                = gameField.getGlobalTransform().getTranslation().getY();
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

            return globalTransform.combine(
                    new Transform(new DoubleVector(fitDX, fitDY)));
        } else {
            return getLocalTransform();
        }
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
    public void setCoords(DoubleVector newCoords) {
        coords = newCoords;
    }

    @Override
    public void setRotationAngle(double newRotationAngle) {
        rotationAngle = newRotationAngle;
    }

    @Override
    public DoubleVector[] getConvexHull() {
        final int blockWidth = gameState.getBlockWidth();
        Transform globalTransform = getGlobalTransform();

        DoubleVector[] convexHull = shapeType.getConvexHull();
        for (int i = 0; i < convexHull.length; i++) {
            convexHull[i]
                = globalTransform.apply(convexHull[i].times(blockWidth));
        }
        return convexHull;
    }

    @Override
    public void addMoveAnimation(MoveAnimation moveAnimation) {
        if (moveAnimation != null) {
            currentMoveAnimation = moveAnimation;
        }
    }

    @Override
    public void addRotateAnimation(RotateAnimation rotateAnimation) {
        if (rotateAnimation != null) {
            currentRotationAnimation = rotateAnimation;
        }
    }

    @SafeVarargs
    public static <E extends Enum<? extends ShapeType>> Shape
        getRandomShapeEvenlyColored(Random random, GameState gameState,
            Rotation rotation, IntVector tileCoords, GameField gameField,
            Class<? extends E>... shapeTypes) {
        ShapeType randomType
                = (ShapeType) Util.getRandomInstance(random, shapeTypes);
        BlockColor randomColor
                = Util.getRandomInstance(random, BlockColor.class);
        return new Shape(gameState, randomType, rotation, tileCoords,
                randomColor, gameField);
    }

    @SafeVarargs
    public static <E extends Enum<? extends ShapeType>> Shape
        getRandomShapeRandomlyColored(Random random, GameState gameState,
            Rotation rotation, IntVector tileCoords, GameField gameField,
            Class<? extends E>... shapeTypes) {
        ShapeType randomType
                = (ShapeType) Util.getRandomInstance(random, shapeTypes);
        // count solid blocks
        int solidBlocksCount = randomType.getSolidBlocksNumber();
        BlockColor[] randomColors = new BlockColor[solidBlocksCount];
        for (int i = 0; i < randomColors.length; i++) {
            randomColors[i]
                    = Util.getRandomInstance(random, BlockColor.class);
        }
        return new Shape(gameState, randomType, rotation, tileCoords,
                randomColors, gameField);
    }
}
