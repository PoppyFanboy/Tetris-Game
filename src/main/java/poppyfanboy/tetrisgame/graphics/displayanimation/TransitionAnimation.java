package poppyfanboy.tetrisgame.graphics.displayanimation;

import poppyfanboy.tetrisgame.graphics.Animation;

public class TransitionAnimation extends Animation<AnimatedDisplay> {
    private final int duration;
    private final double startProgress;

    public TransitionAnimation(int duration) {
        this(duration, 0);
    }

    public TransitionAnimation(int duration, double startProgress) {
        this.duration = duration;
        this.startProgress = startProgress;
    }

    @Override
    public void perform(AnimatedDisplay object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : ((currentDuration + interpolation) / duration);
        object.setTransitionProgress(startProgress
                + progress * (1 - startProgress));

    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(AnimatedDisplay object) {
        object.setTransitionProgress(1.0);
    }

    @Override
    public Animation<AnimatedDisplay> affect(int thisDuration,
            Animation<AnimatedDisplay> other) {
        if (!(other instanceof TransitionAnimation)) {
            return other;
        }
        double currentProgress = startProgress
                + (1 - startProgress) * ((double) thisDuration / duration);
        return new TransitionAnimation(
                (int) ((1 - currentProgress) * duration), currentProgress);
    }

    @Override
    public boolean conflicts(int thisDuration,
            Animation<AnimatedDisplay> other) {
        return false;
    }
}
