package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.util.DoubleVector;

public class AcceleratedMoveAnimation extends Animation<Animated2D> {
    // acceleration is specified in terms of the blocks as measurement units
    private static final double ACCELERATION = 0.0125;

    private final DoubleVector startCoords, endCoords;
    private final int blockWidth;
    private double initialSpeed;

    public AcceleratedMoveAnimation(DoubleVector startCoords,
            DoubleVector endCoords, double initialSpeed, int blockWidth) {
        this.startCoords = startCoords;
        this.endCoords = endCoords;
        this.initialSpeed = initialSpeed;
        this.blockWidth = blockWidth;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double currentDistance
                = getDistance(currentDuration + interpolation, initialSpeed,
                ACCELERATION * blockWidth);
        if (currentDistance < endCoords.subtract(startCoords).length()) {
            object.setCoords(startCoords.add(endCoords
                    .subtract(startCoords).normalize()
                    .times(currentDistance)));
        } else {
            object.setCoords(endCoords);
        }
    }

    @Override
    public boolean isFinished(int duration) {
        return getDistance(duration, initialSpeed, ACCELERATION * blockWidth)
                >= endCoords.subtract(startCoords).length();
    }

    @Override
    public void finish(Animated2D object) {
        object.setCoords(endCoords);
    }

    // S = v0 * t + a * t^2 / 2
    private static double getDistance(double t, double v0, double a) {
        return v0 * t + a * t * t / 2;
    }
}
