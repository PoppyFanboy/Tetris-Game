package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

public class DisappearanceAnimation extends Animation<Animated2D> {
    private final int duration;
    private final double startOpacity;

    public DisappearanceAnimation(double startOpacity, int duration) {
        this.startOpacity = startOpacity;
        this.duration = duration;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        object.setOpacity(startOpacity * (1 - progress));
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setOpacity(0);
    }
}
