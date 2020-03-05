package poppyfanboy.tetrisgame.graphics.animation;

import poppyfanboy.tetrisgame.util.DoubleVector;

import static java.lang.Math.min;

public class MoveAnimation implements Animation {
    private final int duration;
    private final DoubleVector startCoords, endCoords;
    private int currentDuration;

    /**
     * Creates an animation instance that linearly moves the object from
     * the point {@code (fromX, fromY)} to the point {@code (toX, toY)} on
     * the screen.
     *
     * Since the length of the distance between these two
     * points might vary, we introduce the {@code defaultShiftDX} and
     * {@code defaultShiftDY} arguments which define the smallest distance
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
        currentDuration = 0;
    }

    @Override
    public void tick() {
        if (!finished()) {
            currentDuration++;
        }
    }

    @Override
    public void perform(Animated object, double interpolation) {
        double progress = (currentDuration + interpolation) / duration;

        DoubleVector currCoords = startCoords.times(1 - progress)
                .add(endCoords.times(progress));
        object.setCoords(currCoords);
    }

    @Override
    public void perform(Animated object) {
        perform(object, 0.0);
    }

    @Override
    public boolean finished() {
        return currentDuration >= duration;
    }

    @Override
    public int timeLeft() {
        return Math.max(0, duration - currentDuration);
    }
}
