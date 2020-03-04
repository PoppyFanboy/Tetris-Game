package poppyfanboy.tetrisgame.graphics;

import javax.swing.JFrame;

import java.awt.Dimension;
import java.awt.Canvas;

/**
 * A {@code JFrame} wrapper that displays the game.
 */
public class Display {
    private JFrame frame;
    // everything is drawn on the canvas
    private Canvas canvas;
    private String title;
    private int width, height;
    
    public Display(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;

        createDisplay();
    }

    private void createDisplay() {
        // setup the jFrame
        frame = new JFrame();
        frame.setTitle(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // setup the canvas
        canvas = new Canvas();
        canvas.setMaximumSize(new Dimension(width, height));
        canvas.setMinimumSize(new Dimension(width, height));
        canvas.setPreferredSize(new Dimension(width, height));
        // so that you could not focus on a canvas but rather
        // focus on the frame
        canvas.setFocusable(false);
        frame.add(canvas);
        // resizes the window so that we would be able to see the
        // canvas fully
        frame.pack();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public JFrame getFrame() {
        return frame;
    }
}
