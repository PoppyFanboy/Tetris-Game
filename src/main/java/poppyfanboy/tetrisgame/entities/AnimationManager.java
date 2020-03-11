package poppyfanboy.tetrisgame.entities;

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

/**
 * An object that manages the blocks / active shape animations.
 */
class AnimationManager {
    private EnumMap<ActiveShapeAnimation, AnimatedObject<Animated2D>>
            activeShapeAnimations = new EnumMap<>(ActiveShapeAnimation.class);
    private EnumMap<BlockAnimation, List<AnimatedObject<Animated2D>>>
            blocksAnimations = new EnumMap<>(BlockAnimation.class);

    // It might be the case that the callbacks access the animation
    // manager and add new animations, thus mutating the collections
    // while the animation manager is still iterating through the
    // animations in the tick() method. This situation will cause
    // a concurrent modification exception. To avoid this, while
    // the animation manager is in the tick() method all the commands
    // are temporarily addressed to these buffer data structures.
    // At the end of the tick method all queued animations are added
    // to the animation manager properly.

    // The problem with this thing is that it can only handle a
    // single animation addition of the same type, any subsequent
    // additions will override it. Another problem is that you can't
    // interrupt any animations while the animation manager is in the
    // tick() method, and I can't think of a simple solution for this
    // problem.

    private boolean currentlyTicked = false;
    private EnumMap<ActiveShapeAnimation, AnimatedObject<Animated2D>>
            queuedActiveShapeAnimations = new EnumMap<>(ActiveShapeAnimation.class);
    private EnumMap<BlockAnimation, List<AnimatedObject<Animated2D>>>
            queuedBlocksAnimations = new EnumMap<>(BlockAnimation.class);

    public AnimationManager() {
        for (BlockAnimation animation : BlockAnimation.values()) {
            blocksAnimations.put(animation, new LinkedList<>());
            queuedBlocksAnimations.put(animation, new LinkedList<>());
        }
    }

    public void tick() {
        currentlyTicked = true;
        // iterate through the animations and remove the finished ones
        Iterator<AnimatedObject<Animated2D>> iterator
                = activeShapeAnimations.values().iterator();
        while (iterator.hasNext()) {
            AnimatedObject<Animated2D> animation = iterator.next();
            animation.tick();
            if (animation.finished()) {
                iterator.remove();
            }
        }
        for (BlockAnimation animationType : BlockAnimation.values()) {
            iterator = blocksAnimations.get(animationType).iterator();
            while (iterator.hasNext()) {
                AnimatedObject<Animated2D> animation = iterator.next();
                animation.tick();
                if (animation.finished()) {
                    iterator.remove();
                }
            }
        }
        currentlyTicked = false;
        // add the animations from the queue that might occurred there
        // as a result of animation-end callbacks
        if (!queuedActiveShapeAnimations.isEmpty()) {
            activeShapeAnimations.putAll(queuedActiveShapeAnimations);
            queuedActiveShapeAnimations.clear();
        }
        for (BlockAnimation animation : BlockAnimation.values()) {
            List<AnimatedObject<Animated2D>> queuedAnimations
                    = queuedBlocksAnimations.get(animation);
            if (!queuedAnimations.isEmpty()) {
                blocksAnimations.get(animation).addAll(queuedAnimations);
                queuedAnimations.clear();
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
                                        ActiveShapeAnimation animationType,
                                        Animation<Animated2D> animation,
                                        AnimatedObject.CallbackHandler callbackOnEnd,
                                        String argument) {
        AnimatedObject<Animated2D> newAnimation
                = new AnimatedObject<>(shape, animation);
        if (callbackOnEnd != null) {
            newAnimation.notifyOnEnd(callbackOnEnd, argument);
        }
        if (currentlyTicked) {
            queuedActiveShapeAnimations.put(animationType, newAnimation);
        } else {
            activeShapeAnimations.put(animationType, newAnimation);
        }
    }

    public void addBlockAnimation(Block block,
                                  BlockAnimation animationType,
                                  Animation<Animated2D> animation) {
        addBlockAnimation(block, animationType, animation, null, null);
    }

    public void addBlockAnimation(Block block,
                                  BlockAnimation animationType,
                                  Animation<Animated2D> animation,
                                  AnimatedObject.CallbackHandler callbackOnEnd,
                                  String argument) {
        AnimatedObject<Animated2D> newAnimation
                = new AnimatedObject<>(block, animation);
        if (callbackOnEnd != null) {
            newAnimation.notifyOnEnd(callbackOnEnd, argument);
        }
        if (currentlyTicked) {
            queuedBlocksAnimations.get(animationType).add(newAnimation);
        } else {
            blocksAnimations.get(animationType).add(newAnimation);
        }
    }

    // this method is not yet callback-safe
    public void interruptAnimation(ActiveShapeAnimation animationType) {
        activeShapeAnimations.remove(animationType);
    }

    // this method is not yet callback-safe
    public void interruptAnimation(BlockAnimation animationType) {
        blocksAnimations.get(animationType).clear();
    }

    public Animation<Animated2D> getAnimation(ActiveShapeAnimation animationType) {
        if (activeShapeAnimations.containsKey(animationType)) {
            return activeShapeAnimations.get(animationType).getAnimation();
        } else {
            return null;
        }
    }
}