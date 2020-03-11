package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.entities.shapetypes.ShapeType;
import poppyfanboy.tetrisgame.entities.shapetypes.TetrisShapeType;
import poppyfanboy.tetrisgame.graphics.AnimatedObject;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.animation2D.Animated2D;
import poppyfanboy.tetrisgame.graphics.animation2D.HVLinearAnimation;
import poppyfanboy.tetrisgame.input.Controllable;
import poppyfanboy.tetrisgame.input.InputKey;
import poppyfanboy.tetrisgame.input.KeyState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.IntVector;
import poppyfanboy.tetrisgame.states.GameState;
import poppyfanboy.tetrisgame.util.Rotation;
import poppyfanboy.tetrisgame.util.Transform;
import poppyfanboy.tetrisgame.util.Util;

import static java.lang.Math.abs;
import static poppyfanboy.tetrisgame.entities.GameField.GameFieldState.*;
import static poppyfanboy.tetrisgame.util.IntVector.iVect;

/**
 * A game field entity. Wraps several block entities and a single shape
 * entity that can be moved and rotated.
 *
 * Score is calculated based on this bonuses rules:
 *  - 1 line cleared => 1 point
 *  - 2 lines cleared => 3 points
 *  - 3 lines cleared => 5 points
 *  - 4 lines cleared (a tetris) => 8 points
 *  - 1 line cleared with a T-spin => 3 points
 *  - 2 lines cleared with a T-spin => 7 points
 *  - 3 lines cleared with a T-spin => 6 points
 *  - second "difficult" (T-spin or tetris) lines clear in a row(B2B)
 *          => 12 points
 */
public class GameField extends Entity implements TileField, Controllable, AnimatedObject.CallbackHandler {
    public static int DEFAULT_WIDTH = 10, DEFAULT_HEIGHT = 20;
    public static IntVector SPAWN_COORDINATES = iVect(2, 0);

    private GameState gameState;
    private EnumMap<InputKey, KeyState> lastInputs;
    private AnimationManager animationManager= new AnimationManager();

    // graphics
    private Entity parentEntity;
    private DoubleVector coords;
    private double rotationAngle;

    // game logic
    private int widthInBlocks, heightInBlocks;
    private Shape activeShape;
    private NavigableMap<IntVector, Block> fallenBlocks
            = new TreeMap<>(IntVector.Y_ORDER);

    private final Random random;
    private int level = 1;
    private int score = 0;
    private int clearedLinesCount = 0;
    // how many ticks past since last active shape drop
    private int lastDropCounter = 0;

    private int softDropDuration = Game.TICKS_PER_SECOND / 4;
    private int forcedDropDuration = Game.TICKS_PER_SECOND / 16;
    private int userControlAnimationDuration = softDropDuration;
    private int blockBreakDuration = softDropDuration;

    private GameFieldState state = STOPPED;

    // these collections are made unmodifiable
    private List<Block> brokenBlocks = Collections.emptyList();
    private List<Block> droppedBlocks = Collections.emptyList();
    private List<IntVector> droppedBlocksOldKeys = Collections.emptyList();

    /**
     * Creates an empty instance of a game field.
     *
     * @param   coords coordinates of the game field relative to the
     *          parent entity.
     */
    public GameField(GameState gameState, DoubleVector coords,
            int widthInBlocks, int heightInBlocks, Entity parentEntity,
            Random random) {
        this.gameState = gameState;
        this.widthInBlocks = widthInBlocks;
        this.heightInBlocks = heightInBlocks;
        this.parentEntity = parentEntity;
        this.random = random;

        this.coords = coords;
        this.rotationAngle = 0;
    }

    /**
     * Creates a game field with no parent entity.
     */
    public GameField(GameState gameState, DoubleVector coords,
            int widthInBlocks, int heightInBlocks, Random random) {
        this(gameState, coords, widthInBlocks, heightInBlocks, null,
                random);
    }

    public void start() {
        state = SHAPE_SOFT_DROPPING;

        ShapeType randomType
                = Util.getRandomInstance(random, TetrisShapeType.class);
        BlockColor randomColor
                = Util.getRandomInstance(random, BlockColor.class);
        BlockColor[] colors
                = Shape.generateColorsArray(randomType, randomColor);
        spawnNewActiveShape(randomType, SPAWN_COORDINATES, Rotation.INITIAL,
                colors);
    }

    /**
     * In case the game field is in the state of breaking the blocks from
     * the filled lines (playing the animation of breaking them),
     * returns the blocks that are being broken. Otherwise returns an
     * empty collection.
     */
    public List<Block> getBrokenBlocks() {
        if (state == CLEARING_FILLED_LINES) {
            return brokenBlocks;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * The same thing as {@link GameField#getBrokenBlocks()}, but for
     * the state of blocks dropping above the cleared lines.
     */
    public List<Block> getDroppingBlocks() {
        if (state == DROPPING_BLOCKS) {
            return droppedBlocks;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Try to spawn the specified shape on a game field.
     *
     * This method can fail in case the inserted shape overlaps some
     * of the blocks on the field or it is out of the field bounds.
     */
    private boolean spawnNewActiveShape(ShapeType shapeType,
            IntVector tileCoords, Rotation rotation,
            BlockColor[] blockColors) {
        if (!Shape.fits(shapeType, tileCoords, rotation, this)) {
            return false;
        }
        activeShape = new Shape(gameState, shapeType, rotation,
                tileCoords, blockColors, this);
        return true;
    }

    /**
     * There is no need in checking the whole set of the blocks, since
     * when the shape falls it can make at most 4 filled rows, so
     * we can restrict the searching area to 4 specific rows
     * (from startY to endY, boundaries included)
     */
    private void removeFilledRows(int startY, int endY) {
        assert startY <= endY;

        final int width = getWidthInBlocks();
        IntVector startCoords = new IntVector(0, startY);
        IntVector endCoords = new IntVector(width - 1, endY);
        // the iterator returns the blocks from top the top
        // ones down to the bottom ones
        Collection<Block> removalCandidates
                = fallenBlocks.subMap(startCoords, true, endCoords, true)
                .values();
        int currentRow = startY - 1;

        // line indices are sorted from the top to the bottom
        ArrayList<Integer> clearedLinesIndices = new ArrayList<>();
        brokenBlocks = new ArrayList<>();
        // current same-row-blocks-streak
        ArrayList<Block> currentRowBlocks = new ArrayList<>();
        for (Block block : removalCandidates) {
            if (currentRow != block.getTileCoords().getY()) {
                if (currentRowBlocks.size() == width) {
                    brokenBlocks.addAll(currentRowBlocks);
                    clearedLinesIndices.add(currentRow);
                }
                currentRowBlocks.clear();
                currentRowBlocks.add(block);
                currentRow = block.getTileCoords().getY();
            } else {
                currentRowBlocks.add(block);
            }
        }
        // the last row
        if (currentRowBlocks.size() == width) {
            brokenBlocks.addAll(currentRowBlocks);
            clearedLinesIndices.add(currentRow);
        }
        brokenBlocks = Collections.unmodifiableList(brokenBlocks);

        // move down rows that were above the cleared rows
        if (clearedLinesIndices.size() != 0) {
            final int bottomLine = clearedLinesIndices
                    .get(clearedLinesIndices.size() - 1) - 1;
            droppedBlocks = new ArrayList<>(fallenBlocks.subMap(
                    new IntVector(0, 0), true,
                    new IntVector(width - 1, bottomLine), true).values());
            // these has not yet been removed from the fallenBlocks mapping
            droppedBlocks.removeAll(brokenBlocks);

            int removedLinesLeft = clearedLinesIndices.size();
            final int removedLinesCount = clearedLinesIndices.size();
            droppedBlocksOldKeys = new ArrayList<>();
            for (Block block : droppedBlocks) {
                // skip through the cleared lines to the one that is right
                // under the current block
                while (block.getTileCoords().getY()
                        > clearedLinesIndices
                        .get(removedLinesCount - removedLinesLeft)) {
                    removedLinesLeft--;
                    if (removedLinesLeft == 0) {
                        break;
                    }
                }
                if (removedLinesLeft == 0) {
                    break;
                }
                droppedBlocksOldKeys.add(block.getTileCoords());
                block.tileShift(iVect(0, removedLinesLeft));
            }
        }
        droppedBlocks = Collections.unmodifiableList(droppedBlocks);
        droppedBlocksOldKeys
                = Collections.unmodifiableList(droppedBlocksOldKeys);
    }

    /**
     * Changes the state of the game state. Ideally this method should
     * have a some kind of graph of states with all possible transitions
     * between them, thus preventing any illegal transitions. For now
     * this method relies on the correctness of the calling code.
     */
    @Override
    public void handleAnimationEnd(String newState) {
        switch (newState) {
            case "CLEARING_FILLED_LINES":
                state = CLEARING_FILLED_LINES;
                final int startY = activeShape.getTileCoords().getY();
                // lock the active shape
                for (Block block : activeShape.getBlocks(this)) {
                    fallenBlocks.put(block.getTileCoords(), block);
                }
                activeShape = null;
                removeFilledRows(startY, startY + 3);

                for (Block block : brokenBlocks) {
                    animationManager.addBlockAnimation(block, BlockAnimation.BREAK,
                        block.createBlockBreakAnimation(blockBreakDuration),
                        this, "DROPPING_BLOCKS");
                }
                if (brokenBlocks.isEmpty()) {
                    handleAnimationEnd("SHAPE_SPAWN_READY");
                }
                break;

            case "DROPPING_BLOCKS":
                state = DROPPING_BLOCKS;
                for (Block block : brokenBlocks) {
                    fallenBlocks.remove(block.getTileCoords());
                }
                brokenBlocks = Collections.emptyList();

                for (Block block : droppedBlocks) {
                    animationManager.addBlockAnimation(block, BlockAnimation.DROP,
                            block.createDropAnimation(), this,
                            "SHAPE_SPAWN_READY");
                }
                for (int i = droppedBlocksOldKeys.size() - 1; i >= 0; i--) {
                    Block block = droppedBlocks.get(i);
                    fallenBlocks.remove(droppedBlocksOldKeys.get(i));
                    fallenBlocks.put(block.getTileCoords(), block);
                }
                if (droppedBlocks.isEmpty()) {
                    handleAnimationEnd("SHAPE_SPAWN_READY");
                }
                break;

            case "SHAPE_SPAWN_READY":
                state = SHAPE_SPAWN_READY;
                droppedBlocks = Collections.emptyList();
                droppedBlocksOldKeys = Collections.emptyList();
                break;
        }
    }

    @Override
    public int getWidthInBlocks() {
        return widthInBlocks;
    }

    @Override
    public int getHeightInBlocks() {
        return heightInBlocks;
    }

    @Override
    public Collection<? extends TileFieldObject> getObjects() {
        ArrayList<TileFieldObject> objects
                = new ArrayList<>(fallenBlocks.values());
        if (activeShape != null) {
            objects.add(activeShape);
        }
        return objects;
    }

    @Override
    public Entity getParentEntity() {
        return parentEntity;
    }

    @Override
    public Transform getLocalTransform() {
        int blockWidth = gameState.getBlockWidth();
        int width = widthInBlocks * blockWidth;
        int height = heightInBlocks * blockWidth;
        DoubleVector rotationPivot = coords.add(width / 2.0, height / 2.0);

        return new Transform(coords)
            .combine(Transform.getRotation(rotationAngle, rotationPivot));
    }

    @Override
    public Transform getAdditionalTransform(Entity entity) {
        if (entity == activeShape) {
            // fit the shape into the game field
            final int blockWidth = gameState.getBlockWidth();
            DoubleVector[] convexHull = activeShape.getConvexHull();

            int width = getWidthInBlocks() * blockWidth;
            int height = getHeightInBlocks() * blockWidth;
            // (x1, y1) - upper left corner of the game field
            // (x2, y2) - bottom right corner of the game field
            double x1 = 0, y1 = 0;
            double x2 = x1 + width, y2 = y1 + height;

            // additional shifts to try to put the shape inside the game
            // field frame
            int fitDX = 0, fitDY = 0;
            for (DoubleVector point : convexHull) {
                if (point.getX() < x1
                        && abs(x1 - point.getX()) > abs(fitDX)) {
                    fitDX = (int) (x1 - point.getX());
                }
                if (point.getX() > x2
                        && abs(x2 - point.getX()) > abs(fitDX)) {
                    fitDX = (int) (x2 - point.getX());
                }
                if (point.getY() < y1
                        && abs(y1 - point.getY()) > abs(fitDY)) {
                    fitDY = (int) (y1 - point.getY());
                }
                if (point.getY() > y2
                        && abs(y2 - point.getY()) > abs(fitDY)) {
                    fitDY = (int) (y2 - point.getY());
                }
            }
            return new Transform(new DoubleVector(fitDX, fitDY));
        } else {
            return new Transform();
        }
    }

    @Override
    public void render(Graphics2D g, double interpolation) {
        animationManager.perform(interpolation);

        Transform globalTransform = getGlobalTransform();
        DoubleVector coords
                = globalTransform.apply(new DoubleVector(0, 0));

        final int blockWidth = gameState.getBlockWidth();
        BufferedImage wallBlock = gameState.getAssets().getWallBlock();
        for (int row = 0; row < heightInBlocks; row++) {
            for (int col = 0; col < widthInBlocks; col++) {
                DoubleVector blockCoords
                        = coords.add(col * blockWidth, row * blockWidth);
                g.drawImage(wallBlock, (int) blockCoords.getX(),
                        (int) blockCoords.getY(),
                        blockWidth, blockWidth, null);
            }
        }

        if (activeShape != null) {
            activeShape.render(g, interpolation);
        }
        for (Block block : fallenBlocks.values()) {
            block.render(g, interpolation);
        }
    }

    @Override
    public DoubleVector[] getVertices() {
        Transform globalTransform = getGlobalTransform();
        return globalTransform.apply(new DoubleVector[] {
            DoubleVector.dVect(0, 0),
            DoubleVector.dVect(0, heightInBlocks),
            DoubleVector.dVect(widthInBlocks, heightInBlocks),
            DoubleVector.dVect(widthInBlocks, 0)});
    }

    @Override
    public void tick() {
        animationManager.tick();

        if (activeShape != null) {
            activeShape.tick();
        }
        for (Block block : fallenBlocks.values()) {
            block.tick();
        }

        /*if (state.shapeFalling()) {
            lastDropCounter++;
        }*/

        /*if (state == SHAPE_WALL_KICKED) {
            if (activeShape.getTimeTillAnimationFinishes() == 0) {
                state = SHAPE_SOFT_DROPPING;
                if (lastInputs != null) {
                    KeyState downKeyState
                            = lastInputs.get(InputKey.ARROW_DOWN);
                    if (downKeyState == KeyState.HELD
                            || downKeyState == KeyState.PRESSED) {
                        state = SHAPE_FORCED_DROPPING;
                    }
                }
                tryDropActiveShape();
            }
        }*/

        /*if (lastDropCounter >= softDropDuration
                && state == SHAPE_SOFT_DROPPING
                || lastDropCounter >= forcedDropDuration
                && state == SHAPE_FORCED_DROPPING) {
            tryDropActiveShape();
        }

        if (state == SHAPE_FELL
                && activeShape.getTimeTillAnimationFinishes() == 0) {
            state = SHAPE_LOCKED;
            breakBlocks(this);
        }

        if (state == SHAPE_SPAWN_READY) {
            state = SHAPE_SOFT_DROPPING;
            if (lastInputs != null) {
                KeyState downKeyState = lastInputs.get(InputKey.ARROW_DOWN);
                if (downKeyState == KeyState.HELD
                        || downKeyState == KeyState.PRESSED) {
                    state = SHAPE_FORCED_DROPPING;
                }
            }

            ShapeType randomType
                    = Util.getRandomInstance(random, TetrisShapeType.class);
            BlockColor randomColor
                    = Util.getRandomInstance(random, BlockColor.class);
            BlockColor[] colors
                    = Shape.generateColorsArray(randomType, randomColor);
            spawnNewActiveShape(randomType, SPAWN_COORDINATES,
                    Rotation.INITIAL, colors);
        }*/
    }

    /*private void tryDropActiveShape() {
        if (Shape.fits(activeShape, activeShape.getShapeType(),
                activeShape.getTileCoords().add(iVect(0, 1)),
                activeShape.getRotation(), this)) {
            activeShape.tileShift(iVect(0, 1));
            int duration = state == SHAPE_FORCED_DROPPING
                    ? forcedDropDuration
                    : softDropDuration;
            activeShape.addDropAnimation(duration);
            lastDropCounter = 0;
        } else {
            state = SHAPE_FELL;
        }
    }*/

    @Override
    public void control(EnumMap<InputKey, KeyState> inputs) {
        lastInputs = inputs;

        final int blockWidth = gameState.getBlockWidth();
        int xShift = 0;
        Rotation rotationDirection = Rotation.INITIAL;

        for (EnumMap.Entry<InputKey, KeyState> key : inputs.entrySet()) {
            if (key.getValue() == KeyState.PRESSED) {
                switch (key.getKey()) {
                    case ARROW_DOWN:
                        /*if (!state.shapeFalling()) {
                            break;
                        }
                        if (state == SHAPE_SOFT_DROPPING) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / softDropDuration)
                                * forcedDropDuration);
                        }
                        state = SHAPE_FORCED_DROPPING;
                        HVLinearAnimation currentDropAnimation =
                                activeShape.getDropAnimation();
                        if (currentDropAnimation != null
                                && !currentDropAnimation.finished()) {
                            currentDropAnimation = currentDropAnimation.changeDuration(forcedDropDuration, blockWidth);
                        }*/
                        break;
                    case ARROW_LEFT:
                        xShift--;
                        break;
                    case ARROW_RIGHT:
                        xShift++;
                        break;
                    case W:
                        rotationDirection
                                = rotationDirection.add(Rotation.LEFT);
                        break;
                    case S:
                        rotationDirection
                                = rotationDirection.add(Rotation.RIGHT);
                        break;
                }
            }
            if (key.getValue() == KeyState.RELEASED) {
                switch (key.getKey()) {
                    case ARROW_DOWN:
                        /*if (!state.shapeFalling()) {
                            break;
                        }
                        if (state == SHAPE_FORCED_DROPPING) {
                            lastDropCounter = (int) Math.round(
                                (1.0 * lastDropCounter / forcedDropDuration)
                                * softDropDuration);
                        }
                        state = SHAPE_SOFT_DROPPING;
                        HVLinearAnimation currentDropAnimation =
                                activeShape.getDropAnimation();
                        if (currentDropAnimation != null
                                && !currentDropAnimation.finished()) {
                            currentDropAnimation
                                    .changeDuration(softDropDuration);
                        }*/
                        break;
                }
            }
        }
        // for now just block controls when the shape falls completely
        // on the ground; later change it in a way that hitting control
        // buttons in this state, when the old shape fell but a new
        // one has not yet spawned, would change the initial position
        // of a newly spawned shape
        if (xShift != 0 && state.shapeFalling()) {
            /*if (activeShape != null && Shape.fits(activeShape,
                    activeShape.getShapeType(),
                    activeShape.getTileCoords().add(iVect(xShift, 0)),
                    activeShape.getRotation(), this)) {
                activeShape.tileShift(iVect(xShift, 0));
                activeShape.addUserControlAnimation(
                        userControlAnimationDuration);
            }*/
        }
        if (activeShape != null && state.shapeFalling()
                && (rotationDirection.equals(Rotation.LEFT)
                || rotationDirection.equals(Rotation.RIGHT))) {
            /*Rotation newRotation
                    = activeShape.getRotation().add(rotationDirection);
            if (Shape.fits(activeShape, activeShape.getShapeType(),
                    activeShape.getTileCoords(), newRotation, this)) {
                activeShape.rotate(rotationDirection);
                activeShape.addRotationAnimation(
                        rotationDirection, userControlAnimationDuration);
            } else {
                IntVector[] wallKicks = rotationDirection == Rotation.RIGHT
                        ? activeShape.getRightWallKicks()
                        : activeShape.getLeftWallKicks();
                for (IntVector shift : wallKicks) {
                    if (Shape.fits(activeShape, activeShape.getShapeType(),
                            activeShape.getTileCoords().add(shift),
                            newRotation, this)) {
                        // rotate and wall kick
                        activeShape.rotate(rotationDirection);
                        activeShape.tileShift(shift);
                        state = SHAPE_WALL_KICKED;

                        activeShape.addMovementAnimation(
                                userControlAnimationDuration);
                        activeShape.addRotationAnimation(rotationDirection,
                                userControlAnimationDuration);
                        break;
                    }
                }
            }*/
        }
    }

    public enum GameFieldState {
        STOPPED, PAUSED,
        SHAPE_SOFT_DROPPING, SHAPE_FORCED_DROPPING, SHAPE_WALL_KICKED,
        SHAPE_FELL, SHAPE_LOCKED,
        CLEARING_FILLED_LINES, DROPPING_BLOCKS,
        SHAPE_SPAWN_READY;

        public boolean shapeFalling() {
            return this == SHAPE_SOFT_DROPPING
                    || this == SHAPE_FORCED_DROPPING
                    || this == SHAPE_WALL_KICKED;
        }
    }

    public enum ActiveShapeAnimation {
        SOFT_DROP, FORCED_DROP, WALL_KICK, ROTATION
    }
    public enum BlockAnimation {
        BREAK, DROP
    }

    /**
     * An object that manages the blocks / active shape animations.
     */
    private static class AnimationManager {
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
    }
}
