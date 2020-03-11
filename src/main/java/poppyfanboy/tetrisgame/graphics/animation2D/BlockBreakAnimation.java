package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.graphics.Animation;

/**
 * Animation that is used for breaking the blocks in the full filled line.
 */
public class BlockBreakAnimation extends Animation<Animated2D> {
    private static final double FINAL_ROTATION_ANGLE = -Math.PI / 3;
    private static final double SCALE_COEFFICIENT = 0.3;

    private final double startScale, startAngle;
    private int duration;

    public BlockBreakAnimation(double startAngle, double startScale,
            int duration) {
        this.duration = duration;
        this.startAngle = startAngle;
        this.startScale = startScale;
    }

    @Override
    public void perform(Animated2D object, int currentDuration,
            double interpolation) {
        double progress = duration == 0
                ? 1.0
                : (currentDuration + interpolation) / duration;
        object.setOpacity(1.0 - progress);
        object.setScale(startScale
                + progress * (SCALE_COEFFICIENT * startScale - startScale));
        object.setRotationAngle(startAngle + progress * FINAL_ROTATION_ANGLE);
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated2D object) {
        object.setOpacity(0.0);
        object.setScale(SCALE_COEFFICIENT * startAngle);
        object.setRotationAngle(startAngle + FINAL_ROTATION_ANGLE);
    }
}
