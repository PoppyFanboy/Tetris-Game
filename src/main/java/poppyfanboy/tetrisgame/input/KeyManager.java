package poppyfanboy.tetrisgame.input;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.util.*;
import poppyfanboy.tetrisgame.Game;

/**
 * A simple class that manages the mappings of the keys.
 */
public class KeyManager implements KeyListener {
    public static final int DEFAULT_AUTOSHIFT_DELAY
            = (int) (0.4 * Game.TICKS_PER_SECOND);
    public static final int DEFAULT_AUTOFIRE_RATE = 10;

    private EnumMap<InputKey, KeyState> pressedKeyboardKeys;
    private EnumMap<InputKey, Integer> keysHoldTime;
    private List<Controllable> listeners;

    private int autoShiftDelay = DEFAULT_AUTOSHIFT_DELAY;
    private int autofireRate = DEFAULT_AUTOFIRE_RATE;

    public KeyManager() {
        pressedKeyboardKeys = new EnumMap<>(InputKey.class);
        keysHoldTime = new EnumMap<>(InputKey.class);
        for (InputKey key : InputKey.values()) {
            keysHoldTime.put(key, 0);
        }
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
            switch (key.getValue()) {
                case PRESSED:
                    key.setValue(KeyState.HELD);
                    break;
                case HELD:
                    keysHoldTime.computeIfPresent(key.getKey(),
                            (k, v) -> v + 1);
                    if (keysHoldTime.get(key.getKey()) >= autoShiftDelay) {
                        keysHoldTime.put(key.getKey(), 0);
                        key.setValue(KeyState.AUTO_SHIFT);
                    }
                    break;
                case RELEASED:
                    keysHoldTime.computeIfPresent(key.getKey(), (k, v) -> 0);
                    keyIterator.remove();
                    break;
                case AUTO_SHIFT:
                    key.setValue(KeyState.AUTO_SHIFT_HELD);
                    break;
                case AUTO_SHIFT_HELD:
                    keysHoldTime.computeIfPresent(key.getKey(),
                            (k, v) -> v + 1);
                    if (keysHoldTime.get(key.getKey()) >= autofireRate) {
                        keysHoldTime.put(key.getKey(), 0);
                        key.setValue(KeyState.AUTO_SHIFT);
                    }
            }
        }
    }

    public void setAutoShiftDelay(int newDelay) {
        autoShiftDelay = Math.max(0, newDelay);
    }

    public void setAutofireRate(int newDelay) {
        autofireRate = Math.max(0, newDelay);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        InputKey pressedKey = InputKey.getByKeyCode(e.getKeyCode());
        if (pressedKey != null) {
            KeyState keyState = pressedKeyboardKeys.get(pressedKey);
            if (keyState == null || !keyState.isActive()) {
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
