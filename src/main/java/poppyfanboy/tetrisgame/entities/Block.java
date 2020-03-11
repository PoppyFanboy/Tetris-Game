package poppyfanboy.tetrisgame.entities;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import poppyfanboy.tetrisgame.graphics.animation2D.AcceleratedMoveAnimation;
import poppyfanboy.tetrisgame.graphics.animation2D.Animated2D;
import poppyfanboy.tetrisgame.graphics.animation2D.BlockBreakAnimation;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.DoubleVector;

import static poppyfanboy.tetrisgame.util.DoubleVector.dVect;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;

/**
 * Represents a single solid block on the game field.
 */
public class Block extends Entity implements TileFieldObject, Animated2D {
    private GameState gameState;

    private IntVector tileCoords;
    private DoubleVector tileRotationPivot;
    private Rotation rotation;
    private BlockColor blockColor;

    private Entity parentEntity;
    private DoubleVector coords;
    private double rotationAngle = 0;
    private double scale = 1.0;
    private double opacity = 1.0;

    /**
     * Creates a block entity at the specified position on the game field.
     */
    public Block(GameState gameState, IntVector tileCoords,
            DoubleVector tileRotationPivot, BlockColor blockColor,
            Entity parentEntity, DoubleVector coords) {
        this.gameState = gameState;
        this.parentEntity = parentEntity;
        this.tileCoords = tileCoords;
        this.tileRotationPivot = tileRotationPivot;
        this.blockColor = blockColor;
        this.coords = coords;
        rotation = Rotation.INITIAL;
    }

    public Block(Block block, Entity parentEntity,
            DoubleVector coords) {
        this(block.gameState, block.tileCoords, block.tileRotationPivot,
                block.blockColor, parentEntity, coords);
    }

    @Override
    public void tileMove(IntVector newCoords) {
        IntVector shiftDirection = newCoords.subtract(tileCoords);
        tileRotationPivot = tileRotationPivot.add(shiftDirection);
        tileCoords = newCoords;
    }

    public AcceleratedMoveAnimation createDropAnimation() {
        final int blockWidth = gameState.getBlockWidth();

        return new AcceleratedMoveAnimation(coords,
                tileCoords.times(blockWidth).toDouble(), 0.0);
    }

    public BlockBreakAnimation createBlockBreakAnimation(int duration) {
        return new BlockBreakAnimation(rotationAngle, scale, duration);
    }

    public void rotate(Rotation rotationDirection) {
        if (rotationDirection != Rotation.RIGHT
                && rotationDirection != Rotation.LEFT) {
            throw new IllegalArgumentException(String.format(
                "Rotation direction is expected to be either left or"
                + " right. Got: %s", rotationDirection));
        }
        DoubleVector rotatedCoords
                = tileCoords.add(0.5, 0.5).subtract(tileRotationPivot)
                .rotate(rotationDirection).add(tileRotationPivot)
                .add(-0.5, -0.5);
        tileCoords = new IntVector((int) Math.round(rotatedCoords.getX()),
                (int) Math.round(rotatedCoords.getY()));
        this.rotation = rotation.add(rotationDirection);
    }

    @Override
    public void tick() {
    }

    @Override
    public Entity getParentEntity() {
        return parentEntity;
    }

    @Override
    public Transform getLocalTransform() {
        final int blockWidth = gameState.getBlockWidth();
        DoubleVector rotationPivot = coords.add(
                new DoubleVector(blockWidth / 2.0, blockWidth / 2.0));

        return new Transform(coords)
            .combine(Transform.getRotation(rotationAngle, rotationPivot));
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        final int blockWidth = gameState.getBlockWidth();
        // draw blocks as they are on the tile field
        /*
        g.setColor(BlockColor.BLUE.getColor());
        g.setStroke(new BasicStroke(2));
        g.fillRect(tileCoords.getX() * blockWidth + 20,
                tileCoords.getY() * blockWidth + 20,
                blockWidth, blockWidth);*/

        double rotationAngle
                = getGlobalTransform().getRotation().getAngle();

        AffineTransform oldTransform = g.getTransform();
        Composite oldComposite = g.getComposite();

        Transform globalTransform = getGlobalTransform();
        AffineTransform transform = new AffineTransform(
            globalTransform.matrix(0, 0), globalTransform.matrix(1, 0),
            globalTransform.matrix(0, 1), globalTransform.matrix(1, 1),
            globalTransform.matrix(0, 2), globalTransform.matrix(1, 2));
        g.setTransform(transform);

        Assets assets = gameState.getAssets();
        BufferedImage left
                = assets.getColoredBlockLeft(rotationAngle, blockColor);
        BufferedImage right
                = assets.getColoredBlockRight(rotationAngle, blockColor);
        int n = assets.getLightingSamplesCount();
        double progress = (n * (Rotation.normalizeAngle(rotationAngle)
                + Math.PI) / (2 * Math.PI)) % 1;

        g.setComposite(AlphaComposite
                .getInstance(AlphaComposite.SRC_OVER, (float) opacity));
        if (scale == 1.0) {
            g.drawImage(progress < 0.5 ? left : right,
                    0, 0, null);
        } else {
            g.drawImage(progress < 0.5 ? left : right,
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * scale),
                    (int) (blockWidth * scale), null);
        }

        float alpha = (float) (
                (progress < 0.5 ? progress : 1 - progress)
                * opacity);

        g.setComposite(AlphaComposite
                .getInstance(AlphaComposite.SRC_OVER, alpha));
        if (scale == 1.0) {
            g.drawImage(progress < 0.5 ? right : left,
                    0, 0, null);
        } else {
            g.drawImage(progress < 0.5 ? right : left,
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * scale),
                    (int) (blockWidth * scale), null);
        }

        g.setComposite(oldComposite);
        g.setTransform(oldTransform);

        // render convex hull
        /*DoubleVector[] convexHull = this.getConvexHull();
        g.setColor(BlockColor.BLUE.getColor());
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(DoubleVector.getIntX(convexHull),
                DoubleVector.getIntY(convexHull), convexHull.length);*/
    }

    @Override
    public DoubleVector[] getVertices() {
        final int blockWidth = gameState.getBlockWidth();
        return getGlobalTransform().apply(new DoubleVector[] {
            dVect(0, 0), dVect(0, blockWidth),
            dVect(blockWidth, 0), dVect(blockWidth, blockWidth)});
    }

    @Override
    public boolean checkCollision(IntVector collisionPoint) {
        return tileCoords.equals(collisionPoint);
    }

    @Override
    public IntVector getTileCoords() {
        return tileCoords;
    }

    @Override
    public String toString() {
        return tileCoords.toString();
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
}
