package poppyfanboy.tetrisgame.graphics.animation2D;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.util.Rotation;

public class RotationAnimation extends Animation<Animated2D> {
    private final double startAngle, endAngle;
    private final double defaultAngle;
    private final int defaultDuration;

    private int duration;

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
                currentDuration, duration, interpolation));
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
                thisDuration, duration, 0.0);
        double newStart = currentAngle;
        double newEnd = this.endAngle
                + (otherAnimation.endAngle - otherAnimation.startAngle);

        if (this.startAngle >= this.endAngle
                == otherAnimation.startAngle >= otherAnimation.endAngle) {
            return new RotationAnimation(newStart, newEnd,
                this.startAngle >= this.endAngle,
                otherAnimation.defaultDuration,
                this.defaultAngle + otherAnimation.defaultAngle
                - Math.abs(Rotation.normalizeAngle(currentAngle - startAngle)));
        } else {
            newStart = Rotation.normalizeAngle(newStart);
            newEnd = Rotation.normalizeAngle(newEnd);
            double newDelta = Rotation.normalizeAngle(newEnd - newStart);
            return new RotationAnimation(newStart, newStart + newDelta,
                    newDelta < 0, defaultDuration, defaultAngle);
        }
    }

    @Override
    public boolean conflicts(int thisDuration, Animation<Animated2D> other) {
        return false;
    }

    private static double getCurrentAngle(double startAngle, double endAngle,
            int currentDuration, int duration, double interpolation) {
        double progress = duration == 0
                ? 1.0
                : Math.min((currentDuration + interpolation) / duration, 1.0);
        return startAngle + (endAngle - startAngle) * progress;
    }
}
