package poppyfanboy.tetrisgame.entities.shapetypes;

import java.util.Arrays;

import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Util;

/**
 * Represents a shape from the tetris game built of five blocks.
 * For more information on the inner implementation details see
 * {@link TetrisShapeType}.
 */
public enum PentrixShapeType implements ShapeType {
    F_SHAPE, I_SHAPE, L_SHAPE, N_SHAPE, P_SHAPE,
    T_SHAPE, U_SHAPE, V_SHAPE, W_SHAPE, X_SHAPE,
    Y_SHAPE, Z_SHAPE, F_SHAPE_MIRRORED, L_SHAPE_MIRRORED, P_SHAPE_MIRRORED,
    N_SHAPE_MIRRORED, Y_SHAPE_MIRRORED, Z_SHAPE_MIRRORED;

    public static final int FRAME_SIZE = 5;

    private static String[] shapes = {
        // F shape   // I shape   // L shape   // N shape   // P shape
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . . * .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . * * *", "* * * * *", ". . . . *", ". * * . .", ". . * * .",
        ". . . . *", ". . . . .", ". * * * *", ". . * * *", ". . * * *",
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        // T shape   // U shape   // V shape   // W shape   // X shape
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . * . .", ". . . . .", ". . . . *", ". . * . .", ". . * . .",
        ". . * . .", ". . * . *", ". . . . *", ". . * * .", ". * * * .",
        ". * * * .", ". . * * *", ". . * * *", ". . . * *", ". . * . .",
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        // Y shape   // Z shape
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . . . .", ". . . . *", ". . . * .", ". . . . .", ". . . . .",
        ". . . * .", ". . * * *", ". . * * *", ". * . . .", ". . * * *",
        ". * * * *", ". . * . .", ". . * . .", ". * * * *", ". . * * .",
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",

        ". . . . .", ". . . . .", ". . . . .",
        ". . . . .", ". . . . .", ". . * . .",
        ". . . * *", ". . * . .", ". . * * *",
        ". * * * .", ". * * * *", ". . . . *",
        ". . . . .", ". . . . .", ". . . . ."
    };

    private static String[] pivots = {
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . . * .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . * x *", "* * x * *", ". . x . *", ". * * x .", ". . x * .",
        ". . . . *", ". . . . .", ". * * x *", ". . x * *", ". . * x *",
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",

        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . * . .", ". . . . .", ". . . . *", ". . * . .", ". . * . .",
        ". . x . .", ". . * x *", ". . . x *", ". . * x .", ". * x * .",
        ". * * * .", ". . * * *", ". . * * *", ". . . * *", ". . * . .",
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",

        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",
        ". . . . .", ". . . . *", ". . . * .", ". . . . .", ". . . . .",
        ". . x * .", ". . * x *", ". . * x *", ". * . x .", ". . x * *",
        ". * * x *", ". . * . .", ". . * . .", ". * x * *", ". . * x .",
        ". . . . .", ". . . . .", ". . . . .", ". . . . .", ". . . . .",

        ". . . . .", ". . . . .", ". . . . .",
        ". . . . .", ". . . . .", ". . * . .",
        ". . x * *", ". . * x .", ". . * x *",
        ". * * x .", ". * x * *", ". . . . *",
        ". . . . .", ". . . . .", ". . . . ."
    };

    private static int[] rowsSizes = { 5, 5, 5, 3 };

    // clockwise wallkicks for the 3 blocks wide shapes
    private static IntVector[][] wallKicks3 = {
        // 0 == initial, 2 == upside down
        // 0 -> R
        {IntVector.iVect(-1, 0), IntVector.iVect(-1, -1), IntVector.iVect(0,  2), IntVector.iVect(-1,  2)},
        // R -> 2
        {IntVector.iVect( 1, 0), IntVector.iVect( 1,  1), IntVector.iVect(0, -2), IntVector.iVect( 1, -2)},
        // 2 -> L
        {IntVector.iVect( 1, 0), IntVector.iVect( 1, -1), IntVector.iVect(0,  2), IntVector.iVect( 1,  2)},
        // L -> 0
        {IntVector.iVect(-1, 0), IntVector.iVect(-1,  1), IntVector.iVect(0, -2), IntVector.iVect(-1, -2)}
    };

    // clockwise wallkicks for the 4 blocks wide shapes
    private static IntVector[][] wallKicks4 = {
        {IntVector.iVect(-2, 0), IntVector.iVect( 1, 0), IntVector.iVect(-2,  1), IntVector.iVect( 1, -2)},
        {IntVector.iVect(-1, 0), IntVector.iVect( 2, 0), IntVector.iVect(-1, -2), IntVector.iVect( 2,  1)},
        {IntVector.iVect( 2, 0), IntVector.iVect(-1, 0), IntVector.iVect( 2, -1), IntVector.iVect(-1,  2)},
        {IntVector.iVect( 1, 0), IntVector.iVect(-2, 0), IntVector.iVect(-1, -2), IntVector.iVect( 2,  1)}
    };

    // clockwise wallkicks for the 5 blocks wide shapes
    private static IntVector[][] wallKicks5 = {
        {IntVector.iVect(-2, 0), IntVector.iVect( 1, 0), IntVector.iVect(-2,  2), IntVector.iVect( 2, -2)},
        {IntVector.iVect(-1, 0), IntVector.iVect( 2, 0), IntVector.iVect(-2, -2), IntVector.iVect( 2,  2)},
        {IntVector.iVect( 2, 0), IntVector.iVect(-1, 0), IntVector.iVect( 2, -2), IntVector.iVect(-2,  2)},
        {IntVector.iVect( 1, 0), IntVector.iVect(-2, 0), IntVector.iVect(-2, -2), IntVector.iVect( 2,  2)}
    };

    private boolean[][] initial, left, right, upsideDown;
    private DoubleVector pivot;
    private DoubleVector[] convexHull;

    /**
     * Loads the {@code boolean[][]} representations of the shapes for
     * every rotation of the shape.
     */
    static {
        for (int i = 0; i < values().length; i++) {
            PentrixShapeType shape = values()[i];

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
            if (yPivot >= 0) {
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
            case I_SHAPE:
                return Arrays.copyOf(wallKicks5[initialRotationIndex],
                        wallKicks5[initialRotationIndex].length);
            case L_SHAPE:
            case L_SHAPE_MIRRORED:
            case N_SHAPE:
            case N_SHAPE_MIRRORED:
            case Y_SHAPE:
            case Y_SHAPE_MIRRORED:
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
    public int getSolidBlocksNumber() {
        return 5;
    }

    @Override
    public String toString() {
        return Util.booleanMatrixToString(initial);
    }
}
