package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.max;

public class RotateAnimation extends Animation<Animated2D> {
    private final double startAngle, endAngle;
    private final int duration;
    private boolean clockwise;

    /**
     * {@code defaultAngle} is the default angle at which the
     * entity is rotated. For more information see {@link MoveAnimation}
     * class constructor.
     */
    public RotateAnimation(double startAngle, double endAngle,
            int duration, double defaultAngle, boolean clockwise) {
        double progress = abs(endAngle - startAngle) / defaultAngle;
        this.duration = (int) max((min(progress, 1.0) * duration), 1);
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.clockwise = clockwise;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = (currentDuration + interpolation) / duration;
        double angle = startAngle + (endAngle - startAngle) * progress;
        object.setRotationAngle(angle);
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setRotationAngle(endAngle);
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getEndAngle() {
        return endAngle;
    }

    public boolean isClockwise() {
        return clockwise;
    }
}
