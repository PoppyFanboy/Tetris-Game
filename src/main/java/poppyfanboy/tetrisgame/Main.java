package poppyfanboy.tetrisgame;

import poppyfanboy.tetrisgame.states.Resolution;

public class Main {
	public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        Game game = new Game("test", Resolution._1024x800);
        game.start();
    }
}
