package poppyfanboy.tetrisgame.graphics;

/**
 * A wrapper-class for the animation and the animated object. This class
 * is also provided with the ability to assign a callback to the animation,
 * that is triggered when the animation ends.
 */
public final class AnimatedObject<T> {
    private final Animation<T> animation;
    private final T object;

    private int duration = 0;
    private boolean isFinished = false;

    public AnimatedObject(T object, Animation<T> animation) {
        this.animation = animation;
        this.object = object;
    }

    public Animation<T> getAnimation() {
        return animation;
    }

    public void tick() {
        if (!isFinished) {
            duration++;
        } else {
            return;
        }
        if (animation.isFinished(duration)) {
            isFinished = true;
        }
    }

    public void perform() {
        animation.perform(object, duration, 0.0);
    }

    public void perform(double interpolation) {
        animation.perform(object, duration, interpolation);
    }

    public boolean finished() {
        return isFinished;
    }

    public void finish() {
        animation.finish(object);
        isFinished = true;
    }
}
