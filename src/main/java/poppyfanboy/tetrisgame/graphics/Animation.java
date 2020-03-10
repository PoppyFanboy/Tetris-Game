package poppyfanboy.tetrisgame.graphics;

public interface Animation<AnimatedObject> {
    void tick();
    void perform(AnimatedObject object, double interpolation);
    void perform(AnimatedObject object);
    boolean finished();
    void finish(AnimatedObject object);
    int timeLeft();
}
