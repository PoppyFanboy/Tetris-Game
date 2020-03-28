package poppyfanboy.tetrisgame.entities.shapetypes;

import java.util.Arrays;

import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.DoubleVector;
import static poppyfanboy.tetrisgame.util.IntVector.iVect;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Util;

/**
 * Represents a shape from the tetris game built of four blocks.
 */
public enum TetrisShapeType implements ShapeType {
    I_SHAPE, L_SHAPE, L_SHAPE_MIRRORED, CUBE_SHAPE,
    S_SHAPE, T_SHAPE, Z_SHAPE;

    public static final int FRAME_SIZE = 4;

    private static String[] shapes = {
        ". . . .", ". . . .", ". . . .", ". . . .",
        "* * * *", ". * . .", ". . . *", ". * * .",
        ". . . .", ". * * *", ". * * *", ". * * .",
        ". . . .", ". . . .", ". . . .", ". . . .",

        ". . . .", ". . . .", ". . . .",
        ". . * *", ". . * .", ". * * .",
        ". * * .", ". * * *", ". . * *",
        ". . . .", ". . . .", ". . . ."
    };

    // The pivot of rotation is marked with an 'x'. All the other
    // symbols do not matter, they are placed there so that it
    // would be easier to figure out where the pivot is placed relatively
    // to the shape.
    // 
    // In case the pivot does not match either the solid or empty
    // blocks of the shape, the x's are placed around the pivot.
    // (So you can use just 2 diagonal x's in case of the cube shape,
    // surrounding the pivot with 4 x's is also OK.)
    private static String[] pivots = {
        ". . . .", ". . . .", ". . . .", ". . . .",
        "* x * *", ". * . .", ". . . *", ". * x .",
        ". . x .", ". * x *", ". * x *", ". x * .",
        ". . . .", ". . . .", ". . . .", ". . . .",

        ". . . .", ". . . .", ". . . .",
        ". . * *", ". . * .", ". * * .",
        ". * x .", ". * x *", ". . x *",
        ". . . .", ". . . .", ". . . ."
    };

    private static int[] rowsSizes = { 4, 3 };
    
    // clokwise wallkicks for the 2 blocks wide shapes
    private static IntVector[][] wallKicks2 = {
        {}, {}, {}, {}
    };

    // clokwise wallkicks for the 3 blocks wide shapes
    private static IntVector[][] wallKicks3 = {
        // 0 == initial, 2 == upside down
        // 0 -> R
        {iVect(-1, 0), iVect(-1, -1), iVect(0,  2), iVect(-1,  2)},
        // R -> 2
        {iVect( 1, 0), iVect( 1,  1), iVect(0, -2), iVect( 1, -2)},
        // 2 -> L
        {iVect( 1, 0), iVect( 1, -1), iVect(0,  2), iVect( 1,  2)},
        // L -> 0
        {iVect(-1, 0), iVect(-1,  1), iVect(0, -2), iVect(-1, -2)}
    };

    // clokwise wallkicks for the 4 blocks wide shapes
    private static IntVector[][] wallKicks4 = {
        {iVect(-2, 0), iVect( 1, 0), iVect(-2,  1), iVect( 1, -2)},
        {iVect(-1, 0), iVect( 2, 0), iVect(-1, -2), iVect( 2,  1)},
        {iVect( 2, 0), iVect(-1, 0), iVect( 2, -1), iVect(-1,  2)},
        {iVect( 1, 0), iVect(-2, 0), iVect(-1, -2), iVect( 2,  1)}
    };

    private boolean[][] initial, left, right, upsideDown;
    private DoubleVector pivot;
    private DoubleVector[] convexHull;
    
    /**
     * Loads the {@code boolean[][]} representations of the shapes
     * for every rotation.
     */
    static {
        for (int i = 0; i < values().length; i++) {
            TetrisShapeType shape = values()[i];

            shape.initial = Util.convertStringShapesArray(shapes,
                FRAME_SIZE, rowsSizes, i, "*");

            boolean[][] pivotTable = Util.convertStringShapesArray(pivots,
                    FRAME_SIZE, rowsSizes, i, "x");

            double xPivot = -1, yPivot = -1;
            for (int y = 0; y < pivotTable.length; y++) {
                for (int x = 0; x < pivotTable[y].length; x++) {
                    if (pivotTable[y][x]) {
                        if (yPivot < 0) {
                            xPivot = x;
                            yPivot = y;
                        } else {
                            xPivot = (xPivot + x) / 2;
                            yPivot = (yPivot + y) / 2;
                        }
                    }
                }
            }
            if (xPivot >= 0) {
                shape.pivot = new DoubleVector(xPivot + 0.5, yPivot + 0.5);
            }

            shape.left = Util.rotate(shape.initial, shape.pivot,
                    Rotation.LEFT);
            shape.right = Util.rotate(shape.initial, shape.pivot,
                    Rotation.RIGHT);
            shape.upsideDown = Util.rotate(shape.initial,
                    shape.pivot, Rotation.UPSIDE_DOWN);
            shape.convexHull = ShapeType.getConvexHull(shape);
        }
        // these arrays are not needed anymore
        shapes = null;
        pivots = null;
        rowsSizes = null;
    }

    @Override
    public boolean isSolid(int x, int y) {
        return initial[y][x];
    }

    @Override
    public boolean isSolid(int x, int y, Rotation rotation) {
        switch (rotation) {
            case LEFT:
                return left[y][x];
            case RIGHT:
                return right[y][x];
            case UPSIDE_DOWN:
                return upsideDown[y][x];
            default:
                return initial[y][x];
        }
    }

    @Override
    public int getFrameSize() {
        return FRAME_SIZE;
    }

    @Override
    public IntVector[] getRightWallKicks(Rotation initialRotation) {
        int initialRotationIndex = initialRotation.ordinal();
        switch (this) {
            case CUBE_SHAPE:
                return Arrays.copyOf(wallKicks2[initialRotationIndex],
                        wallKicks2[initialRotationIndex].length);
            case I_SHAPE:
                return Arrays.copyOf(wallKicks4[initialRotationIndex],
                        wallKicks4[initialRotationIndex].length);
            default:
                return Arrays.copyOf(wallKicks3[initialRotationIndex],
                        wallKicks3[initialRotationIndex].length);
        }
    }

    @Override
    public IntVector[] getLeftWallKicks(Rotation initialRotation) {
        IntVector[] wallKicks
                = getRightWallKicks(initialRotation.add(Rotation.LEFT));
        for (int i = 0; i < wallKicks.length; i++) {
            wallKicks[i] = wallKicks[i].negate();
        }
        return wallKicks;
    }


    @Override
    public DoubleVector getRotationPivot() {
        return pivot;
    }

    @Override
    public DoubleVector[] getConvexHull() {
        return Arrays.copyOf(convexHull, convexHull.length);
    }

    @Override
    public IntVector getPreciseAABBMin() {
        if (this == I_SHAPE) {
            return iVect(0, 1);
        } else {
            return iVect(1, 1);
        }
    }

    @Override
    public IntVector getPreciseAABBMax() {
        switch (this) {
            case I_SHAPE:
                return iVect(3, 1);
            case CUBE_SHAPE:
                return iVect(2, 2);
            default:
                return iVect(3, 2);
        }
    }

    @Override
    public int getSolidBlocksNumber() {
        return 4;
    }

    @Override
    public String toString() {
        return Util.booleanMatrixToString(initial);
    }
}
