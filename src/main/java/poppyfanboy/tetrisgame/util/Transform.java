package poppyfanboy.tetrisgame.util;

import java.awt.geom.AffineTransform;
import java.util.Arrays;

/**
 * A convenience class for any general transformations of the 2D plane,
 * that is it is a move-and-rotate transformation.
 */
public class Transform {
    private final DoubleVector translation;
    private final RotationTransform rotation;
    private final double[] matrix;

    public Transform() {
        translation = new DoubleVector(0, 0);
        rotation = new RotationTransform();
        matrix = generateTransformMatrix(rotation, translation);
    }

    public Transform(DoubleVector translation) {
        this.translation = translation;
        rotation = new RotationTransform();
        matrix = generateTransformMatrix(rotation, translation);
    }

    public Transform(RotationTransform rotation,
            DoubleVector translation) {
        this.translation = translation;
        this.rotation = rotation;
        matrix = generateTransformMatrix(rotation, translation);
    }

    private static double[] generateTransformMatrix(
            RotationTransform rotation, DoubleVector translation) {
        return new double[] {
                rotation.getCos(), -rotation.getSin(), translation.getX(),
                rotation.getSin(),  rotation.getCos(), translation.getY(),
                0, 0, 1
        };
    }

    public DoubleVector apply(DoubleVector v) {
        return translation.add(rotation.apply(v));
    }

    public DoubleVector apply(double x, double y) {
        return translation.add(rotation.apply(new DoubleVector(x, y)));
    }

    public DoubleVector[] apply(DoubleVector[] vectors) {
        DoubleVector[] transformed
                = Arrays.copyOf(vectors, vectors.length);
        for (int i = 0; i < transformed.length; i++) {
            transformed[i] = apply(transformed[i]);
        }
        return transformed;
    }

    public Transform combine(Transform other) {
        return new Transform(
                other.rotation.combine(this.rotation),
                other.apply(this.translation));
    }

    public Transform combine(DoubleVector otherTranslation) {
        return combine(new Transform(otherTranslation));
    }

    public Transform combine(RotationTransform otherRotation) {
        return combine(
                new Transform(otherRotation, new DoubleVector(0, 0)));
    }

    public DoubleVector getTranslation() {
        return translation;
    }

    public RotationTransform getRotation() {
        return rotation;
    }

    /**
     * Returns a matrix representation of the transform. The matrix
     * has the following form:
     * cosX -sinX xt
     * sinX  cosX yt
     * 0     0    1
     * This method accesses the specific row and column of this matrix.
     */
    public double matrix(int row, int col) {
        return matrix[row * 3 + col];
    }

    public AffineTransform getTransform() {
        return new AffineTransform(
            matrix(0, 0), matrix(1, 0),
            matrix(0, 1), matrix(1, 1),
            matrix(0, 2), matrix(1, 2));
    }

    public static Transform getRotation(double angle, DoubleVector pivot) {
        return new Transform(pivot.times(-1))
            .combine(new Transform(new RotationTransform(angle), pivot));
    }

    @Override
    public String toString() {
        return String.format("angle = %.4f, shift = (%.4f, %.4f)",
                rotation.getAngle(),
                translation.getX(), translation.getY());
    }
}
