package poppyfanboy.tetrisgame.entities.shapetypes;

import java.util.ArrayList;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Rotation;

/**
 * A general interface for any shapes in the tetris game. An object of this
 * type should be made immutable (ideally this should be an enumeration
 * of the possible shapes).
 */
public interface ShapeType {
    /**
     * Tells, if the specified point with the coordinates relative to
     * the upper-left corner of the box of size {@code getFrameSize()},
     * in which the shape is contained, contains a solid block.
     */
    boolean isSolid(int x, int y);

    boolean isSolid(int x, int y, Rotation rotation);

    /**
     * Size of the {@code boolean[][]} array returned by the
     * {@code getShape} methods.
     */
    int getFrameSize();

    /**
     * Returns an array of so-called "wall-kicks". These are just the
     * offsets that are applied to the tetris shape when it does not fit
     * into the game field after the clockwise rotation. The wallkicks
     * for counter-clockwise rotation are just like these ones, but
     * negated.
     *
     * @param   initialRotation is the position from which the shape is
     *          rotated clockwise.
     */
    IntVector[] getRightWallKicks(Rotation initialRotation);

    /**
     * Does the same thing as the {@code getRightWallKicks} method but
     * for the counter-clockwise rotation.
     */
    IntVector[] getLeftWallKicks(Rotation initialRotation);

    /**
     * Returns a point positioned in the center of the shape relative
     * to the upper-left corner of the frame in which the shape is fitted.
     */
    DoubleVector getRotationPivot();

    DoubleVector[] getConvexHull();

    /**
     * Shapes might be loosely fitted into the frame, these methods return
     * the coordinates (in terms of the frame) of the smallest AABB surrounding
     * the shape.
     */
    default IntVector getPreciseAABBMin() {
        return new IntVector(0, 0);
    }

    default IntVector getPreciseAABBMax() {
        int frameSize = getFrameSize();
        return new IntVector(frameSize, frameSize);
    }

    default int getSolidBlocksNumber() {
        final int frameSize = getFrameSize();
        int solidBlocksCount = 0;
        for (int x = 0; x < frameSize; x++) {
            for (int y = 0; y < frameSize; y++) {
                if (isSolid(x, y)) {
                    solidBlocksCount++;
                }
            }
        }
        return solidBlocksCount;
    }

    /**
     * Returns a set normalized coordinates of the convex hull of the
     * specified shape. This method is not supposed to be used
     * anywhere except for the static initializer blocks of the
     * implementations of the {@code ShapeType} interface.
     */
    static DoubleVector[] getConvexHull(ShapeType shapeType) {
        ArrayList<DoubleVector> points = new ArrayList<>();
        final int frameSize = shapeType.getFrameSize();
        for (int x = 0; x < frameSize; x++) {
            for (int y = 0; y < frameSize; y++) {
                if (shapeType.isSolid(x, y)) {
                    points.add(new DoubleVector(x, y));
                    points.add(new DoubleVector(x + 1.0, y + 1.0));
                    points.add(new DoubleVector(x + 1.0, y));
                    points.add(new DoubleVector(x, y + 1.0));
                }
            }
        }
        return DoubleVector
            .getConvexHull(points.toArray(new DoubleVector[0]));
    }
}