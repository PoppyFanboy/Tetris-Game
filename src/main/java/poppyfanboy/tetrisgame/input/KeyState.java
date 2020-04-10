package poppyfanboy.tetrisgame.input;

/**
 * State of the key in terms of the current game tick.
 */
public enum KeyState {
    // the key has been pressed during the current tick
    PRESSED,
    // autoshift timer has elapsed and now the key automatically fires
    // every N ticks
    AUTO_SHIFT, AUTO_SHIFT_HELD,
    // the key has been released during the current tick
    RELEASED,
    // the key is pressed during the whole current tick
    HELD;

    public boolean isActive() {
        return this != RELEASED;
    }

    public boolean fired() {
        return this == PRESSED || this == AUTO_SHIFT;
    }
}
