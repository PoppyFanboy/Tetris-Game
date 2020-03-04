package poppyfanboy.tetrisgame.entities;

import java.util.Collection;

/**
 * A class that should be implemented by the object that represents
 * a tile field. In this case this is just a {@code GameField} object.
 */
public interface TileField {
    int getWidthInBlocks();
    int getHeightInBlocks();

    /**
     * Returns a set of objects currently present on the game field.
     * It may be useful for the collision detection: if the object on
     * a tile field has a reference to its tile field, it can easily
     * check, if it collides with any of the objects on the field.
     */
    Collection<? extends TileFieldObject> getObjects();
}
