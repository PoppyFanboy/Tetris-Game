package poppyfanboy.tetrisgame.entities;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import poppyfanboy.tetrisgame.graphics.AnimatedObject;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.AnimationEndHandler;
import poppyfanboy.tetrisgame.graphics.animation2D.Animated2D;

/**
 * An object that manages all the animations in the game. Objects to be
 * animated are added to the manager as soon as they are created, and removed
 * after they are removed from the game. The objects are identified by their
 * hash code, so you need to override it to something meaningful in order to
 * guarantee performance of the animation gym.
 */
class AnimationManager {
    private HashMap<Animated2D,
                AnimatedObject<Animated2D, ActiveShapeAnimationType>>
            activeShapesAnimated = new HashMap<>();
    private HashMap<Animated2D,
                AnimatedObject<Animated2D, LockedBlockAnimationType>>
            lockedBlocksAnimated = new HashMap<>();

    private boolean isIterating = false;
    private Queue<PostponedAction<?, ?>> postponedActions = new ArrayDeque<>();

    public void tick() {
        isIterating = true;
        activeShapesAnimated.values().forEach(AnimatedObject::tick);
        lockedBlocksAnimated.values().forEach(AnimatedObject::tick);
        isIterating = false;
        while (!postponedActions.isEmpty()) {
            postponedActions.poll().perform();
        }
    }

    public void perform(double interpolation) {
        activeShapesAnimated.values()
                .forEach(object -> object.perform(interpolation));
        lockedBlocksAnimated.values()
                .forEach(object -> object.perform(interpolation));
    }

    // -- object addition operations --

    public void addActiveShape(Shape activeShape) {
        addObject(activeShape, activeShapesAnimated,
                ActiveShapeAnimationType.class);
    }

    public void addLockedBlock(Block lockedBlock) {
        addObject(lockedBlock, lockedBlocksAnimated,
                LockedBlockAnimationType.class);
    }

    // -- animation addition operations --

    /**
     * @throws  IllegalArgumentException in case the specified active shape
     *          is not present in the animation manager. You have to first add
     *          it through the {@link AnimationManager#addActiveShape(Shape)}
     *          method.
     *
     *          I could design this method in a way that it would do
     *          this {@code addActiveShape} call for you, but it would slightly
     *          complicate the implementation because of the thing with the
     *          postponed actions, and also I assume that if the calling code
     *          does not explicitly add the active shape to the animation
     *          manager, then it might mean that there is a some kind of a
     *          mistake in the calling code.
     */
    public void addAnimation(Shape activeShape,
            ActiveShapeAnimationType animationType,
            Animation<Animated2D> animation, AnimationEndHandler endHandler) {
        addAnimation(activeShape, animationType, animation, endHandler,
                activeShapesAnimated);
    }

    public void addAnimation(Block fallenBlock,
            LockedBlockAnimationType animationType,
            Animation<Animated2D> animation, AnimationEndHandler endHandler) {
        addAnimation(fallenBlock, animationType, animation, endHandler,
                lockedBlocksAnimated);
    }

    public void addAnimation(Shape activeShape,
            ActiveShapeAnimationType animationType,
            Animation<Animated2D> animation) {
        addAnimation(activeShape, animationType, animation, null);
    }

    public void addAnimation(Block fallenBlock,
            LockedBlockAnimationType animationType,
            Animation<Animated2D> animation) {
        addAnimation(fallenBlock, animationType, animation, null);
    }

    // -- callbacks addition operations --

    public void addAnimationCallback(Shape activeShape,
            ActiveShapeAnimationType animationType,
            AnimationEndHandler endHandler) {
        addAnimationCallback(activeShape, animationType, endHandler,
                activeShapesAnimated);
    }

    // -- animations getter operations --

    public Animation<Animated2D> getAnimation(Shape activeShape,
            ActiveShapeAnimationType animationType) {
        return activeShapesAnimated
                .get(activeShape).getAnimation(animationType);
    }

    // -- animation interruption operations --

    public void interruptAnimation(Shape activeShape,
            ActiveShapeAnimationType animationType) {
        interruptAnimation(activeShape, animationType, activeShapesAnimated);
    }

    // -- object removal operations --

    public void removeActiveShape(Shape activeShape) {
        removeObject(activeShape, activeShapesAnimated);
    }

    public void removeFallenBlock(Block fallenBlock) {
        removeObject(fallenBlock, lockedBlocksAnimated);
    }


    // -------------------------------------------------------
    // -- generic implementations of the methods from above --
    // -------------------------------------------------------

    private <T, K extends Enum<K>> void addObject(T object,
            Map<T, AnimatedObject<T, K>> map, Class<K> animationTypes) {
        if (isIterating) {
            postponedActions
                    .add(getObjectAdditionAction(object, map, animationTypes));
        }
        if (map.containsKey(object)) {
            throw new IllegalArgumentException(String.format("There is "
                    + "already a %s object present in the animation manager",
                    object));
        }
        map.put(object, new AnimatedObject<>(object, animationTypes));
    }

    // if no endHandler is specified, set it to null
    private <T, K extends Enum<K>> void addAnimation(T object,
            K animationType, Animation<T> animation,
            AnimationEndHandler endHandler,
            Map<T, AnimatedObject<T, K>> map) {
        throwExceptionIfNotPresent(object, map);
        map.get(object).addAnimation(animationType, animation, endHandler);
    }

    private <T, K extends Enum<K>> void addAnimationCallback(T object,
            K animationType, AnimationEndHandler endHandler,
            Map<T, AnimatedObject<T, K>> map) {
        throwExceptionIfNotPresent(object, map);
        map.get(object).addCallback(animationType, endHandler);
    }

    private <T, K extends Enum<K>> void interruptAnimation(T object,
            K animationType, Map<T, AnimatedObject<T, K>> map) {
        throwExceptionIfNotPresent(object, map);
        map.get(object).interruptAnimation(animationType);
    }

    private <T, K extends Enum<K>> void removeObject(T object,
            Map<T, AnimatedObject<T, K>> map) {
        if (isIterating) {
            postponedActions.add(getObjectRemovalAction(object, map));
        }
        map.remove(object);
    }


    private class PostponedAction<T, K extends Enum<K>> {
        private final ActionType actionType;
        private final Map<T, AnimatedObject<T, K>> map;

        private T object;
        private Class<K> animationTypes;

        private PostponedAction(ActionType actionType, T object,
                Map<T, AnimatedObject<T, K>> map, Class<K> animationTypes) {
            this.actionType = actionType;
            this.object = object;
            this.map = map;
            this.animationTypes = animationTypes;
        }

        public void perform() {
            switch (actionType) {
                case ADD_OBJECT:
                    addObject(object, map, animationTypes);
                    break;

                case REMOVE_OBJECT:
                    removeObject(object, map);
                    break;
            }
        }
    }
    private enum ActionType {
        ADD_OBJECT, REMOVE_OBJECT
    }

    private <T, K extends Enum<K>> PostponedAction<T, K>
            getObjectRemovalAction(T object,
                    Map<T, AnimatedObject<T, K>> map) {
        return new PostponedAction<>(
                ActionType.REMOVE_OBJECT, object, map, null);
    }

    private <T, K extends Enum<K>> PostponedAction<T, K>
            getObjectAdditionAction(T object, Map<T, AnimatedObject<T, K>> map,
                    Class<K> animationTypes) {
        return new PostponedAction<>(
                ActionType.ADD_OBJECT, object, map, animationTypes);
    }

    // a small helper method that throws an exception in case the object is
    // not present among the keys of the map
    private static <T> void throwExceptionIfNotPresent(T object,
            Map<T, ?> map) {
        if (!map.containsKey(object)) {
            throw new IllegalArgumentException(String.format("The specified"
                    + " object %s is not present in the animation manager.",
                    object));
        }
    }
}

enum EntityType {
    ACTIVE_SHAPE, LOCKED_BLOCK
}
enum ActiveShapeAnimationType {
    DROP, WALL_KICK, ROTATION, LEFT_RIGHT
}
enum LockedBlockAnimationType {
    BREAK, DROP
}