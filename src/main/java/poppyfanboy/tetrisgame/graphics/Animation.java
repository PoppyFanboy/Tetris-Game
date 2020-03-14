package poppyfanboy.tetrisgame.graphics;

public abstract class Animation<AnimatedObject> {
    public abstract void perform(AnimatedObject object, int currentDuration,
            double interpolation);

    public abstract boolean isFinished(int currentDuration);

    public abstract void finish(AnimatedObject object);

    public boolean conflicts(Animation<AnimatedObject> other) {
        return false;
    }
}
