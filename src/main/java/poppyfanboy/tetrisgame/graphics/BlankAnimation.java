package poppyfanboy.tetrisgame.graphics;

public class BlankAnimation<E> extends Animation<E> {
    private final int duration;

    public BlankAnimation(int duration) {
        this.duration = duration;
    }

    @Override
    public void perform(E object, int currentDuration, double interpolation) {
        // do nothing
    }

    @Override
    public boolean isFinished(int currentDuration) {
        return currentDuration >= duration;
    }

    @Override
    public void finish(E object) {
        // do nothing
    }
}
