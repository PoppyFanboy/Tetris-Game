package poppyfanboy.tetrisgame.entities.animations;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import poppyfanboy.tetrisgame.entities.Block;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.animation2D.BlockBreakAnimation;

public class BlocksBreakAnimation implements Animation<GameField> {
    private List<BlockBreakAnimation> blocksAnimations;
    private List<Block> brokenBlocks;

    public BlocksBreakAnimation(Collection<Block> brokenBlocks,
            int duration) {
        this.brokenBlocks = new LinkedList<>(brokenBlocks);

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
    public void perform(GameField object, double interpolation) {
        Iterator<BlockBreakAnimation> animationsIterator
                = blocksAnimations.iterator();
        Iterator<Block> blocksIterator = brokenBlocks.iterator();
        while (animationsIterator.hasNext()) {
            BlockBreakAnimation animation = animationsIterator.next();
            Block block = blocksIterator.next();
            animation.perform(block, interpolation);
        }
    }

    @Override
    public void perform(GameField object) {
        perform(object, 0.0);
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
    public void finish(GameField object) {
        Iterator<BlockBreakAnimation> animationsIterator
                = blocksAnimations.iterator();
        Iterator<Block> blocksIterator = brokenBlocks.iterator();
        while (animationsIterator.hasNext()) {
            BlockBreakAnimation animation = animationsIterator.next();
            Block block = blocksIterator.next();
            animation.finish(block);
        }
    }

    @Override
    public int timeLeft() {
        return 0;
    }
}
