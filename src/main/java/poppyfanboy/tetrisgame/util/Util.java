package poppyfanboy.tetrisgame.util;

import java.util.Random;

import static java.lang.Math.round;

public class Util {
    /**
     * Convenience method for converting a human-readable
     * {@code String} array containing {@code boolean[][]} arrays
     * into the raw {@code boolean[][][]} data. It returns the
     * {@code boolean[][]} array by the specified index.
     *
     * @param   shapes source data. {@code boolean[][]} arrays here
     *          are stored as follows (`frameSize` = 2 case):
     * <code><pre>
     * shapes = {
     *     ". .", "* .", ". *",
     *     ". .", ". *", "* .",
     *
     *     "* .", "* *",
     *     "* .", ". *"
     * }
     * </pre></code>
     *          Thus the call
     *          {@code convertStringShapesArray(shapes, 2, [3, 2], 3, "*")}
     *          will return the [[true, false], [true, false]] array.
     *
     * @param   frameSize simultaneously the width and the height of
     *          the arrays stored in {@code shapes} array.
     *
     * @param   rowsSizes an array containing numbers of
     *          {@code boolean[][]} arrays in each visual row.
     *
     * @param   shapeIndex the ordinal number of the target
     *          {@code boolean[][]} that will eventually be returned.
     *
     * @param   trueString the value which is mapped to the {@code true}
     *          value in terms of the {@code boolean[][] array}.
     *
     * @throws  IllegalArgumentException in case the shape index is out
     *          of bounds.
     */
    public static boolean[][] convertStringShapesArray(String[] shapes,
            int frameSize, int[] rowsSizes, int shapeIndex,
            String trueString) {
        if (shapeIndex < 0) {
            throw new IllegalArgumentException(String.format(
                "Shape index argument is expected to be non-negative."
                + " Got: shapeIndex = %d.", shapeIndex));
        }

        // shapeIndex is converted into the imaginary matrix coordinates
        int targetCol = -1, targetRow = -1;
        // number of `shapes` array entries skipped in front of the
        // representation of the target `boolean[][]` array
        int rowOffset = 0;
        // traverse the rows of boolean[][] arrays
        for (int i = 0; i < rowsSizes.length; i++) {
            if (shapeIndex >= rowsSizes[i]) {
                shapeIndex -= rowsSizes[i];
                // skip the row
                rowOffset += frameSize * rowsSizes[i];
            } else {
                // row found
                targetCol = shapeIndex;
                targetRow = i;
                break;
            }
        }
        if (targetCol == -1) {
            throw new IllegalArgumentException(String.format(
                "%d-th shape does not belong to this set of shapes",
                shapeIndex));
        }

        boolean[][] result = new boolean[frameSize][frameSize];
        for (int row = 0; row < frameSize; row++) {
            int rawIndex
                = rowOffset + targetCol + row * rowsSizes[targetRow];
            String[] rowData = shapes[rawIndex].split(" ");
            for (int col = 0; col < frameSize; col++) {
                result[row][col] = rowData[col].equals(trueString);
            }
        }
        return result;
    }

    public static String booleanMatrixToString(boolean[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (boolean[] row : matrix) {
            for (boolean element : row) {
                if (element) {
                    sb.append("* ");
                } else {
                    sb.append(". ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Rotates the shape defined in the {@code shape} array.
     *
     * @param   shape is the thing to be rotated. Solid blocks of the
     *          shape are marked with {@code true} values, empty spaces
     *          are marked with {@code false}. The {@code shape} is
     *          expected to be a square array.
     *
     * @throws  IllegalArgumentException in case the {@code shape} array
     *          is not a square array.
     */
    public static boolean[][] rotate(boolean[][] shape, DoubleVector pivot,
            Rotation direction) {
        final int height = shape.length;
        for (int row = 0, width = -1; row < height; row++) {
            if (width < 0) {
                width = shape[row].length;
                if (width != height) {
                    throw new IllegalArgumentException(String.format(
                        "shape array is expected to be square. Got:"
                        + " shape.length = %d, shape[0].length = %d",
                        height, width));
                }
            } else {
                if (width != shape[row].length) {
                    throw new IllegalArgumentException(String.format(
                        "shape array is expected to be square. Got:"
                        + " shape[0].length = %d, shape[%d].length = %d",
                        width, row, shape[row].length));
                }
            }
        }

        boolean[][] rotated = new boolean[height][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < height; x++) {
                if (shape[y][x]) {
                    DoubleVector rotatedCoords
                        = new DoubleVector(x + 0.5, y + 0.5)
                        .subtract(pivot).rotate(direction).add(pivot);
                    int xRotated = (int) round(rotatedCoords.getX() - 0.5);
                    int yRotated = (int) round(rotatedCoords.getY() - 0.5);

                    rotated[yRotated][xRotated] = shape[y][x];
                }
            }
        }
        return rotated;
    }

    /**
     * Copies the given jagged {@code boolean[][]} jagged array.
     */
    public static boolean[][] jaggedArrayCopy(boolean[][] array) {
        boolean[][] copy = new boolean[array.length][];
        for (int i = 0; i < array.length; i++) {
            copy[i] = new boolean[array[i].length];
            System.arraycopy(array[i], 0, copy[i], 0, array[i].length);
        }
        return copy;
    }

    @SafeVarargs
    public static <E extends Enum<?>> E getRandomInstance(
            Random random, Class<? extends E>... enums) {
        int randomEnumIndex = -1, randomInstanceIndex = -1;
        int watchedOptionsCount = 0;
        for (int i = 0; i < enums.length; i++) {
            int instancesCount = enums[i].getEnumConstants().length;
            watchedOptionsCount += instancesCount;
            if (random.nextDouble()
                    <= (double) instancesCount / watchedOptionsCount) {
                randomEnumIndex = i;
                randomInstanceIndex = random.nextInt(instancesCount);
            } else {
                break;
            }
        }
        if (randomEnumIndex != -1 && randomInstanceIndex != -1) {
            return
                enums[randomEnumIndex].getEnumConstants()[randomInstanceIndex];
        } else {
            return null;
        }
    }
}
