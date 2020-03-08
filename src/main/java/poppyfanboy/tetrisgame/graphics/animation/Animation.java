package poppyfanboy.tetrisgame.graphics.animation;

public interface Animation {
    void tick();
    void perform(Animated object, double interpolation);
    void perform(Animated object);
    boolean finished();
    void finish(Animated object);
    int timeLeft();
}
