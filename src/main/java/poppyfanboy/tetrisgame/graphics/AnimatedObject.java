package poppyfanboy.tetrisgame.graphics;

import java.util.LinkedList;

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
    private LinkedList<Callback> callbacks;

    public AnimatedObject(T object, Animation<T> animation) {
        this.animation = animation;
        this.object = object;
    }

    public void tick() {
        if (!isFinished) {
            duration++;
        } else {
            return;
        }
        if (animation.isFinished(duration)) {
            isFinished = true;
            for (Callback callback : callbacks) {
                callback.trigger();
            }
        }
    }

    public void notifyOnEnd(CallbackHandler callback,
                            String callbackArgument) {
        if (callbacks == null) {
            callbacks = new LinkedList<>();
        }
        callbacks.add(new Callback(callback, callbackArgument));
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

    public interface CallbackHandler {
        void handleAnimationEnd(String argument);
    }

    private static class Callback {
        private CallbackHandler method;
        private String argument;

        Callback(CallbackHandler method, String argument) {
            this.method = method;
            this.argument = argument;
        }

        public void trigger() {
            method.handleAnimationEnd(argument);
        }
    }
}
