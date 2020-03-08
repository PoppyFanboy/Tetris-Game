package poppyfanboy.tetrisgame.graphics.animation;

import poppyfanboy.tetrisgame.util.DoubleVector;

public class AcceleratedMoveAnimation implements Animation {
    private static final double ACCELERATION = 0.1;

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
            double t = currentDuration;
            // S = vt + at^2 / 2
            currentDistance = t * initialSpeed + ACCELERATION * t * t / 2;
            if (currentDistance
                    > endCoords.subtract(startCoords).length()) {
                currentDistance = endCoords.subtract(startCoords).length();
                isFinished = true;
            }
        }
    }

    @Override
    public void perform(Animated object, double interpolation) {
        double t = currentDuration + interpolation;
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
        double distanceLeft = endCoords.subtract(startCoords).length()
                - currentDistance;
        if (distanceLeft <= 0) {
            return 0;
        } else {
            return (int) Math.ceil(
                    (-initialSpeed
                    + Math.sqrt(
                        initialSpeed * initialSpeed
                        + 8 * ACCELERATION * distanceLeft))
                    / (2 * ACCELERATION));
        }
    }

    @Override
    public void finish(Animated object) {
        currentDuration += timeLeft() + 1;
        currentDistance = endCoords.subtract(startCoords).length();
        isFinished = true;
        perform(object);
    }
}
