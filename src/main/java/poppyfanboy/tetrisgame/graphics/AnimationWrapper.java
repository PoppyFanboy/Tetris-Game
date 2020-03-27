package poppyfanboy.tetrisgame.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import poppyfanboy.tetrisgame.graphics.AnimationEndHandler.AnimationEndReason;

/**
 * A wrapper class for the animation, an object related to it and the
 * callbacks that are triggered when the animation ends.
 */
public class AnimationWrapper<T> {
    private final Animation<T> animation;
    private final T object;

    private int duration = 0;
    // might not have the same value as animation.isFinished(duration)
    private boolean isFinished;
    private List<AnimationEndHandler> endHandlers;

    public AnimationWrapper(T object, Animation<T> animation) {
        if (object == null || animation == null) {
            throw new IllegalArgumentException("Neither animation, nor"
                    + " animated objects can be null.");
        }
        this.animation = animation;
        this.object = object;
    }

    public AnimationWrapper(T object, Animation<T> animation,
            AnimationEndHandler endHandler) {
        if (object == null || animation == null || endHandler == null) {
            throw new IllegalArgumentException("One of the passed arguments"
                    + " is null.");
        }
        this.animation = animation;
        this.object = object;
        endHandlers = new ArrayList<>(Collections.singleton(endHandler));
    }

    private AnimationWrapper(T object, Animation<T> animation,
            boolean isFinished, List<AnimationEndHandler> endHandlers) {
        this.animation = animation;
        this.object = object;
        this.endHandlers = endHandlers;
        this.isFinished = isFinished;
    }

    public void tick() {
        if (isFinished) {
            return;
        }
        duration++;
        if (animation.isFinished(duration)) {
            animation.finish(object);
            isFinished = true;
            if (endHandlers != null) {
                endHandlers.forEach(endHandler -> endHandler.handleAnimationEnd(
                        AnimationEndReason.PROPERLY_FINISHED));
            }
        }
    }

    public void tempFastForward(double ticksCount) {
        animation.perform(object, duration + (int) ticksCount,
                ticksCount - ((int) ticksCount));
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
        if (!isFinished) {
            isFinished = true;
            if (endHandlers != null) {
                endHandlers.forEach(endHandler -> endHandler
                        .handleAnimationEnd(AnimationEndReason.FORCE_FINISHED));
            }
        }
    }

    /**
     * @param   reason can only be either {@code INTERRUPTED} or {@code
     * INTERRUPTED_BY_ANIMATION}. Otherwise an {@code
     * IllegalArgumentException} is thrown.
     */
    public void interrupt(AnimationEndReason reason) {
        if (reason != AnimationEndReason.INTERRUPTED
                && reason != AnimationEndReason.INTERRUPTED_BY_ANIMATION) {
            throw new IllegalArgumentException("Unexpected reason passed.");
        }
        if (!isFinished) {
            isFinished = true;
            if (endHandlers != null) {
                endHandlers.forEach(endHandler -> endHandler.
                        handleAnimationEnd(AnimationEndReason.INTERRUPTED));
            }
        }
    }

    public void notifyOnAnimationEnd(AnimationEndHandler endHandler) {
        if (endHandler == null) {
            throw new IllegalArgumentException("Animation end handlers cannot"
                    + " be null");
        }
        if (endHandlers == null) {
            endHandlers = new ArrayList<>();
        }
        endHandlers.add(endHandler);
    }

    public Animation<T> getAnimation() {
        return animation;
    }

    public boolean conflicts(AnimationWrapper<T> other) {
        return this.animation.conflicts(this.duration, other.animation);
    }

    public AnimationWrapper<T> affect(AnimationWrapper<T> other) {
        if (this.object != other.object) {
            return other;
        }
        Animation<T> affectedAnimation = this.animation.affect(this.duration,
                other.animation);
        List<AnimationEndHandler> newEndHandlers = new ArrayList<>();
        if (!this.isFinished && this.endHandlers != null) {
            newEndHandlers.addAll(this.endHandlers);
        }
        if (!other.isFinished && other.endHandlers != null) {
            newEndHandlers.addAll(other.endHandlers);
        }
        return new AnimationWrapper<>(object, affectedAnimation,
                other.isFinished, newEndHandlers);
    }


    public T getObject() {
        return object;
    }
}
