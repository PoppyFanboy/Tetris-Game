package poppyfanboy.tetrisgame.entities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import poppyfanboy.tetrisgame.graphics.AnimatedObject;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.animation2D.Animated2D;

enum ActiveShapeAnimation {
    DROP, WALL_KICK, ROTATION, LEFT_RIGHT
}
enum BlockAnimation {
    BREAK, DROP
}

interface CallbackHandler {
    void handleAnimationEnd(String argument);
}

/**
 * An object that manages the blocks / active shape animations.
 */
class AnimationManager {
    private EnumMap<ActiveShapeAnimation, AnimatedObject<Animated2D>>
            activeShapeAnimations = new EnumMap<>(ActiveShapeAnimation.class);
    private EnumMap<ActiveShapeAnimation, String>
            activeShapeCallbackArguments = new EnumMap<>(ActiveShapeAnimation.class);

    private EnumMap<BlockAnimation, List<AnimatedObject<Animated2D>>>
            blocksAnimations = new EnumMap<>(BlockAnimation.class);
    private EnumMap<BlockAnimation, String>
            blocksCallbackArguments = new EnumMap<>(BlockAnimation.class);

    private final CallbackHandler handler;

    public AnimationManager(CallbackHandler handler) {
        this.handler = handler;
    }

    public void tick() {
        singleObjectAnimationTick(activeShapeAnimations,
                activeShapeCallbackArguments, handler);

        multipleObjectAnimationTick(blocksAnimations,
                blocksCallbackArguments, handler);
    }

    // iterates through the animations, updates them, deletes the
    // finished ones from the mapping and after that invokes the
    // callbacks if there are any
    private static <T extends Enum<T>> void singleObjectAnimationTick(
            EnumMap<T, ? extends AnimatedObject<?>> animations,
            EnumMap<T, String> callbackArguments, CallbackHandler handler) {
        List<T> finishedAnimations = null;
        Iterator<T> animationsIterator = animations.keySet().iterator();
        while (animationsIterator.hasNext()) {
            T animationType = animationsIterator.next();
            AnimatedObject<?> animatedObject
                    = animations.get(animationType);
            animatedObject.tick();
            if (animatedObject.finished()) {
                animatedObject.perform();
                animationsIterator.remove();
                if (finishedAnimations == null) {
                    finishedAnimations = new ArrayList<>();
                }
                finishedAnimations.add(animationType);
            }
        }
        if (finishedAnimations != null) {
            for (T animationType : finishedAnimations) {
                if (callbackArguments.containsKey(animationType)) {
                    // callback could (and actually does) mutate the
                    // callbacks map, so you must first remove the old
                    // value and only then trigger the callback
                    String callbackArgument = callbackArguments.get(animationType);
                    callbackArguments.remove(animationType);
                    handler.handleAnimationEnd(callbackArgument);
                }
            }
        }
    }

    // does the same thing, but the animation is considered to be finished
    // only if all of the animation objects on the list are finished
    // (passing an empty list does not count as a finished animation)
    private static <AnimationType extends Enum<AnimationType>, T extends AnimatedObject<?>>
            void multipleObjectAnimationTick(EnumMap<AnimationType,
            List<T>> animations, EnumMap<AnimationType,
            String> callbackArguments, CallbackHandler handler) {
        List<AnimationType> finishedAnimations = null;
        Iterator<AnimationType> animationsIterator = animations.keySet().iterator();
        while (animationsIterator.hasNext()) {
            AnimationType animationType = animationsIterator.next();
            List<T> animatedObjects
                    = animations.get(animationType);
            if (animatedObjects.isEmpty()) {
                animationsIterator.remove();
                continue;
            }
            Iterator<T> animatedObjectsIterator
                    = animatedObjects.iterator();
            while (animatedObjectsIterator.hasNext()) {
                AnimatedObject<?> animatedObject
                        = animatedObjectsIterator.next();
                animatedObject.tick();
                if (animatedObject.finished()) {
                    animatedObject.perform();
                    animatedObjectsIterator.remove();
                }
            }
            if (animatedObjects.isEmpty()) {
                if (finishedAnimations == null) {
                    finishedAnimations = new ArrayList<>();
                }
                finishedAnimations.add(animationType);
                animationsIterator.remove();
            }
        }
        if (finishedAnimations != null) {
            for (AnimationType animationType : finishedAnimations) {
                if (callbackArguments.containsKey(animationType)) {
                    String callbackArgument = callbackArguments.get(animationType);
                    callbackArguments.remove(animationType);
                    handler.handleAnimationEnd(callbackArgument);
                }
            }
        }
    }

    public void perform(double interpolation) {
        activeShapeAnimations.values().forEach(
                animation -> animation.perform(interpolation));
        blocksAnimations.values().forEach(
                animationList -> animationList.forEach(
                        animation -> animation.perform(interpolation)));
    }

    public void addActiveShapeAnimation(Shape shape,
            ActiveShapeAnimation animationType,
            Animation<Animated2D> animation) {
        addActiveShapeAnimation(shape, animationType, animation, null, null);
    }

    public void addActiveShapeAnimation(Shape shape,
            ActiveShapeAnimation animationType, Animation<Animated2D> animation,
            CallbackHandler callbackOnEnd, String argument) {
        AnimatedObject<Animated2D> animatedObject
                = new AnimatedObject<>(shape, animation);
        if (callbackOnEnd != null) {
            activeShapeCallbackArguments.put(animationType, argument);
        }
        activeShapeAnimations.put(animationType, animatedObject);
    }

    public void addActiveShapeCallback(ActiveShapeAnimation animationType,
            String argument) {
        activeShapeCallbackArguments.put(animationType, argument);
    }

    public void addBlockAnimation(List<Block> blocks,
            BlockAnimation animationType, List<Animation<Animated2D>> animations) {
        addBlockAnimation(blocks, animationType, animations, null, null);
    }

    public void addBlockAnimation(List<Block> blocks,
            BlockAnimation animationType, List<Animation<Animated2D>> animations,
            CallbackHandler callbackOnEnd, String argument) {
        List<AnimatedObject<Animated2D>> animatedObjects = new LinkedList<>();
        Iterator<Animation<Animated2D>> animationsIterator = animations.iterator();
        for (Block block : blocks) {
            Animation<Animated2D> animation = animationsIterator.next();
            animatedObjects.add(new AnimatedObject<>(block, animation));
        }
        if (callbackOnEnd != null) {
            blocksCallbackArguments.put(animationType, argument);
        }
        blocksAnimations.put(animationType, animatedObjects);
    }

    public Animation<Animated2D> getAnimation(ActiveShapeAnimation animationType) {
        if (activeShapeAnimations.containsKey(animationType)) {
            return activeShapeAnimations.get(animationType).getAnimation();
        } else {
            return null;
        }
    }

    /**
     * Interrupts the animation and cancels any related callbacks.
     */
    public void interruptAnimation(ActiveShapeAnimation animationType) {
        if (activeShapeAnimations.containsKey(animationType)) {
            activeShapeAnimations.remove(animationType);
            activeShapeCallbackArguments.remove(animationType);
        }
    }
}