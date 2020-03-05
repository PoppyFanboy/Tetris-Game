package poppyfanboy.tetrisgame.entities;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

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
public class Block extends Entity implements TileFieldObject {
    private GameState gameState;

    private IntVector tileCoords;
    private DoubleVector tileRotationPivot;
    private Rotation rotation;
    private BlockColor blockColor;

    private GameField gameField;
    private Entity parentEntity;

    // visual representation
    private DoubleVector refCoords;

    /**
     * Creates a block entity at the specified position on the game field.
     */
    public Block(GameState gameState, IntVector tileCoords,
            DoubleVector tileRotationPivot, BlockColor blockColor,
            Entity parentEntity, DoubleVector refCoords,
            GameField gameField) {
        this.gameState = gameState;
        this.gameField = gameField;
        this.parentEntity = parentEntity;
        // tile-field related
        this.tileCoords = tileCoords;
        this.tileRotationPivot = tileRotationPivot;
        this.blockColor = blockColor;
        // visual representation related
        this.refCoords = refCoords;

        rotation = Rotation.INITIAL;
    }

    @Override
    public void tileMove(IntVector newCoords) {
        IntVector shiftDirection = newCoords.subtract(tileCoords);
        tileRotationPivot = tileRotationPivot.add(shiftDirection);

        tileCoords = newCoords;
    }

    @Override
    public void tileShift(IntVector shiftDirection) {
        tileRotationPivot = tileRotationPivot.add(shiftDirection);

        tileCoords = tileCoords.add(shiftDirection);
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
    public Transform getGlobalTransform() {
        if (parentEntity != null) {
            return getLocalTransform()
                    .combine(parentEntity.getGlobalTransform());
        } else {
            return getLocalTransform();
        }
    }

    @Override
    public Transform getLocalTransform() {
        return new Transform(refCoords);
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        // draw blocks as they are on the tile field
        /*final int blockWidth = gameState.getBlockWidth();
        g.setColor(BlockColor.BLUE.getColor());
        g.setStroke(new BasicStroke(2));
        g.drawRect(tileCoords.getX() * blockWidth + 20,
                tileCoords.getY() * blockWidth + 20,
                blockWidth, blockWidth);*/

        double rotationAngle
                = getGlobalTransform().getRotation().getAngle();

        AffineTransform oldTransform = g.getTransform();
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

        g.drawImage(progress < 0.5 ? left : right,
                0, 0, null);

        float alpha = (float) (progress < 0.5 ? progress : 1 - progress);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite
                .getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(progress < 0.5 ? right : left,
                0, 0, null);

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
}
