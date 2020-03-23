package poppyfanboy.tetrisgame.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * A simple convenience class that holds the coordinates of a point.
 *
 * Some basic operations like adding two vectors or rotating
 * a vector by 90 or 180 degrees are also provided.
 */
public class DoubleVector {
    public static final Comparator<DoubleVector> Y_ORDER
        = (p1, p2) -> p1.y != p2.y
            ? Double.compare(p1.y, p2.y)
            : Double.compare(p1.x, p2.x);

    private double x, y;

    public DoubleVector(double x, double y) {
        this.y = y;
        this.x = x;
    }

    public DoubleVector add(DoubleVector other) {
        return new DoubleVector(this.x + other.x, this.y + other.y);
    }

    public DoubleVector add(IntVector other) {
        return
            new DoubleVector(this.x + other.getX(), this.y + other.getY());
    }

    public DoubleVector add(double x, double y) {
        return new DoubleVector(this.x + x, this.y + y);
    }

    public DoubleVector subtract(DoubleVector other) {
        return new DoubleVector(this.x - other.x, this.y - other.y);
    }

    /**
     * 90 degree counter-clockwise rotation.
     */
    public DoubleVector rotateLeft() {
        // multiply rotation matrix by this vector
        // | newX | = |  0 1 | | x |
        // | newY | = | -1 0 | | y |
        return new DoubleVector(y, -x);
    }

    /**
     * 90 degree clockwise rotation.
     */
    public DoubleVector rotateRight() {
        // | newX | = | 0 -1 | | x |
        // | newY | = | 1  0 | | y |
        return new DoubleVector(-y, x);
    }

    /**
     * 180 degree rotation.
     */
    public DoubleVector rotatePI() {
        return new DoubleVector(-x, -y);
    }

    /**
     * Rotates the vector in the specified direction.
     */
    public DoubleVector rotate(Rotation direction) {
        switch (direction) {
            case LEFT:
                return rotateLeft();
            case RIGHT:
                return rotateRight();
            case UPSIDE_DOWN:
                return rotatePI();
            default:
                return this;
        }
    }

    /**
     * Normalizes the vector, so it would has 1.0 length.
     */
    public DoubleVector normalize() {
        if (x != 0 || y != 0) {
            return new DoubleVector(x / length(), y / length());
        } else {
            return new DoubleVector(0, 0);
        }
    }

    public DoubleVector times(double coefficient) {
        return new DoubleVector(coefficient * x, coefficient * y);
    }

    public DoubleVector times(double xCoefficient, double yCoefficient) {
        return new DoubleVector(xCoefficient * x, yCoefficient * y);
    }

    public DoubleVector rotate(double angle) {
        // | newX | = | cos a -sin a | | x |
        // | newY | = | sin a  cos a | | y |
        return new DoubleVector(cos(angle) * x - sin(angle) * y,
            sin(angle) * x + cos(angle) * y);
    }

    public DoubleVector rotate(double angle, DoubleVector pivot) {
        return this.subtract(pivot).rotate(angle).add(pivot);
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public Comparator<DoubleVector> getPolarAngleComparator(double eps) {
        return (p1, p2) -> {
            if (p1.y - this.y >= 0 && p2.y - this.y < 0) {
                // p1 is above the OX, p2 is below
                return -1;
            } else if (p1.y - this.y < 0 && p2.y - this.y >= 0) {
                // p1 is below the OX, p2 is above
                return 1;
            } else if (p1.y == this.y && p2.y == this.y) {
                // horizontal collinear points
                if (p1.x - this.x >= 0 && p2.x - this.x < 0) {
                    return -1;
                } else if (p1.x - this.x < 0 && p2.x - this.x >= 0) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                // now the polar angle between p1 and p2 is < PI
                return -ccwTurn(this, p1, p2, eps);
            }
        };
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f)", x, y);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (this.getClass() != otherObject.getClass()) {
            return false;
        }
        DoubleVector other = (DoubleVector) otherObject;
        return Double.compare(this.x, other.x) == 0
                && Double.compare(this.y, other.y) == 0;
    }

    public static double dotProduct(DoubleVector v1, DoubleVector v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    /**
     * Returns a polar angle of the given vector. The returned value
     * lies in the interval [0, 2PI).
     */
    public static double polarAngle(DoubleVector v) {
        double atan2 = Math.atan2(v.y, v.x);
        return atan2 >= 0 ? atan2 : atan2 + 2 * Math.PI;
    }

    /**
     * Returns {@code -1} in case the points a, b, c form a clockwise
     * turn, {@code 1} in case they form a counter-clockwise turn,
     * and {@code 0} in case these three point lie on the same line.
     */
    public static int ccwTurn(DoubleVector a, DoubleVector b,
            DoubleVector c, double epsilon) {
        // compute the x2 signed area of the triangle built on the
        // ab and ac vectors
        double area
            = (b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y);
        if (area < -epsilon) {
            return -1;
        } else if (area > epsilon) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Performs precise computations, but might be error-prone in case
     * the inputs have some round-off errors.
     */
    public static int ccwTurn(DoubleVector a, DoubleVector b,
            DoubleVector c) {
        return ccwTurn(a, b, c, 0.0);
    }

    /**
     * Returns a convex hull of the given set of points. It is OK for the
     * {@code points} array to contain duplicate values. The returned
     * array is an array of convex hull points enumerated in the
     * counter-clockwise direction.
     */
    public static DoubleVector[] getConvexHull(DoubleVector[] points,
            double eps) {
        if (points == null) {
            return null;
        }
        if (points.length <= 3) {
            return Arrays.copyOf(points, points.length);
        }
        ArrayDeque<DoubleVector> convexHull = new ArrayDeque<>();
        Arrays.sort(points, DoubleVector.Y_ORDER);
        Arrays.sort(points, 1, points.length,
                points[0].getPolarAngleComparator(eps));
        // add the lowest point to the convex hull
        convexHull.push(points[0]);

        // find first point that differ from the points[0]
        int nextNonEqual = -1;
        for (int i = 1; i < points.length; i++) {
            if (!points[0].equals(points[i])) {
                nextNonEqual = i;
                break;
            }
        }
        if (nextNonEqual == -1) {
            // all points are the same
            return convexHull.toArray(new DoubleVector[0]);
        }

        int nextTurn = -1;
        for (int i = nextNonEqual + 1; i < points.length; i++) {
            if (DoubleVector.ccwTurn(points[0], points[nextNonEqual],
                    points[i], eps) != 0) {
                nextTurn = i;
                break;
            }
        }
        if (nextTurn == -1) {
            // all points lie on the same line
            convexHull.push(points[points.length - 1]);
            return convexHull.toArray(new DoubleVector[0]);
        }
        convexHull.push(points[nextTurn - 1]);

        for (int i = nextTurn; i < points.length; i++) {
            DoubleVector top = convexHull.poll();
            while (DoubleVector.ccwTurn(
                    convexHull.peek(), top, points[i],eps) <= 0) {
                top = convexHull.poll();
            }
            convexHull.push(top);
            convexHull.push(points[i]);
        }
        return convexHull.toArray(new DoubleVector[0]);
    }

    public static DoubleVector[] getConvexHull(DoubleVector[] points) {
        return getConvexHull(points, 0.0);
    }

    /**
     * A static factory method that acts just like the {@code new
     * DoubleVector(x, y)} constructor, so that one could add this method
     * to the static import and shorten the declaration of the vector.
     */
    public static DoubleVector dVect(double x, double y) {
        return new DoubleVector(x, y);
    }

    /**
     * A convenience method that retrieves an array of x values from the 
     * array of coordinates. Might be useful when some method accepts
     * arrays of x and y values separately (like the
     * {@link java.awt.Graphics2D#drawPolyline(int[], int[], int)} method).
     */
    public static double[] getX(DoubleVector[] array) {
        double[] x = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            x[i] = array[i].x;
        }
        return x;
    }

    /**
     * Retrieve an array of y values from the array of coordinates.
     */
    public static double[] getY(DoubleVector[] array) {
        double[] y = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            y[i] = array[i].y;
        }
        return y;
    }

    /**
     * Retrieve an array of x values from the array of coordinates and
     * cast them to {@code (int)} values.
     */
    public static int[] getIntX(DoubleVector[] array) {
        int[] x = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            x[i] = (int) array[i].x;
        }
        return x;
    }

    /**
     * Retrieve an array of y values from the array of coordinates and
     * cast them to {@code (int)} values.
     */
    public static int[] getIntY(DoubleVector[] array) {
        int[] y = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            y[i] = (int) array[i].y;
        }
        return y;
    }
}
