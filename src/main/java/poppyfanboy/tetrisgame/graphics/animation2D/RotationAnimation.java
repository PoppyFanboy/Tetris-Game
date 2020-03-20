package poppyfanboy.tetrisgame.graphics.animation2D;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import poppyfanboy.tetrisgame.graphics.Animation;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.max;
import poppyfanboy.tetrisgame.util.Rotation;

public class RotationAnimation extends Animation<Animated2D> {
    private final double startAngle, endAngle;

    private int duration;
    private final boolean isClockwise;
    private final double defaultAngle;
    private final int defaultDuration;

    /**
     * {@code defaultAngle} is the default angle at which the
     * entity is rotated. For more information see {@link MoveAnimation}
     * class constructor.
     */
    public RotationAnimation(double startAngle, double endAngle,
            boolean isClockwise, int duration, double defaultAngle) {
        double progress = abs(endAngle - startAngle) / defaultAngle;
        this.defaultDuration = duration;
        this.duration = (int) max((min(progress, 1.0) * duration), 1);
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.isClockwise = isClockwise;
        this.defaultAngle = defaultAngle;
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

        double newStart = currentAngle;
        double newEnd = this.endAngle
                + (otherAnimation.endAngle - otherAnimation.startAngle);

        if (this.isClockwise == otherAnimation.isClockwise) {
            return new RotationAnimation(newStart, newEnd, isClockwise,
                otherAnimation.defaultDuration,
                this.defaultAngle + otherAnimation.defaultAngle
                        - Math.abs(currentAngle - startAngle));
        } else {
            newStart = Rotation.normalizeAngle(newStart);
            newEnd = Rotation.normalizeAngle(newEnd);

            // rotate to the closest angle
            if (!otherAnimation.isClockwise && Math.abs(newEnd - newStart)
                    > Math.abs(newStart - newEnd - 2 * Math.PI)) {
                newEnd += 2 * Math.PI;
            }
            if (otherAnimation.isClockwise && Math.abs(newEnd - newStart)
                    > Math.abs(newStart - newEnd + 2 * Math.PI)) {
                newEnd -= 2 * Math.PI;
            }

            if (cos(newStart) * sin(newEnd) - cos(newEnd) * sin(newStart) > 0) {
                return new RotationAnimation(newStart, newEnd,
                        !isClockwise, defaultDuration, defaultAngle);
            } else {
                return new RotationAnimation(newStart, newEnd,
                        isClockwise, defaultDuration, defaultAngle);
            }
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
        return startAngle + (endAngle - startAngle) * progress;
    }
}
