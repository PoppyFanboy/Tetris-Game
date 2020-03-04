package poppyfanboy.tetrisgame.util;

import static java.lang.Math.PI;
import static java.lang.Math.floor;

/**
 * Represents the rotation of a figure in the game.
 * {@code LEFT} corresponds to the 90 degree counter-clockwise rotation
 * relative to the initial rotation of the figure, {@code RIGHT}
 * corresponds to the 90 degree clockwise rotation, {@code UPSIDE_DOWN}
 * means that the figure rotated by 180 degree,
 * {@code INITIAL} corresponds to the initial rotation of the figure.
 *
 * This very class can also be used to represent the <i>direction</i> of
 * the rotation, not the <i>position</i> of something relative to its
 * initial position.
 *
 *
 */
public enum Rotation {
    // rotations are ordered in a clockwise order
    INITIAL, RIGHT, UPSIDE_DOWN, LEFT;
    
    /**
     * Adds to rotations of the figure and returns the result.
     * For example, {@code RIGHT.add(UPSIDE_DOWN)} will return
     * {@code LEFT}.
     */
    public Rotation add(Rotation rotation) {
        return values()[(this.ordinal() + rotation.ordinal()) % 4];
    }

    /**
     * Returns a radian angle between the initial rotation and this
     * rotation (from the first rotation to the second). The returned
     * value lies on the [-PI, PI) interval.
     */
    public double getAngle() {
        return normalizeAngle(ordinal() * PI / 2);
    }

    /**
     * Returns a radian angle between the two given rotations (from the
     * first to the second).
     */
    public static double getAngle(Rotation from, Rotation to) {
        return getAngle(from.getAngle(), to.getAngle());
    }

    /**
     * Returns a signed angle between the two given radian angles.
     */
    public static double getAngle(double from, double to) {
        return normalizeAngle(to - from);
    }

    /**
     * Given the angle in radians normalizes it to the value from the
     * interval [-PI, PI).
     */
    public static double normalizeAngle(double angle) {
        return floatMod(angle + PI, 2 * PI) - PI;
    }

    /**
     * Returns such smallest non-negative value {@code value} such that
     * {@code x - value} is divisible by {@code y}, assuming that the
     * {@code y} parameter is non-negative.
     */
    private static double floatMod(double x, double y) {
        assert y > 0;

        // works both for negative and positive x's
        return x - y * floor(x / y);
    }
}
