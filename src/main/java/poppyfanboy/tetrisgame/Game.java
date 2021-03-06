package poppyfanboy.tetrisgame;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.io.IOException;

import poppyfanboy.tetrisgame.graphics.Display;
import poppyfanboy.tetrisgame.input.KeyManager;
import poppyfanboy.tetrisgame.states.*;

/**
 * A game instance. This class takes care of creating a window for the game
 * and every other aspect of running the game. The game loop is also
 * located here.
 */
public class Game implements Runnable {
    // how many times per second *the game* is updated
    public static final int TICKS_PER_SECOND = 50;
    // max time for each game update
    private static final long SKIP_TICKS = 1_000_000_000 / TICKS_PER_SECOND;
    // how many times in a row the game can update without rendering
    // (so that on slow computers the game would run consistently)
    private static final int MAX_FRAMESKIP = 5;

    private Display display;
    private Resolution resolution;

    private final String title;

    // separate thread for the game state
    private Thread thread;
    private boolean running;

    // game states
    private State currentState;
    private State gameState;
    private State menuState;

    // key manager
    private KeyManager keyManager;

    public Game(String title, Resolution resolution) {
        this.resolution = resolution;
        this.title = title;
        keyManager = new KeyManager();
    }

    public synchronized void start() {
        // the game is already running
        if (running) {
            return;
        }

        running = true;
        // run the game in a new thread
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        // the game is already stopped
        if (!running) {
            return;
        }
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    // initialize the graphics, load the assets, create the game states
    private void init() throws IOException {
        display
            = new Display(title, resolution.getWidth(), resolution.getHeight());
        display.getFrame().addKeyListener(keyManager);
        gameState = new GameState(this);
        menuState = new MenuState(this);
        currentState = gameState;
    }

    /**
     * The main method of the game that contains the game loop and
     * graphics initialization.
     * Game loop:
     *  1. update the variables, positions of objects, etc
     *  2. render everything to the screen
     *  3. go to step 1
     */
    @Override
    public void run() {
        try {
            init();
        } catch (IOException ex) {
            stop();
            ex.printStackTrace();
            return;
        }
        // next time to update the game
        long nextGameTick = System.nanoTime();

        while (running) {
            int frameSkipCount = 0;
            while (System.nanoTime() > nextGameTick
                    && frameSkipCount < MAX_FRAMESKIP) {
                tick();
                nextGameTick += SKIP_TICKS;
                frameSkipCount++;
            }
            double interpolation = ((double) (System.nanoTime()
                - nextGameTick + SKIP_TICKS)) / SKIP_TICKS;
            render(interpolation);
        }
        stop();
    }
    
    // update the game state
    private void tick() {
        if (currentState != null) {
            currentState.tick();
        }
        keyManager.tick();
    }
    
    /**
     * Renders the active game state to the screen.
     */
    private void render(double interpolation) {
        BufferStrategy bs = display.getCanvas().getBufferStrategy();
        if (bs == null) {
            // if the canvas does not have a BS, create one
            display.getCanvas().createBufferStrategy(3);
            return;
        }
        // graphics
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // clear the screen
        g.clearRect(0, 0, resolution.getWidth(), resolution.getHeight());
        if (currentState != null) {
            currentState.render(g, interpolation);
        }
        bs.show();
        g.dispose();
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public Resolution getResolution() {
        return resolution;
    }
}
