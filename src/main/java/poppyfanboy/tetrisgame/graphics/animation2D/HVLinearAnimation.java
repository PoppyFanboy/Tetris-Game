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

        double distance = Math.abs(endCoords - startCoords);
        this.duration
                = (int) (min(distance / defaultShift, 1.0) * duration);
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        double currCoords
                = startCoords * (1 - progress) + endCoords * progress;
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

    public HVLinearAnimation changeDuration(int newDuration,
            double defaultShift) {
        return new HVLinearAnimation(startCoords, endCoords, newDuration,
                defaultShift, isHorizontal);
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
}
