package poppyfanboy.tetrisgame.input;

import java.awt.event.KeyEvent;

public enum InputKey {
    ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
    W, A, S, D,
    R,
    SPACE, ENTER;

    /**
     * Maps a raw keyboard key code to the {@code InputKey} enum instance.
     */
    public static InputKey getByKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
                return ARROW_UP;
            case KeyEvent.VK_DOWN:
                return ARROW_DOWN;
            case KeyEvent.VK_LEFT:
                return ARROW_LEFT;
            case KeyEvent.VK_RIGHT:
                return ARROW_RIGHT;
            case KeyEvent.VK_W:
                return W;
            case KeyEvent.VK_S:
                return S;
            case KeyEvent.VK_A:
                return A;
            case KeyEvent.VK_D:
                return D;
            case KeyEvent.VK_R:
                return R;
            case KeyEvent.VK_SPACE:
                return SPACE;
            case KeyEvent.VK_ENTER:
                return ENTER;
            default:
                return null;
        }
    }
}
