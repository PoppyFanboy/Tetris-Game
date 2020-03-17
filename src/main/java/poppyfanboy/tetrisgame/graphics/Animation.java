package poppyfanboy.tetrisgame.graphics;

public abstract class Animation<AnimatedObject> {
    public abstract void perform(AnimatedObject object, int currentDuration,
            double interpolation);

    public abstract boolean isFinished(int currentDuration);

    public abstract void finish(AnimatedObject object);

    /**
     * Returns {@code true} in case the {@code other} animation cannot
     * start with {@code this} animation still running, so the old animation
     * will be interrupted. Otherwise the animation manager will try to combine
     * the old and the new animations.
     *
     * By default any animations conflicts with any other given animation.
     */
    public boolean conflicts(int thisDuration,
            Animation<AnimatedObject> other) {
        return true;
    }

    /**
     * {@code this} animation tries to affect the {@code other} animation.
     * It is supposed that the {@code other} animation is just about to start.
     * The resulting affected animation is returned. (For example, if both of
     * the animations are rotations, you might want to start the other
     * rotation right where this animation currently is.)
     *
     * Note that for this method to be called the {@code this.conflicts(other)}
     * must return {@code false}.
     */
    public Animation<AnimatedObject> affect(int thisDuration,
            Animation<AnimatedObject> other) {
        return other;
    }
}
