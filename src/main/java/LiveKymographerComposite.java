import ij.CompositeImage;
import ij.ImagePlus;
import ij.IJ;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Color;

public class LiveKymographerComposite extends CompositeImage {

    LiveKymographerComposite(String title, int height) {
        // width, channels and bitdepth are reset when the composite is updated
        super(IJ.createHyperStack(title, 1, height, 1, 1, 1, 32));
    }

    protected void syncChannelDisplay(ImagePlus image) {
        this.setPosition(image.getChannel(), 1, 1);
        this.setDisplayMode(image.getDisplayMode());
        this.updateAndDraw();  // Required here, such that IJ.COMPOSITE is synchronized on image and kymograph for the check below

        ImageProcessor ip = image.getProcessor();
        ImageProcessor kp = this.getProcessor();
        this.setLuts(image.getLuts());
        kp.setMinAndMax(ip.getMin(), ip.getMax());

        if (image.getDisplayMode() == IJ.COMPOSITE) {
            for (int c = 0; c < image.getNChannels(); c++) {
                ip = ((CompositeImage) image).getProcessor(c+1);
                kp = this.getProcessor(c+1);
                kp.setLut(ip.getLut());
                kp.setMinAndMax(ip.getMin(), ip.getMax());
            }
        }
    }

    protected void syncTimeIndicator(ImagePlus image) {
        float indicatorTemporalPosition = (image.getFrame() - 0.5f) / image.getNFrames();
        int indicatorVPixelPosition = Math.round(indicatorTemporalPosition * this.height);

        Line line = new Line(0, indicatorVPixelPosition, this.width, indicatorVPixelPosition);
        Color strokeColor = Roi.getColor();
        strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 128);
        line.setStrokeColor(strokeColor);
        line.setStrokeWidth(1);

        Overlay kymographOverlay = new Overlay();
        kymographOverlay.addElement(line);
        this.setOverlay(kymographOverlay);
    }
}
