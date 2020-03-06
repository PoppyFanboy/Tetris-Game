package poppyfanboy.tetrisgame.graphics.animation;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.util.DoubleVector;

public class AcceleratedMoveAnimation implements Animation {
    private static final double ACCELERATION = 500.0;

    private final DoubleVector startCoords, endCoords;
    // traversed distance at the current tick
    private double currentDistance;
    private int currentDuration;
    private double initialSpeed;

    private boolean isFinished = false;

    public AcceleratedMoveAnimation(DoubleVector startCoords,
            DoubleVector endCoords, double initialSpeed) {
        this.startCoords = startCoords;
        this.endCoords = endCoords;
        this.initialSpeed = initialSpeed;

        currentDistance = 0;
    }

    @Override
    public void tick() {
        if (!finished()) {
            currentDuration++;
            double t = (1.0 * currentDuration) / Game.TICKS_PER_SECOND;
            // S = vt + at^2
            currentDistance = t * initialSpeed + ACCELERATION * t * t;
            if (currentDistance
                    > endCoords.subtract(startCoords).length()) {
                currentDistance = endCoords.subtract(startCoords).length();
                isFinished = true;
            }
        }
    }

    @Override
    public void perform(Animated object, double interpolation) {
        double t = (currentDuration + interpolation)
                / Game.TICKS_PER_SECOND;
        double currentDistance = t * initialSpeed + ACCELERATION * t * t;
        if (currentDistance > endCoords.subtract(startCoords).length()) {
            currentDistance = endCoords.subtract(startCoords).length();
        }
        object.setCoords(startCoords.add(
                endCoords.subtract(startCoords).normalize()
                .times(currentDistance)));
    }

    @Override
    public void perform(Animated object) {
        perform(object, 0.0);
    }

    @Override
    public boolean finished() {
        return isFinished;
    }

    @Override
    public int timeLeft() {
        // implementation placeholder
        return 0;
    }
}
