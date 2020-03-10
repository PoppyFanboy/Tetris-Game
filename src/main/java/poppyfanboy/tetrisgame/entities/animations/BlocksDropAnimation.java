package poppyfanboy.tetrisgame.entities.animations;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import poppyfanboy.tetrisgame.entities.Block;
import poppyfanboy.tetrisgame.entities.GameField;
import poppyfanboy.tetrisgame.graphics.Animation;
import poppyfanboy.tetrisgame.graphics.animation2D.AcceleratedMoveAnimation;

public class BlocksDropAnimation implements Animation<GameField> {
    private List<AcceleratedMoveAnimation> blocksAnimations;
    private List<Block> droppingBlocks;

    public BlocksDropAnimation(Collection<Block> droppingBlocks) {
        this.droppingBlocks = new LinkedList<>(droppingBlocks);

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
    public void perform(GameField object, double interpolation) {
        Iterator<AcceleratedMoveAnimation> animationsIterator
                = blocksAnimations.iterator();
        Iterator<Block> blocksIterator = droppingBlocks.iterator();
        while (animationsIterator.hasNext()) {
            AcceleratedMoveAnimation animation = animationsIterator.next();
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
        for (AcceleratedMoveAnimation animation : blocksAnimations) {
            if (!animation.finished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void finish(GameField object) {
        Iterator<AcceleratedMoveAnimation> animationsIterator
                = blocksAnimations.iterator();
        Iterator<Block> blocksIterator = droppingBlocks.iterator();
        while (animationsIterator.hasNext()) {
            AcceleratedMoveAnimation animation = animationsIterator.next();
            Block block = blocksIterator.next();
            animation.finish(block);
        }
    }

    @Override
    public int timeLeft() {
        return 0;
    }
}
