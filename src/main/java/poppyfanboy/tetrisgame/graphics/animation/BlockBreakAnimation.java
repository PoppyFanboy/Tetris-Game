package poppyfanboy.tetrisgame.graphics.animation;

/**
 * Animation that is used for breaking the blocks in the full filled line.
 */
public class BlockBreakAnimation implements Animation {
    private static final double FINAL_ROTATION_ANGLE = -Math.PI / 3;
    private static final double FINAL_SCALE = 0.4;

    private int duration;
    private int currentDuration = 0;

    public BlockBreakAnimation(int duration) {
        this.duration = duration;
    }

    @Override
    public void tick() {
        if (!finished()) {
            currentDuration++;
        }
    }

    @Override
    public void perform(Animated object, double interpolation) {
        double progress = (currentDuration + interpolation) / duration;
        object.setOpacity(1.0 - progress);
        object.setRotationAngle(FINAL_ROTATION_ANGLE * progress);
        double newScale = object.getScale()
                + (FINAL_SCALE - object.getScale()) * progress;
        object.setScale(newScale);
    }

    @Override
    public void perform(Animated object) {
        perform(object, 0.0);
    }

    @Override
    public boolean finished() {
        return currentDuration >= duration;
    }

    @Override
    public void finish(Animated object) {
        currentDuration = duration;
        perform(object);
    }

    @Override
    public int timeLeft() {
        return Math.max(0, duration - currentDuration);
    }
}
