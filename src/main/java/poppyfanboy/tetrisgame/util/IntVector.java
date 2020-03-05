package poppyfanboy.tetrisgame.util;

import java.util.Comparator;

/**
 * A simple convenience class used to hold the integer coordinates of a
 * point. The very same functionality is provided by using the
 * {@link DoubleVector} class, but I think that
 * it is better to provide a separate class for the ints, so to limit the
 * set of possible values.
 */
public class IntVector {
    public static Comparator<IntVector> Y_ORDER = (p1, p2) ->
            p1.y != p2.y
                ? Integer.compare(p1.y, p2.y)
                : Integer.compare(p1.x, p2.x);

    private int x, y;

    public IntVector(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public IntVector add(IntVector other) {
        return new IntVector(this.x + other.x, this.y + other.y);
    }

    public DoubleVector add(DoubleVector other) {
        return new DoubleVector(this.x + other.getX(),
                this.y + other.getY());
    }

    public IntVector subtract(IntVector other) {
        return new IntVector(this.x - other.x, this.y - other.y);
    }

    public DoubleVector subtract(DoubleVector other) {
        return new DoubleVector(this.x - other.getX(),
                this.y - other.getY());
    }

    public IntVector add(int x, int y) {
        return new IntVector(this.x + x, this.y + y);
    }

    public DoubleVector add(double x, double y) {
        return new DoubleVector(this.x + x, this.y + y);
    }

    public IntVector negate() {
        return new IntVector(-x, -y);
    }

    public IntVector times(int coefficient) {
        return new IntVector(x * coefficient, y * coefficient);
    }

    public DoubleVector toDouble() {
        return new DoubleVector(x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (this.getClass() != otherObject.getClass()) {
            return false;
        }
        IntVector other = (IntVector) otherObject;
        return this.x == other.x && this.y == other.y;
    }

    /**
     * See {@link DoubleVector#dVect(double, double)}.
     */
    public static IntVector iVect(int x, int y) {
        return new IntVector(x, y);
    }
}
