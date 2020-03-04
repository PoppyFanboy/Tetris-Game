package poppyfanboy.tetrisgame.input;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.util.*;

/**
 * A simple class that manages the mappings of the keys.
 */
public class KeyManager implements KeyListener {
    private EnumMap<InputKey, KeyState> pressedKeyboardKeys;
    private List<Controllable> listeners;

    public KeyManager() {
        pressedKeyboardKeys = new EnumMap<>(InputKey.class);
        listeners = new LinkedList<>();
    }

    /**
     * Add a new listener that will be provided with the current tick
     * inputs. It allows duplicate listeners so to not to use the hash
     * sets, just an ordered list, thus the listeners are notified in a
     * deterministic order.
     */
    public void addListener(Controllable listener) {
        listeners.add(listener);
    }

    /**
     * Checks, if the specified listener is present in this key manager
     * instance.
     */
    public boolean hasListener(Controllable listener) {
        return listeners.contains(listener);
    }

    /**
     * Removes the given listener from the key manager. Returns
     * {@code false} in case the listener s not present in this
     * key manager, returns {@code true} otherwise.
     */
    public boolean removeListener(Controllable listener) {
        return listeners.remove(listener);
    }

    // triggers the callbacks and updates the keys
    public void tick() {
        EnumMap<InputKey, KeyState> data
            = new EnumMap<>(pressedKeyboardKeys);
        for (Controllable listener : listeners) {
            listener.control(data);
        }
        // update the key states
        Iterator<EnumMap.Entry<InputKey, KeyState>> keyIterator
            = pressedKeyboardKeys.entrySet().iterator();
        while (keyIterator.hasNext()) {
            EnumMap.Entry<InputKey, KeyState> key = keyIterator.next();
            if (key.getValue() == KeyState.PRESSED) {
                key.setValue(KeyState.HELD);
            }
            if (key.getValue() == KeyState.RELEASED) {
                keyIterator.remove();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        InputKey pressedKey = InputKey.getByKeyCode(e.getKeyCode());
        if (pressedKey != null) {
            KeyState keyState = pressedKeyboardKeys.get(pressedKey);
            if (keyState != KeyState.HELD) {
                pressedKeyboardKeys.put(pressedKey, KeyState.PRESSED);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        InputKey pressedKey = InputKey.getByKeyCode(e.getKeyCode());
        if (pressedKey != null) {
            pressedKeyboardKeys.put(pressedKey, KeyState.RELEASED);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // process the typed unicode character
        // (use later for typing the username or something like that)
    }
}
