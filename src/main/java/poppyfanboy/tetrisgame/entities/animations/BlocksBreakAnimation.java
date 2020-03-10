package poppyfanboy.tetrisgame.entities.animations;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import poppyfanboy.tetrisgame.entities.Block;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.animation2D.BlockBreakAnimation;

public class BlocksBreakAnimation implements Animation {
    private final GameField gameField;
    private List<BlockBreakAnimation> blocksAnimations;

    public BlocksBreakAnimation(GameField gameField,
            Collection<Block> brokenBlocks, int duration) {
        this.gameField = gameField;

        blocksAnimations = new LinkedList<>();
        for (Block block : brokenBlocks) {
            blocksAnimations.add(block.createBlockBreakAnimation(duration));
        }
    }

    @Override
    public void tick() {
        Iterator<BlockBreakAnimation> animationsIterator
                = blocksAnimations.iterator();
        while (animationsIterator.hasNext()) {
            BlockBreakAnimation animation = animationsIterator.next();
            animation.tick();
        }
    }

    @Override
    public void perform(double interpolation) {
        for (BlockBreakAnimation animation : blocksAnimations) {
            animation.perform(interpolation);
        }
    }

    @Override
    public void perform() {
        perform(0.0);
    }

    @Override
    public boolean finished() {
        for (BlockBreakAnimation animation : blocksAnimations) {
            if (!animation.finished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void finish() {
        for (BlockBreakAnimation animation : blocksAnimations) {
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
