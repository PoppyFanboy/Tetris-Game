package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

import static java.lang.Math.min;
import static java.lang.Math.max;

public class GhostModeAnimation extends Animation<Animated2D> {
    private final double startProgress;
    private final int duration;

    public GhostModeAnimation(int duration) {
        this.duration = duration;
        startProgress = 0;
    }

    public GhostModeAnimation(int duration, double startProgress) {
        this.duration = duration;
        this.startProgress = max(min(startProgress, 1), 0);
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : min(max((currentDuration + interpolation) / duration,
                        startProgress), 1);
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

    @Override
    public Animation<Animated2D> affect(int thisDuration,
            Animation<Animated2D> other) {
        if (!(other instanceof GhostModeAnimation)) {
            return other;
        }
        GhostModeAnimation otherAnimation = (GhostModeAnimation) other;
        double currentProgress = duration == 0
                ? 1.0
                : min(max((double) thisDuration / duration, startProgress), 1);
        return new GhostModeAnimation(otherAnimation.duration, currentProgress);
    }

    @Override
    public boolean conflicts(int thisDuration, Animation<Animated2D> other) {
        return !(other instanceof GhostModeAnimation);
    }
}
