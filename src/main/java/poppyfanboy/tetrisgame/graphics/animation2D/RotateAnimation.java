package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.max;

public class RotateAnimation implements Animation {
    private final Animated2D object;
    private final double startAngle, endAngle;
    private final int duration;

    private int currentDuration;
    private boolean clockwise;

    /**
     * {@code defaultAngle} is the default angle at which the
     * entity is rotated. For more information see {@link MoveAnimation}
     * class constructor.
     */
    public RotateAnimation(Animated2D object,
            double startAngle, double endAngle,
            int duration, double defaultAngle, boolean clockwise) {
        double progress = abs(endAngle - startAngle) / defaultAngle;
        this.duration = (int) max((min(progress, 1.0) * duration), 1);
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.clockwise = clockwise;
        this.object = object;
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

    @Override
    public void tick() {
        if (!finished()) {
            currentDuration++;
        }
    }

    @Override
    public void perform(double interpolation) {
        double progress = (currentDuration + interpolation) / duration;
        double angle = startAngle + (endAngle - startAngle) * progress;
        object.setRotationAngle(angle);
    }

    @Override
    public void perform() {
        perform(0.0);
    }

    @Override
    public boolean finished() {
        return currentDuration >= duration;
    }

    @Override
    public int timeLeft() {
        return Math.max(0, duration - currentDuration);
    }

    @Override
    public void finish() {
        currentDuration = duration;
        perform();
    }
}
