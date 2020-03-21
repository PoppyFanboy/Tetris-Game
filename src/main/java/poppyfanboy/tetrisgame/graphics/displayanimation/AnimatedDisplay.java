package poppyfanboy.tetrisgame.graphics.displayanimation;

import java.util.function.Function;

public interface AnimatedDisplay {
    // transition between images on the screen
    void setTransitionProgress(double progress);
    // noise
    void setNoiseDensity(double noiseDensity);
    // distortion
    void setDistortion(Function<Double, Double> distortionFunction);
    void setDistortionProgress(double progress);
    void setDistortionIntensity(double intensity);
}
