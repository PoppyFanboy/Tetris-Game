package poppyfanboy.tetrisgame.util;

/**
 * A convenience class that represents a rotation transformation of the
 * 2D plane. The rotations can be combined with each other allowing you
 * to easily apply a series of rotation transformations.
 */
public class RotationTransform {
    private double cosX, sinX;

    public RotationTransform() {
        // default values correspond to the identity rotation matrix
        cosX = 1.0;
        sinX = 0.0;
    }

    public RotationTransform(double angle) {
        cosX = Math.cos(angle);
        sinX = Math.sin(angle);
    }

    private RotationTransform(double cosX, double sinX) {
        this.cosX = cosX;
        this.sinX = sinX;
    }

    public DoubleVector apply(DoubleVector v) {
        return new DoubleVector(
                cosX * v.getX() - sinX * v.getY(),
                sinX * v.getX() + cosX * v.getY());
    }

    public RotationTransform combine(RotationTransform other) {
        return new RotationTransform(
                this.cosX * other.cosX - this.sinX * other.sinX,
                this.sinX * other.cosX + this.cosX * other.sinX);
    }

    public Transform combine(Transform other) {
        return new Transform(this, new DoubleVector(0, 0)).combine(other);
    }

    public double getAngle() {
        return Math.atan2(sinX, cosX);
    }

    public double getCos() {
        return cosX;
    }

    public double getSin() {
        return sinX;
    }
}
