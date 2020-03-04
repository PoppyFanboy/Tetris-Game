package poppyfanboy.tetrisgame.states;

import java.awt.Graphics2D;
import java.util.EnumMap;

import poppyfanboy.tetrisgame.Game;
import poppyfanboy.tetrisgame.input.InputKey;
import poppyfanboy.tetrisgame.input.KeyState;

public class MenuState extends State {
    public MenuState(Game game) {
        super(game);
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(Graphics2D g, double leftover) {
    }

    @Override
    public void control(EnumMap<InputKey, KeyState> inputs) {

    }
}
