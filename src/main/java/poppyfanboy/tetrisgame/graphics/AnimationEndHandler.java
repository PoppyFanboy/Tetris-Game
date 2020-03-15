package poppyfanboy.tetrisgame.graphics;

@FunctionalInterface
public interface AnimationEndHandler {
    void handleAnimationEnd(AnimationEndReason reason);

    enum AnimationEndReason {
        PROPERLY_FINISHED, FORCE_FINISHED, INTERRUPTED,
        INTERRUPTED_BY_ANIMATION;

        public boolean interrupted() {
            return this == INTERRUPTED || this == INTERRUPTED_BY_ANIMATION;
        }

        public boolean finished() {
            return !interrupted();
        }
    }
}
