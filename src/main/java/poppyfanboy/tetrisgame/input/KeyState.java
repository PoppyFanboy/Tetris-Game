package poppyfanboy.tetrisgame.input;

/**
 * State of the key in terms of the current game tick.
 */
public enum KeyState {
    // the key has been pressed during the current tick
    PRESSED,
    // the key has been released during the current tick
    RELEASED,
    // the key is pressed during the whole current tick
    HELD;
}
