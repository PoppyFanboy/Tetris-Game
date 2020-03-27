package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.util.DoubleVector;

import static java.lang.Math.min;

/**
 * An animation that either moves the object vertically without
 * changing the y coordinates or moves the object horizontally
 * not changing the x coordinate as well.
 */
public class HVLinearAnimation extends Animation<Animated2D> {
    private final double startCoords, endCoords;
    private final int defaultDuration;
    private final double defaultShift;

    private int duration;
    private boolean isHorizontal;

    /**
     * @param   isHorizontal {@code true} for the horizontal movement,
     *          {@code false} for the vertical movement.
     */
    private HVLinearAnimation(double startCoords, double endCoords,
            int duration, double defaultShift, boolean isHorizontal) {
        this.startCoords = startCoords;
        this.endCoords = endCoords;
        this.isHorizontal = isHorizontal;
        this.defaultShift = defaultShift;
        this.defaultDuration = duration;

        double distance = Math.abs(endCoords - startCoords);
        this.duration
                = (int) (min(distance / defaultShift, 1.0) * duration);
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double currCoords = getCurrentCoords(startCoords, endCoords,
                currentDuration, duration, interpolation);
        if (isHorizontal) {
            // y coordinate remains unchanged
            object.setCoords(new DoubleVector(
                    currCoords, object.getCoords().getY()));
        } else {
            object.setCoords(new DoubleVector(
                    object.getCoords().getX(), currCoords));
        }
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        if (isHorizontal) {
            object.setCoords(
                    new DoubleVector(endCoords, object.getCoords().getY()));
        } else {
            object.setCoords(
                    new DoubleVector(object.getCoords().getX(), endCoords));
        }
    }

    @Override
    public Animation<Animated2D> affect(int thisDuration,
            Animation<Animated2D> other) {
        if (!(other instanceof HVLinearAnimation)) {
            return other;
        }
        HVLinearAnimation otherAnimation = (HVLinearAnimation) other;

        if (this.isHorizontal != otherAnimation.isHorizontal
                || this.defaultShift != otherAnimation.defaultShift) {
            return otherAnimation;
        }

        double currentCoords = getCurrentCoords(this.startCoords,
                this.endCoords, thisDuration, this.duration, 0.0);
        return new HVLinearAnimation(currentCoords, otherAnimation.endCoords,
                otherAnimation.defaultDuration, defaultShift, isHorizontal);
    }

    @Override
    public boolean conflicts(int thisDuration, Animation<Animated2D> other) {
        return false;
    }

    public static HVLinearAnimation getHorizontalAnimation(
            double startCoords, double endCoords, int duration,
            double defaultShift) {
        return new HVLinearAnimation(startCoords, endCoords,
                duration, defaultShift, true);
    }

    public static HVLinearAnimation getVerticalAnimation(
            double startCoords, double endCoords, int duration,
            double defaultShift) {
        return new HVLinearAnimation(startCoords, endCoords,
                duration, defaultShift, false);
    }

    private static double getCurrentCoords(double startCoords,
            double endCoords, int currentDuration, int duration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : Math.min((currentDuration + interpolation) / duration, 1.0);
        return startCoords * (1 - progress) + endCoords * progress;
    }
}
