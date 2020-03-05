package poppyfanboy.tetrisgame.states;

import java.awt.Graphics2D;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.graphics.Assets;
import poppyfanboy.tetrisgame.input.Controllable;

/**
 * A class that represents a state of the game. At the very least
 * the game consists of the "main menu" state and the "game"
 * state in which the game itself is run.
 *
 * This class also holds the current state of the game, thus acting
 * like a state manager.
 */
public abstract class State {
    private Game game;

    public State(Game game) {
        this.game = game;
    }

    /**
     * Updates the state instance.
     */
    public abstract void tick();
    /**
     * @param   interpolation depicts how far we are between the game
     *          update ticks.
     */
    public abstract void render(Graphics2D g, double interpolation);

    public Game getGame() {
        return game;
    }

    public Assets getAssets() {
        return game.getAssets();
    }
}
