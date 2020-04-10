package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

public class OpacityAnimation extends Animation<Animated2D> {
    private final int duration;
    private final double startOpacity, endOpacity;

    public OpacityAnimation(double startOpacity,
            double endOpacity, int duration) {
        this.startOpacity = startOpacity;
        this.endOpacity = endOpacity;
        this.duration = duration;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        object.setOpacity(
                startOpacity * (1 - progress) + endOpacity * progress);
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setOpacity(endOpacity);
    }
}
