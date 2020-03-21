package poppyfanboy.tetrisgame.graphics.displayanimation;

import poppyfanboy.tetrisgame.graphics.Animation;

public class TransitionAnimation extends Animation<AnimatedDisplay> {
    private final int duration;

    public TransitionAnimation(int duration) {
        this.duration = duration;
    }

    @Override
    public void perform(AnimatedDisplay object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        object.setTransitionProgress(progress);
        object.setDistortionProgress(progress);
        object.setNoiseDensity(-0.4 * progress * progress + 0.4 * progress);
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(AnimatedDisplay object) {
        object.setTransitionProgress(1.0);
        object.setDistortionProgress(1.0);
        object.setNoiseDensity(0.0);
    }
}
