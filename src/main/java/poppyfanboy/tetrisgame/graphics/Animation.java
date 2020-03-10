package poppyfanboy.tetrisgame.graphics;

/**
 * Animated object is expected to be embedded into the animation itself.
 * *Not doing so* could make sense if the animations were something more
 * complicated, and the same animations could be applied to different
 * objects.
 */
public interface Animation {
    void tick();
    void perform(double interpolation);
    void perform();
    boolean finished();
    void finish();
    int timeLeft();
}
