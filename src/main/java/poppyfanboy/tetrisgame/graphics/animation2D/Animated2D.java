package poppyfanboy.tetrisgame.graphics.animation2D;

import poppyfanboy.tetrisgame.util.DoubleVector;

/**
 * An interface implemented by any entity that can handle animations.
 * The basic ones are rotation and smooth movement. Animations objects
 * are added to the entity, and next N game ticks, that they last,
 * they control the appeal of the entity through the public methods
 * of this interface. In case multiple animations added they are somehow
 * evaluated in a way that some animations might have smaller priority
 * than the other, so they will have stronger affect on the appeal of the
 * object.
 */
public interface Animated2D {
    void setCoords(DoubleVector newCoords);
    DoubleVector getCoords();

    void setRotationAngle(double newRotationAngle);
    double getRotationAngle();

    /**
     * @throws  IllegalArgumentException in case {@code newScale} argument
     *          is negative.
     */
    void setScale(double newScale);
    double getScale();

    /**
     * @throws  IllegalArgumentException in case {@code newOpacity}
     *          argument does not lie within the {@code [0, 1]} interval.
     */
    void setOpacity(double newOpacity);
    double getOpacity();

    void setBrightness(double newBrightness);
}
