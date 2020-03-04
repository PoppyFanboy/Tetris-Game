package poppyfanboy.tetrisgame.entities;

import poppyfanboy.tetrisgame.util.IntVector;

/**
 * Every object on the game field should implement this interface.
 * Used for convenient collision checks. The coordinates of the object
 * are specified in terms of the game field tiles.
 */
public interface TileFieldObject {
    /**
     * Checks if the specified tile of a game field collides with
     * this object. Returns {@code true} in case the specified tile
     * belongs to this object, returns {@code false} otherwise.
     */
    boolean checkCollision(IntVector collisionTile);
    void tileMove(IntVector newTileCoordinates);
    void tileShift(IntVector shiftDirection);
    IntVector getTileCoords();
}
