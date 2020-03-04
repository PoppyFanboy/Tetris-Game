package poppyfanboy.tetrisgame.input;

import java.util.EnumMap;

/**
 * Any classes that should be notified when a key is pressed
 * should implement this interface. The currently present controls
 * are passed to the {@link Controllable#control} method.
 */
public interface Controllable {
    void control(EnumMap<InputKey, KeyState> inputs);
}
