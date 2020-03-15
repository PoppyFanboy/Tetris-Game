package poppyfanboy.tetrisgame.graphics;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Queue;

import poppyfanboy.tetrisgame.graphics.AnimationEndHandler.AnimationEndReason;

/**
 * A wrapper-class for the animation and the animations related to this object.
 */
public final class AnimatedObject<T, K extends Enum<K>> {
    private final Class<K> animationTypes;
    private final EnumMap<K, AnimationWrapper<T>> animations;
    private final T object;

    private boolean isIterating = false;
    private Queue<PostponedAction> postponedActions = new ArrayDeque<>();

    public AnimatedObject(T object, Class<K> animationTypes) {
        if (object == null || animationTypes == null) {
            throw new IllegalArgumentException("None of the constructor"
                    + " arguments can be null");
        }
        this.object = object;
        this.animationTypes = animationTypes;
        animations = new EnumMap<>(animationTypes);
    }

    public void tick() {
        isIterating = true;
        Iterator<AnimationWrapper<T>> animationsIterator
                = animations.values().iterator();
        while (animationsIterator.hasNext()) {
            AnimationWrapper<T> animation = animationsIterator.next();
            animation.tick();
            if (animation.finished()) {
                animationsIterator.remove();
            }
        }
        isIterating = false;
        while (!postponedActions.isEmpty()) {
            postponedActions.poll().perform();
        }
    }

    public void perform() {
        animations.values().forEach(AnimationWrapper::perform);
    }

    public void perform(double interpolation) {
        animations.values()
                .forEach(animation -> animation.perform(interpolation));
    }

    /**
     * Adds an animation of the specified type to the object. In case there
     * already was an animation of this type, this method interrupts that old
     * animation and overrides it with a new one.
     */
    public void addAnimation(K animationType, Animation<T> animation) {
        addAnimation(animationType, animation, null);
    }

    /**
     * In case the {@code endHandler} argument is null, does not add a
     * callback to the animation.
     */
    public void addAnimation(K animationType, Animation<T> animation,
            AnimationEndHandler endHandler) {
        if (isIterating) {
            postponedActions.add(getAddAnimationAction(animationType, animation,
                    endHandler));
            return;
        }

        if (animations.containsKey(animationType)) {
            animations.get(animationType)
                    .interrupt(AnimationEndReason.INTERRUPTED_BY_ANIMATION);
        }
        animations.put(animationType,
                endHandler == null
                    ? new AnimationWrapper<>(object, animation)
                    : new AnimationWrapper<>(object, animation, endHandler));
    }

    public void addCallback(K animationType, AnimationEndHandler endHandler) {
        if (!animations.containsKey(animationType)) {
            throw new IllegalArgumentException(String.format("There is no "
                    + "animation of the specified %s type on the list",
                    animationType));
        }
        animations.get(animationType).notifyOnAnimationEnd(endHandler);
    }

    /**
     * Removes the animation from the list without properly finishing it and
     * thus avoiding triggering the callbacks on the animation end.
     */
    public void interruptAnimation(K animationType) {
        if (isIterating) {
            postponedActions.add(getInterruptAnimationAction(animationType));
            return;
        }
        if (animations.containsKey(animationType)) {
            animations.get(animationType)
                    .interrupt(AnimationEndReason.INTERRUPTED);
            animations.remove(animationType);
        }
    }

    public void finishAnimation(K animationType) {
        if (isIterating) {
            postponedActions.add(getFinishAnimationAction(animationType));
        }
        if (animations.containsKey(animationType)) {
            animations.get(animationType)
                    .interrupt(AnimationEndReason.FORCE_FINISHED);
            animations.remove(animationType);
        }
    }

    public Animation<T> getAnimation(K animationType) {
        if (!animations.containsKey(animationType)
                || animations.get(animationType).finished()) {
            return null;
        }
        return animations.get(animationType).getAnimation();
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    private class PostponedAction {
        private final ActionType actionType;
        private final K animationType;

        // these parameters can be set to null in case they are not needed
        private Animation<T> animation;
        private AnimationEndHandler endHandler;

        private PostponedAction(ActionType actionType, K animationType,
                Animation<T> animation, AnimationEndHandler endHandler) {
            this.actionType = actionType;
            this.animationType = animationType;
            this.animation = animation;
            this.endHandler = endHandler;
        }

        public void perform() {
            switch (actionType) {
                case ADD_ANIMATION:
                    addAnimation(animationType, animation);
                    break;
                case ADD_ANIMATION_CALLBACKED:
                    addAnimation(animationType, animation, endHandler);
                    break;
                case INTERRUPT_ANIMATION:
                    interruptAnimation(animationType);
                    break;
                case FINISH_ANIMATION:
                    finishAnimation(animationType);
                    break;
            }
        }
    }

    private enum ActionType {
        ADD_ANIMATION, ADD_ANIMATION_CALLBACKED, ADD_CALLBACK,
        INTERRUPT_ANIMATION, FINISH_ANIMATION
    }

    private PostponedAction getAddAnimationAction(K animationType,
            Animation<T> animation, AnimationEndHandler endHandler) {
        return new PostponedAction(ActionType.ADD_ANIMATION_CALLBACKED,
                animationType, animation, endHandler);
    }

    private PostponedAction getInterruptAnimationAction(K animationType) {
        return new PostponedAction(ActionType.INTERRUPT_ANIMATION,
                animationType, null, null);
    }

    private PostponedAction getFinishAnimationAction(K animationType) {
        return new PostponedAction(ActionType.FINISH_ANIMATION,
                animationType, null, null);
    }
}
