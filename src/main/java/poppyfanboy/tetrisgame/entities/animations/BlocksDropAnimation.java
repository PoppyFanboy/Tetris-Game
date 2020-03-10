package poppyfanboy.tetrisgame.entities.animations;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import poppyfanboy.tetrisgame.entities.Block;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.animation2D.AcceleratedMoveAnimation;

public class BlocksDropAnimation implements Animation {
    private final GameField gameField;
    private List<AcceleratedMoveAnimation> blocksAnimations;

    public BlocksDropAnimation(GameField gameField,
            Collection<Block> droppingBlocks) {
        this.gameField = gameField;

        blocksAnimations = new LinkedList<>();
        for (Block block : droppingBlocks) {
            blocksAnimations.add(block.createDropAnimation());
        }
    }

    @Override
    public void tick() {
        Iterator<AcceleratedMoveAnimation> animationsIterator
                = blocksAnimations.iterator();
        while (animationsIterator.hasNext()) {
            AcceleratedMoveAnimation animation = animationsIterator.next();
            animation.tick();
        }
    }

    @Override
    public void perform(double interpolation) {
        for (AcceleratedMoveAnimation animation : blocksAnimations) {
            animation.perform(interpolation);
        }
    }

    @Override
    public void perform() {
        perform(0.0);
    }

    @Override
    public boolean finished() {
        for (AcceleratedMoveAnimation animation : blocksAnimations) {
            if (!animation.finished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void finish() {
        for (AcceleratedMoveAnimation animation : blocksAnimations) {
            animation.finish();
        }
    }

    @Override
    public int timeLeft() {
        int maxTimeLeft = 0;
        for (Animation  animation : blocksAnimations) {
            if (animation.timeLeft() > maxTimeLeft) {
                maxTimeLeft = animation.timeLeft();
            }
        }
        return maxTimeLeft;
    }
}
