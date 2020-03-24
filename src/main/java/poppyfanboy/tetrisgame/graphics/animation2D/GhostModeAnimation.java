package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

public class GhostModeAnimation extends Animation<Animated2D> {
    private int duration;

    public GhostModeAnimation(int duration) {
        this.duration = duration;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        object.setScale(3 * progress * progress - 3 * progress + 1);
        object.setBrightness(-4 * progress * progress + 4 * progress);
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setScale(1);
        object.setBrightness(0);
    }
}
