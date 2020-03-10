package poppyfanboy.tetrisgame.graphics.animation2D;

import static java.lang.Math.min;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.util.DoubleVector;

/**
 * An animation that either moves the object vertically without
 * changing the y coordinates or moves the object horizontally
 * not changing the x coordinate as well.
 */
public class HVLinearAnimation implements Animation {
    private final Animated2D object;
    private final double startCoords, endCoords;

    private int duration;
    private int currentDuration;
    private boolean isHorizontal;

    /**
     * @param   isHorizontal {@code true} for the horizontal movement,
     *          {@code false} for the vertical movement.
     */
    private HVLinearAnimation(Animated2D object,
            double startCoords, double endCoords,
            int duration, double defaultShift, boolean isHorizontal) {
        this.object = object;
        this.startCoords = startCoords;
        this.endCoords = endCoords;
        this.isHorizontal = isHorizontal;

        double distance = Math.abs(endCoords - startCoords);
        this.duration
                = (int) (min(distance / defaultShift, 1.0) * duration);
        currentDuration = 0;
    }

    public void changeDuration(int newDuration) {
        currentDuration = (int) Math.round(
                (1.0 * currentDuration / duration) * newDuration);
        duration = newDuration;
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

    public static HVLinearAnimation getHorizontalAnimation(Animated2D object,
            double startCoords, double endCoords, int duration,
            double defaultShift) {
        return new HVLinearAnimation(object, startCoords, endCoords,
                duration, defaultShift, true);
    }

    public static HVLinearAnimation getVerticalAnimation(Animated2D object,
            double startCoords, double endCoords, int duration,
            double defaultShift) {
        return new HVLinearAnimation(object, startCoords, endCoords,
                duration, defaultShift, false);
    }
}
