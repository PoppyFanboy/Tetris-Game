package poppyfanboy.tetrisgame;

public class Main {
	public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        Game game = new Game("test", 360, 680, 32);
        game.start();
    }
}
