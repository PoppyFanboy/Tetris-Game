package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.max;
import poppyfanboy.tetrisgame.util.Rotation;

public class RotationAnimation extends Animation<Animated2D> {
    private final double startAngle, endAngle;
    private final int defaultDuration;
    private final double defaultAngle;

    private int duration;
    private final boolean isClockwise;

    /**
     * {@code defaultAngle} is the default angle at which the
     * entity is rotated. For more information see {@link MoveAnimation}
     * class constructor.
     */
    public RotationAnimation(double startAngle, double endAngle,
            boolean isClockwise, int duration, double defaultAngle) {
        double progress = abs(endAngle - startAngle) / defaultAngle;
        this.defaultDuration = duration;
        this.defaultAngle = defaultAngle;
        this.duration = (int) max((min(progress, 1.0) * duration), 1);
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.isClockwise = isClockwise;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getEndAngle() {
        return endAngle;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        object.setRotationAngle(getCurrentAngle(startAngle, endAngle,
                isClockwise, currentDuration, duration, interpolation));
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setRotationAngle(endAngle);
    }

    /**
     * In case the {@code other} rotation has the same direction as this
     * rotation, prolongs the current rotation by the final angle of the
     * {@code other} rotation. Otherwise returns a rotation animation in the
     * same direction as the {@code other} is, with the closest {@code PI /
     * 2} multiple as the ending angle and the current angle (according to the
     * {@code currentDuration} argument) as the starting angle.
     */
    @Override
    public Animation<Animated2D> affect(int thisDuration,
            Animation<Animated2D> other) {
        // but in general the other animation does not necessarily has to be of
        // class RotateAnimation
        if (!(other instanceof RotationAnimation)) {
            return other;
        }
        RotationAnimation otherAnimation = (RotationAnimation) other;
        double currentAngle = getCurrentAngle(this.startAngle, this.endAngle,
                this.isClockwise, thisDuration, defaultDuration, 0.0);

        if (this.isClockwise == otherAnimation.isClockwise) {
            return new RotationAnimation(currentAngle,
                    otherAnimation.endAngle, isClockwise,
                    otherAnimation.defaultDuration,
                    otherAnimation.defaultAngle);
        } else {
            return new RotationAnimation(currentAngle,
                    otherAnimation.endAngle, !isClockwise,
                    otherAnimation.defaultDuration,
                    otherAnimation.defaultAngle);
        }
    }

    @Override
    public boolean conflicts(int thisDuration, Animation<Animated2D> other) {
        return false;
    }

    private static double getCurrentAngle(double startAngle, double endAngle,
            boolean isClockwise, int currentDuration, int duration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        double nonNormalized = !isClockwise && endAngle >= startAngle
                || isClockwise && endAngle <= startAngle
            ? startAngle + (endAngle - startAngle) * progress
            : startAngle
                + (2 * Math.PI - Math.abs(endAngle - startAngle)) * progress;

        return Rotation.normalizeAngle(nonNormalized);
    }
}
