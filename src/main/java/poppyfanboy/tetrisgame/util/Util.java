package poppyfanboy.tetrisgame.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.round;
import static poppyfanboy.tetrisgame.util.DoubleVector.dVect;

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
                    sb.append("b");
                } else {
                    sb.append(" ");
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
            if (random.nextInt(watchedOptionsCount) < instancesCount) {
                randomEnumIndex = i;
                randomInstanceIndex = random.nextInt(instancesCount);
            }
        }
        if (randomEnumIndex != -1 && randomInstanceIndex != -1) {
            return
                enums[randomEnumIndex].getEnumConstants()[randomInstanceIndex];
        } else {
            return null;
        }
    }

    public static BufferedImage scaleImage(BufferedImage image, double scaleX,
            double scaleY, boolean bilinearInterpolation) {
        if (image == null) {
            return null;
        }
        scaleX =  Math.max(scaleX, 0);
        scaleY =  Math.max(scaleY, 0);

        BufferedImage scaled = new BufferedImage(
                (int) Math.ceil(image.getWidth() * scaleX),
                (int) Math.ceil(image.getHeight() * scaleY),
                BufferedImage.TYPE_INT_ARGB_PRE);

        if (scaleX != 1 || scaleY != 1) {
            AffineTransform transform
                    = AffineTransform.getScaleInstance(scaleX, scaleY);
            AffineTransformOp op = new AffineTransformOp(transform,
                    bilinearInterpolation
                            ? AffineTransformOp.TYPE_BILINEAR
                            : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            op.filter(image, scaled);
        } else {
            scaled.getGraphics().drawImage(image, 0, 0, null);
        }
        return scaled;
    }

    public static BufferedImage resizeImage(BufferedImage image, int newWidth,
            int newHeight, boolean bilinearInterpolation) {
        if (image == null) {
            return null;
        }
        newWidth = Math.max(newWidth, 0);
        newHeight = Math.max(newHeight, 0);
        return scaleImage(image, (double) newWidth / image.getWidth(),
                (double) newHeight / image.getHeight(), bilinearInterpolation);
    }

    public static BufferedImage mirrorImage(BufferedImage image,
            boolean horizontal, boolean vertical) {
        if (image == null) {
             return null;
        }
        BufferedImage mirrored = new BufferedImage(image.getWidth(),
                image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        // mirror
        AffineTransform transform = AffineTransform.getScaleInstance(
                vertical ? -1 : 1,
                horizontal ? -1 : 1);
        // shift back
        if (horizontal) {
            transform.translate(0, -image.getHeight());
        }
        if (vertical) {
            transform.translate(-image.getWidth(), 0);
        }
        Graphics2D g2d = (Graphics2D) mirrored.getGraphics();
        g2d.drawImage(image, transform, null);
        return mirrored;
    }

    public static BufferedImage rotateImage(BufferedImage image, int quadrant) {
        if (image == null) {
            return null;
        }
        quadrant = ((quadrant % 4) + 4) % 4;

        BufferedImage rotated = new BufferedImage(image.getWidth(),
                image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        AffineTransform transform = AffineTransform.getQuadrantRotateInstance(
                quadrant, image.getHeight() / 2.0,
                image.getWidth() / 2.0);
        Graphics2D g2d = (Graphics2D) rotated.getGraphics();
        g2d.drawImage(image, transform, null);
        return rotated;
    }


    /**
     * Fills the {@code width x height} tiled area with tile images. If some
     * of the images for the tiles are missing, just pass {@code null}
     * instead of those. Missing images will be replaced with {@code center}
     * image, if possible.
     *
     * If either the width or height is less than 2 tiles, the image is tiled
     * using solely the {@code center} images.
     *
     * All images will be scaled to the {@code blockWidth x blockWidth} size,
     * if they have a different size.
     */
    public static BufferedImage fillTiles(int width, int height,
            int blockWidth, BufferedImage upperLeftCorner,
            BufferedImage leftSide, BufferedImage center) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(String.format("Width and"
                + " height must be non-negative integers. Got: width = %d,"
                + " height = %d", width, height));
        }
        center = resizeImage(center, blockWidth, blockWidth, false);

        upperLeftCorner
                = resizeImage(upperLeftCorner, blockWidth, blockWidth, false);
        BufferedImage upperRightCorner
                = mirrorImage(upperLeftCorner, false, true);
        BufferedImage bottomRightCorner
                = mirrorImage(upperLeftCorner, true, true);
        BufferedImage bottomLeftCorner
                = mirrorImage(upperLeftCorner, true, false);

        leftSide = resizeImage(leftSide, blockWidth, blockWidth, false);
        BufferedImage rightSide = Util.rotateImage(leftSide, 2);
        BufferedImage upperSide = Util.rotateImage(leftSide, 1);
        BufferedImage bottomSide = Util.rotateImage(leftSide, 3);

        // center tile does not need to be rotated or mirrored
        if (upperLeftCorner == null || width < 2 || height < 2) {
            upperLeftCorner = center;
        }
        if (leftSide == null || width < 2 || height < 2) {
            leftSide = center;
        }

        BufferedImage tiledImage = new BufferedImage(width * blockWidth,
                height * blockWidth, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = tiledImage.getGraphics();

        // corners
        if (upperLeftCorner != null) {

            g.drawImage(upperLeftCorner, 0, 0, null);
            g.drawImage(upperRightCorner, (width - 1) * blockWidth, 0, null);
            g.drawImage(bottomRightCorner,
                    (width - 1) * blockWidth, (height - 1) * blockWidth, null);
            g.drawImage(bottomLeftCorner, 0, (height - 1) * blockWidth, null);
        }

        // upper/bottom sides
        for (int i = 1; i < width - 1; i++) {
            g.drawImage(upperSide, i * blockWidth, 0, null);
            g.drawImage(bottomSide, i * blockWidth,
                    (height - 1) * blockWidth, null);
        }
        // left/right sides
        for (int i = 1; i < height - 1; i++) {
            g.drawImage(leftSide, 0, i * blockWidth, null);
            g.drawImage(rightSide,
                    (width - 1) * blockWidth, i * blockWidth, null);
        }

        // center pieces
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                g.drawImage(center, x * blockWidth, y * blockWidth, null);
            }
        }
        return tiledImage;
    }

    public static boolean convexHullsIntersect(List<DoubleVector> convexHull1,
            List<DoubleVector> convexHull2) {
        /*System.out.println(convexHull1);
        System.out.println(convexHull2);
        System.out.println("--");*/

        for (int i = 0; i < convexHull1.size(); i++) {
            DoubleVector p1 = convexHull1.get(i);
            DoubleVector p2 = convexHull1.get((i + 1) % convexHull1.size());
            for (int j = 0; j < convexHull2.size(); j++) {
                DoubleVector q1 = convexHull2.get(j);
                DoubleVector q2 = convexHull2.get((j + 1) % convexHull2.size());
                if (segmentsIntersect(p1, p2, q1, q2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean segmentsIntersect(DoubleVector p1, DoubleVector p2,
            DoubleVector q1, DoubleVector q2) {
        // edge cases
        if (Math.max(p1.getX(), p2.getX()) < Math.min(q1.getX(), q2.getX())) {
            return false;
        }
        if (Math.max(q1.getX(), q2.getX()) < Math.min(p1.getX(), p2.getX())) {
            return false;
        }
        if (Math.max(p1.getY(), p2.getY()) < Math.min(q1.getY(), q2.getY())) {
            return false;
        }
        if (Math.max(q1.getY(), q2.getY()) < Math.min(p1.getY(), p2.getY())) {
            return false;
        }
        double kp = (p1.getY() - p2.getY()) / (p1.getX() - p2.getX());
        double bp = p1.getY() - kp * p1.getX();
        double kq = (q1.getY() - q2.getY()) / (q1.getX() - q2.getX());
        double bq = q1.getY() - kq * q1.getX();

        // parallel lines
        if (kp == kq || Double.isInfinite(kp) && Double.isInfinite(kq)) {
            return false;
        }
        // first segment is vertical
        if (Double.isInfinite(kp)) {
            double yIntersect = kq * p1.getX() + bq;
            return yIntersect < Math.max(p1.getY(), p2.getY())
                    && yIntersect > Math.min(p1.getY(), p2.getY());
        }
        // second segment is vertical
        if (Double.isInfinite(kq)) {
            double yIntersect = kp * q1.getX() + bp;
            return yIntersect < Math.max(q1.getY(), q2.getY())
                    && yIntersect > Math.min(q1.getY(), q2.getY());
        }

        double xIntersect = (bq - bp) / (kp - kq);
        return xIntersect > Math.max(Math.min(p1.getX(), p2.getX()),
                        Math.min(q1.getX(), q2.getX()))
                && Math.min(Math.max(p1.getX(), p2.getX()),
                        Math.max(q1.getX(), q2.getX())) > xIntersect;
    }
}
