package poppyfanboy.tetrisgame.entities;

import java.awt.Graphics2D;

import poppyfanboy.tetrisgame.util.DoubleVector;
import poppyfanboy.tetrisgame.util.Transform;

/**
 * A game entity that can be rendered to the screen. The state of the
 * entity is updated by calling the {@code tick()} method. The entity is
 * painted to the screen using the relative coordinates to handle the case
 * when one entity is inside the other one.
 *
 * The entities are assembled in a tree-like structure where each node
 * represents a single entity.
 */
public abstract class Entity {
    /**
     * Default implementation of the method that obtains a convex hull.
     * The returned array is an array of convex hull points enumerated
     * in the counter-clockwise direction.
     *
     * This method is meant to be used in the general case, when there
     * could be done some optimizations on finding the convex hull
     * of the entity, this method is expected to be overridden.
     */
    public DoubleVector[] getConvexHull() {
        return DoubleVector.getConvexHull(getVertices(), 1e-8);
    }

    public abstract void tick();

    /**
     * Paints the entity to the screen relative to the specified reference
     * point.
     */
    public abstract void render(Graphics2D g, double interpolation);

    /**
     * Returns a set of vertices of the entity which form a polygon that
     * surrounds the entity. There might be duplicates and the returned
     * set of vertices does not necessarily need to form a convex hull.
     */
    public abstract DoubleVector[] getVertices();

    /**
     * Returns a parent-entity of this entity. In case there is none
     * returns just {@code null}.
     */
    public abstract Entity getParentEntity();

    /**
     * Returns a transformation of the 2D plane that the entity imposes.
     * For example, if we'd have a game field entity that would've
     * contained some game objects, the transformation of the game field
     * entity would be the shift vector, that positions the game field
     * on the screen.
     *
     * As we go down the tree of the entities more and more transformations
     * are combined, as opposed to the root-node case, when the only
     * transformation is the one that is directly related to this
     * root-node.
     */
    public abstract Transform getGlobalTransform();

    public abstract Transform getLocalTransform();
}
