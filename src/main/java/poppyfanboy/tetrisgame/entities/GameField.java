package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.states.GameState;

import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.graphics.animation2D.RotationAnimation;
import poppyfanboy.tetrisgame.entities.shapetypes.ShapeType;
import poppyfanboy.tetrisgame.entities.shapetypes.TetrisShapeType;

import poppyfanboy.tetrisgame.input.Controllable;
import poppyfanboy.tetrisgame.input.InputKey;
import poppyfanboy.tetrisgame.input.KeyState;
import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.IntVector;
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
public class GameField extends Entity implements TileField, Controllable {
    public static int DEFAULT_WIDTH = 10, DEFAULT_HEIGHT = 20;
    public static IntVector SPAWN_COORDINATES = iVect(2, 0);

    private GameState gameState;
    private AnimationManager animationManager;
    private NextShapeDisplay nextShapeDisplay;
    private EnumMap<InputKey, KeyState> lastInputs;

    // graphics
    private Entity parentEntity;
    private DoubleVector coords;
    private double rotationAngle;

    // game logic
    private GameFieldState state = STOPPED;
    private Queue<GameFieldState> statesQueue = new StatesQueue();

    private int widthInBlocks, heightInBlocks;
    private Shape activeShape;
    private ShapeType nextShapeType;
    // blocks that were locked at the game field after some of the shapes
    // fell onto the bottom of the game field
    private NavigableMap<IntVector, Block> lockedBlocks
            = new TreeMap<>(IntVector.Y_ORDER);

    private final Random random;
    private int level = 1;
    private int score = 0;
    private int clearedLinesCount = 0;

    // current timings (updated as the player score is going up)
    private int softDropDuration = Game.TICKS_PER_SECOND / 4;
    private int forcedDropDuration = Game.TICKS_PER_SECOND / 16;
    private int userControlAnimationDuration = softDropDuration;
    private int blockBreakDuration = softDropDuration;

    // these collections are made unmodifiable
    // blocks that are being broken when the filled lines are removed
    private List<Block> brokenBlocks = Collections.emptyList();
    // blocks above the removed filled lines that are dropped down after the
    // animation of removing the filled lines is finished
    private List<Block> droppedBlocks = Collections.emptyList();
    // coordinates of these blocks before they were dropped down
    private List<IntVector> droppedBlocksOldKeys = Collections.emptyList();

    public enum GameFieldState {
        STOPPED, PAUSED,
        SHAPE_SOFT_DROP, SHAPE_FORCED_DROP, SHAPE_WALL_KICKED,
        SHAPE_FELL, SHAPE_LOCKED,
        CLEARING_FILLED_LINES, DROPPING_BLOCKS,
        SHAPE_SPAWN_READY;

        // states in which the player can still control the active shape
        private static final EnumSet<GameFieldState> SHAPE_CONTROLLABLE
                = EnumSet.of(SHAPE_SOFT_DROP, SHAPE_FORCED_DROP,
                SHAPE_WALL_KICKED, SHAPE_FELL);

        private static final EnumMap<GameFieldState, EnumSet<GameFieldState>>
                possibleTransitions;

        static {
            possibleTransitions = new EnumMap<>(GameFieldState.class);
            possibleTransitions.put(STOPPED, EnumSet.of(SHAPE_SPAWN_READY));
            possibleTransitions
                    .put(PAUSED, EnumSet.allOf(GameFieldState.class));
            possibleTransitions.put(SHAPE_SOFT_DROP, SHAPE_CONTROLLABLE);
            possibleTransitions.put(SHAPE_FORCED_DROP, SHAPE_CONTROLLABLE);
            possibleTransitions.put(SHAPE_WALL_KICKED, SHAPE_CONTROLLABLE);
            possibleTransitions.put(SHAPE_FELL, EnumSet.of(SHAPE_LOCKED));
            possibleTransitions.put(SHAPE_LOCKED,
                    EnumSet.of(CLEARING_FILLED_LINES, SHAPE_SPAWN_READY));
            possibleTransitions.put(CLEARING_FILLED_LINES,
                    EnumSet.of(DROPPING_BLOCKS, SHAPE_SPAWN_READY));
            possibleTransitions.put(DROPPING_BLOCKS,
                    EnumSet.of(SHAPE_SPAWN_READY));
            possibleTransitions.put(SHAPE_SPAWN_READY, SHAPE_CONTROLLABLE);
        }

        public boolean shapeFalling() {
            return this == SHAPE_SOFT_DROP || this == SHAPE_FORCED_DROP
                    || this == SHAPE_SPAWN_READY;
        }

        public boolean shapeControllable() {
            return SHAPE_CONTROLLABLE.contains(this);
        }

        public boolean transitionPossible(GameFieldState other) {
            return possibleTransitions.get(this).contains(other);
        }
    }

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

        animationManager = gameState.getAnimationManager();
        nextShapeType = Util.getRandomInstance(random, TetrisShapeType.class);
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
        statesQueue.offer(SHAPE_SPAWN_READY);
    }

    /**
     * Changes the state of the game state. Ideally this method should
     * have a some kind of graph of states with all possible transitions
     * between them, thus preventing any illegal transitions. For now
     * this method relies on the correctness of the calling code.
     */
    private void changeState(GameFieldState newState) {
        state = newState;

        switch (newState) {
            case SHAPE_SPAWN_READY:
                droppedBlocks = Collections.emptyList();
                droppedBlocksOldKeys = Collections.emptyList();

                if (spawnNewActiveShape()) {
                    statesQueue.offer(SHAPE_SOFT_DROP);
                } else {
                    statesQueue.offer(SHAPE_FELL);
                }
                break;

            case SHAPE_SOFT_DROP:
            case SHAPE_FORCED_DROP:
                state = SHAPE_SOFT_DROP;
                if (lastInputs != null
                        && lastInputs.containsKey(InputKey.ARROW_DOWN)
                        && lastInputs.get(InputKey.ARROW_DOWN).isActive()) {
                    state = SHAPE_FORCED_DROP;
                }

                if (Shape.fits(activeShape, activeShape.getShapeType(),
                        activeShape.getTileCoords().add(iVect(0, 1)),
                        activeShape.getRotation(), this)) {
                    activeShape.tileShift(iVect(0, 1));
                    int duration = state == SHAPE_FORCED_DROP
                            ? forcedDropDuration
                            : softDropDuration;
                    animationManager.addAnimation(activeShape,
                            ActiveShapeAnimationType.DROP,
                            activeShape.createDropAnimation(duration),
                            reason -> { if (reason.finished())
                                                statesQueue.offer(state); });
                } else {
                    statesQueue.offer(SHAPE_FELL);
                }
                break;

            case SHAPE_WALL_KICKED:
                break;

            case SHAPE_FELL:
                // wait until all of the animations are gone
                for (ActiveShapeAnimationType animationType
                        : ActiveShapeAnimationType.values()) {
                    if (animationManager.getAnimation(
                            activeShape, animationType) != null) {
                        animationManager.addAnimationCallback(activeShape,
                                animationType,
                                reason -> statesQueue.offer(SHAPE_FELL));
                        return;
                    }
                }
                // implement lock delay
                statesQueue.clear();
                statesQueue.offer(CLEARING_FILLED_LINES);
                break;

            case CLEARING_FILLED_LINES:
                final int startY = activeShape.getTileCoords().getY();
                // lock the active shape
                for (Block block : activeShape.getBlocks(this)) {
                    lockedBlocks.put(block.getTileCoords(), block);
                    animationManager.addLockedBlock(block);
                }
                animationManager.removeActiveShape(activeShape);
                activeShape = null;
                removeFilledRows(startY, startY + 3);

                for (Block block : brokenBlocks) {
                    animationManager.addAnimation(block,
                        LockedBlockAnimationType.BREAK,
                        block.createBlockBreakAnimation(blockBreakDuration));
                }
                if (!brokenBlocks.isEmpty()) {
                    // add callback only for one block, since all blocks take
                    // the same time to disappear
                    animationManager.addAnimationCallback(brokenBlocks.get(0),
                            LockedBlockAnimationType.BREAK,
                            reason -> statesQueue.offer(DROPPING_BLOCKS));
                }
                if (brokenBlocks.isEmpty()) {
                    statesQueue.offer(SHAPE_SPAWN_READY);
                }
                break;

            case DROPPING_BLOCKS:
                for (Block block : brokenBlocks) {
                    lockedBlocks.remove(block.getTileCoords());
                    animationManager.removeLockedBlock(block);
                }
                brokenBlocks = Collections.emptyList();

                for (Block block : droppedBlocks) {
                    animationManager.addAnimation(block,
                            LockedBlockAnimationType.DROP,
                            block.createDropAnimation());
                }
                if (!droppedBlocks.isEmpty()) {
                    // add callback only for the highest block, since it will
                    // take the longest time to drop down among all other blocks
                    animationManager.addAnimationCallback(droppedBlocks.get(0),
                            LockedBlockAnimationType.DROP,
                            reason -> statesQueue.offer(SHAPE_SPAWN_READY));
                }
                for (int i = droppedBlocksOldKeys.size() - 1; i >= 0; i--) {
                    Block block = droppedBlocks.get(i);
                    lockedBlocks.remove(droppedBlocksOldKeys.get(i));
                    lockedBlocks.put(block.getTileCoords(), block);
                }
                if (droppedBlocks.isEmpty()) {
                    statesQueue.offer(SHAPE_SPAWN_READY);
                }
                break;
        }
    }

    /**
     * Tries to spawn a new active shape (of a random color and type) at the top
     * of the game field. In case it successes it also adds the newly spawned
     * active shape to the animation manager.
     *
     * Returns {@code false} in case it fails to do so (either it is spawned
     * so that it overlaps some of the fallen blocks, or it is spawned
     * completely out of player sight).
     *
     * (Spawn locations should probably be defined in the shape types
     * themselves, but for now they are just hard-coded into the game field
     * object.)
     */
    private boolean spawnNewActiveShape() {
        ShapeType shapeType = nextShapeType;
        nextShapeType
                = Util.getRandomInstance(random, TetrisShapeType.class);
        BlockColor color
                = Util.getRandomInstance(random, BlockColor.class);
        BlockColor[] blockColors
                = Shape.generateColorsArray(shapeType, color);

        if (!Shape.fits(shapeType, SPAWN_COORDINATES, Rotation.INITIAL, this)) {
            return false;
        }
        activeShape = new Shape(gameState, shapeType, Rotation.INITIAL,
                SPAWN_COORDINATES, blockColors, this);
        animationManager.addActiveShape(activeShape);
        if (nextShapeDisplay != null) {
            nextShapeDisplay.setNextShape(nextShapeType);
            nextShapeDisplay.startTransitionAnimation();
        }
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
                = lockedBlocks.subMap(startCoords, true, endCoords, true)
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
            droppedBlocks = new ArrayList<>(lockedBlocks.subMap(
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
                = new ArrayList<>(lockedBlocks.values());
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
        DoubleVector rotationPivot
                = new DoubleVector(width / 2.0, height / 2.0);
        return Transform.getRotation(rotationAngle, rotationPivot)
                .combine(new Transform(coords));
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
        final int blockWidth = gameState.getBlockWidth();
        AffineTransform oldTransform = g.getTransform();
        g.setTransform(getGlobalTransform().getTransform());

        BufferedImage brickWall
                = gameState.getAssets().getSprite(Assets.SpriteType.BRICK_WALL);
        g.drawImage(brickWall, 0, 0, null);


        BufferedImage frame = gameState.getAssets()
                .getSprite(Assets.SpriteType.GAME_FIELD_FRAME);
        g.drawImage(frame, -blockWidth, -blockWidth, null);

        g.setTransform(oldTransform);

        if (activeShape != null) {
            activeShape.render(g, interpolation);
        }
        for (Block block : lockedBlocks.values()) {
            block.render(g, interpolation);
        }
    }

    @Override
    public DoubleVector[] getVertices() {
        Transform globalTransform = getGlobalTransform();
        int blockWidth = gameState.getBlockWidth();
        return globalTransform.apply(new DoubleVector[] {
            DoubleVector.dVect(0, 0),
            DoubleVector.dVect(0, heightInBlocks * blockWidth),
            DoubleVector.dVect(widthInBlocks * blockWidth,
                    heightInBlocks * blockWidth),
            DoubleVector.dVect(widthInBlocks * blockWidth, 0)});
    }

    @Override
    public void tick() {
        if (activeShape != null) {
            activeShape.tick();
        }
        for (Block block : lockedBlocks.values()) {
            block.tick();
        }
        if (!statesQueue.isEmpty()) {
            changeState(statesQueue.poll());
        }
    }

    @Override
    public void control(EnumMap<InputKey, KeyState> inputs) {
        lastInputs = inputs;

        int xShift = 0;
        Rotation rotationDirection = Rotation.INITIAL;
        final boolean shapeControllable = state.shapeFalling()
                && (statesQueue.peek() == null
                        || statesQueue.peek().shapeFalling());

        for (EnumMap.Entry<InputKey, KeyState> key : inputs.entrySet()) {
            if (key.getValue() == KeyState.PRESSED) {
                switch (key.getKey()) {
                    case ARROW_DOWN:
                        if (!shapeControllable) {
                            break;
                        }
                        state = SHAPE_FORCED_DROP;
                        animationManager.addAnimation(activeShape,
                                ActiveShapeAnimationType.DROP,
                                activeShape.createDropAnimation(
                                        forcedDropDuration));
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
                        if (!shapeControllable) {
                            break;
                        }
                        state = SHAPE_SOFT_DROP;
                        animationManager.addAnimation(activeShape,
                                ActiveShapeAnimationType.DROP,
                                activeShape.createDropAnimation(
                                        softDropDuration));
                        break;
                }
            }
        }

        if (xShift != 0 && shapeControllable) {
            if (Shape.fits(activeShape, activeShape.getShapeType(),
                    activeShape.getTileCoords().add(iVect(xShift, 0)),
                    activeShape.getRotation(), this)) {
                activeShape.tileShift(iVect(xShift, 0));
                animationManager.addAnimation(activeShape,
                        ActiveShapeAnimationType.LEFT_RIGHT,
                        activeShape.createUserControlAnimation(
                            userControlAnimationDuration));
            }
        }
        if (shapeControllable && (rotationDirection == Rotation.LEFT
                    || rotationDirection == Rotation.RIGHT)) {
            boolean isClockwise = rotationDirection == Rotation.LEFT;
            double newAngle = isClockwise ? -Math.PI / 2 : Math.PI / 2;
            Rotation newRotation
                    = activeShape.getRotation().add(rotationDirection);

            RotationAnimation rotationAnimation
                    = activeShape.createRotationAnimation(newAngle,
                    isClockwise, userControlAnimationDuration);

            if (Shape.fits(activeShape, activeShape.getShapeType(),
                    activeShape.getTileCoords(), newRotation, this)) {
                activeShape.rotate(rotationDirection);
                animationManager.addAnimation(activeShape,
                        ActiveShapeAnimationType.ROTATION,
                        rotationAnimation);
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

                        animationManager.addAnimation(activeShape,
                            ActiveShapeAnimationType.ROTATION,
                            rotationAnimation);

                        animationManager.addAnimation(activeShape,
                            ActiveShapeAnimationType.WALL_KICK,
                            activeShape.createMovementAnimation(
                                    userControlAnimationDuration),
                            reason -> statesQueue.offer(SHAPE_SOFT_DROP));

                        animationManager.interruptAnimation(activeShape,
                        ActiveShapeAnimationType.DROP);
                        animationManager.interruptAnimation(activeShape,
                        ActiveShapeAnimationType.LEFT_RIGHT);

                        statesQueue.offer(SHAPE_WALL_KICKED);
                        break;
                    }
                }
            }
        }
    }

    public void setNextShapeDisplay(NextShapeDisplay nextShapeDisplay) {
        this.nextShapeDisplay = nextShapeDisplay;
    }

    private static class StatesQueue extends AbstractQueue<GameFieldState> {
        private Queue<GameFieldState> queue = new ArrayDeque<>();

        @Override
        public Iterator<GameFieldState> iterator() {
            return queue.iterator();
        }

        @Override
        public int size() {
            return queue.size();
        }

        @Override
        public boolean offer(GameFieldState gameFieldState) {
            if (queue.isEmpty()
                    || queue.peek().transitionPossible(gameFieldState)) {
                return queue.offer(gameFieldState);
            }
            return false;
        }

        @Override
        public GameFieldState poll() {
            return queue.poll();
        }

        @Override
        public GameFieldState peek() {
            return queue.peek();
        }
    }
}
