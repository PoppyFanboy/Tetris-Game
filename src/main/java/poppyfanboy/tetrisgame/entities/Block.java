package poppyfanboy.tetrisgame.entities;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.graphics.animation2D.AcceleratedMoveAnimation;
import poppyfanboy.tetrisgame.graphics.animation2D.Animated2D;
import poppyfanboy.tetrisgame.graphics.animation2D.BlockBreakAnimation;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;

import static poppyfanboy.tetrisgame.util.DoubleVector.dVect;

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
    private double brightness = 0.0;

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

    public void startDropAnimation() {
        AcceleratedMoveAnimation animation = new AcceleratedMoveAnimation(
                coords, tileCoords.toDouble(), 0.0);
        gameState.getAnimationManager().addAnimation(this,
                LockedBlockAnimationType.DROP, animation);
    }

    public void startBreakAnimation(int duration) {
        BlockBreakAnimation animation
                = new BlockBreakAnimation(rotationAngle, scale, duration);
        gameState.getAnimationManager().addAnimation(this,
                LockedBlockAnimationType.BREAK,
                animation);
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
        DoubleVector rotationPivot = coords.add(new DoubleVector(0.5, 0.5));
        return new Transform(coords)
                .combine(Transform.getRotation(rotationAngle, rotationPivot));
    }

    @Override
    public void render(Graphics2D gOriginal, double interpolation) {
        final int blockWidth = gameState.getResolution().getBlockWidth();
        // draw blocks as they are on the tile field
        /*g.setColor(BlockColor.BLUE.getColor());
        g.setStroke(new BasicStroke(2));
        g.fillRect(tileCoords.getX() * blockWidth + 20,
                tileCoords.getY() * blockWidth + 20,
                blockWidth, blockWidth);*/

        final DoubleVector lightSourceCoords = new DoubleVector(22, 10);
        double rotationAngle = getGlobalTransform().tScale(blockWidth)
                .getRotation().getAngle();
        DoubleVector lightVector = lightSourceCoords.subtract(
                getGlobalTransform().apply(new DoubleVector(0.5, 0.5)))
                .normalize();
        double lightAngle = Math.atan2(lightVector.getY(), lightVector.getX());

        Graphics2D g = (Graphics2D) gOriginal.create();
        g.setTransform(getGlobalTransform().tScale(blockWidth).getTransform());
        Assets assets = gameState.getAssets();

        BufferedImage sprite = assets.getColoredBlockLeft(
                -lightAngle + rotationAngle, blockColor);

        int n = Assets.LIGHTING_SAMPLES_COUNT;
        double progress = (n * (Rotation.normalizeAngle(rotationAngle - lightAngle) + Math.PI) / (2 * Math.PI)) % 1;

        g.setComposite(AlphaComposite
                .getInstance(AlphaComposite.SRC_OVER, (float) opacity));
        if (scale == 1.0) {
            g.drawImage(sprite, 0, 0, null);
        } else {
            g.drawImage(sprite,
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * scale),
                    (int) (blockWidth * scale), null);
        }

        if (brightness != 0) {
            g.setComposite(AlphaComposite
                    .getInstance(AlphaComposite.SRC_OVER, (float) brightness));
            g.drawImage(assets.getGhostBlock(),
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * (1 - scale) / 2),
                    (int) (blockWidth * scale),
                    (int) (blockWidth * scale), null);
        }

        // render convex hull
        /*DoubleVector[] convexHull = this.getConvexHull();
        g.setColor(BlockColor.BLUE.getColor());
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(DoubleVector.getIntX(convexHull),
                DoubleVector.getIntY(convexHull), convexHull.length);*/

        g.dispose();
    }

    @Override
    public DoubleVector[] getVertices() {
        return getLocalTransform().apply(new DoubleVector[] {
            dVect(0, 0), dVect(0, 1.0),
            dVect(1.0, 0), dVect(1.0, 1.0)});
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
        newOpacity =  Math.min(Math.max(newOpacity, 0), 1);
        opacity = newOpacity;
    }

    @Override
    public double getOpacity() {
        return opacity;
    }

    @Override
    public void setBrightness(double newBrightness) {
        newBrightness = Math.min(Math.max(newBrightness, 0), 1);
        brightness = newBrightness;
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
    public String toString() {
        return String.format("[ %s colored block, coords: %s ]",
                blockColor, tileCoords.toString());
    }
}
