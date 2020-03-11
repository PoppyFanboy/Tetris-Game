package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.util.DoubleVector;

import static java.lang.Math.min;

public class MoveAnimation extends Animation<Animated2D> {
    private final int duration;
    private final DoubleVector startCoords, endCoords;

    /**
     * Creates an animation instance that linearly moves the object from
     * the point {@code (fromX, fromY)} to the point {@code (toX, toY)} on
     * the screen.
     *
     * Since the length of the distance between these two
     * points might vary, we introduce the {@code defaultShift}
     * argument which define the smallest distance
     * between two points which is traversed in {@code duration} time.
     * If the points are farther away from one another the animation will
     * still take the specified time to complete, otherwise the duration
     * is shortened proportional to how close are the "to" and "from"
     * points.
     */
    public MoveAnimation(DoubleVector startCoords, DoubleVector endCoords,
            int duration, double defaultShift) {
        double distance = endCoords.subtract(startCoords).length();
        this.duration
            = (int) (min(distance / defaultShift, 1.0) * duration);
        this.startCoords = startCoords;
        this.endCoords = endCoords;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = (currentDuration + interpolation) / duration;

        DoubleVector currCoords = startCoords.times(1 - progress)
                .add(endCoords.times(progress));
        object.setCoords(currCoords);
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setCoords(endCoords);
    }
}
