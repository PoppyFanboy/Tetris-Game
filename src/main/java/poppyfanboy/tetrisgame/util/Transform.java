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

    public Transform() {
        translation = new DoubleVector(0, 0);
        rotation = new RotationTransform();
    }

    public Transform(DoubleVector translation) {
        this.translation = translation;
        rotation = new RotationTransform();
    }

    public Transform(RotationTransform rotation,
            DoubleVector translation) {
        this.translation = translation;
        this.rotation = rotation;
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

    public Transform combine(double x, double y) {
        return this.combine(new Transform(new DoubleVector(x, y)));
    }

    public Transform combine(DoubleVector otherTranslation) {
        return combine(new Transform(otherTranslation));
    }

    public Transform combine(RotationTransform otherRotation) {
        return combine(
                new Transform(otherRotation, new DoubleVector(0, 0)));
    }

    public Transform tScale(int scale) {
        return new Transform(rotation, translation.times(scale));
    }

    public DoubleVector getTranslation() {
        return translation;
    }

    public RotationTransform getRotation() {
        return rotation;
    }

    public AffineTransform getTransform() {
        return new AffineTransform(rotation.getCos(), rotation.getSin(),
                -rotation.getSin(), rotation.getCos(),
                translation.getX(), translation.getY());
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
