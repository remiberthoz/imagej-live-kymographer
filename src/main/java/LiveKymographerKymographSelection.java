import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Polygon;

public class LiveKymographerKymographSelection {

    PolygonRoi polyline;

    public LiveKymographerKymographSelection(PolygonRoi polyline) {
        this.polyline = polyline;
    }

    public LiveKymographerKymographSelection(int x1, int x2, int y1, int y2) {
        this.polyline = new PolygonRoi(new float[] {x1, x2}, new float[] {y1, y2}, Roi.POLYLINE);
    }

    /** Get the current line selection (or null if there is none) */
    protected static LiveKymographerKymographSelection getFrom(ImagePlus image) {
        Roi roi = image.getRoi();
        if (roi == null)
            return null;
        PolygonRoi polyline;
        switch (roi.getType()) {
            case Roi.LINE:
                Line line = (Line) roi.clone();
                float[] xPoints = {line.x1, line.x2};
                float[] yPoints = {line.y1, line.y2};
                polyline = new PolygonRoi(xPoints, yPoints, 2, Roi.POLYLINE);
                break;
            case Roi.POLYLINE:
                polyline = (PolygonRoi) roi.clone();
                break;
            default:
                return null;
        }

        return new LiveKymographerKymographSelection(polyline);
    }

    public int getLength() {
        return (int) polyline.getUncalibratedLength();
    }

    public float[] getPixels(ImageProcessor ip, int channel) {
        int L = (int) polyline.getUncalibratedLength();
        Polygon polygon = polyline.getPolygon();
        float[] pixels = new float[L];
        int l = 0;
        for (int i = 0; i < polygon.npoints-1; i++) {
            Line line = new Line(polygon.xpoints[i], polygon.ypoints[i], polygon.xpoints[i+1], polygon.ypoints[i+1]);
            float[] linePixels = getPixelsOnLine(ip, line, (int) polyline.getStrokeWidth());
            for (int j = 0; j < linePixels.length; j++) {
                if (l >= L)
                    IJ.error(LiveKymographer_.LIVE_KYMOGRAPHER_ROI);
                pixels[l] = linePixels[j];
                l+=1;
            }
        }
        return pixels;
    }

    private static float[] getPixelsOnLine(ImageProcessor ip, Line line, int width) {
        // NOTE: Since we rely on ImageProcessor.getLine(), we run into
        // trouble when part the line goes outside of the image boundaries.
        int W = width;
        if (W % 2 == 0)
            W = W + 1;
        int L = (int) line.getRawLength();
        int Lx = line.x2 - line.x1;
        int Ly = line.y2 - line.y1;
        double Ux = -Ly / Math.sqrt(Lx*Lx + Ly*Ly);
        double Uy = Lx / Math.sqrt(Lx*Lx + Ly*Ly);

        float[] pixels = new float[L];
        for (int w = -W/2; w <= W/2; w++) {
            int x1 = (int) Math.min(Math.max(line.x1 + w*Ux, 0), ip.getWidth()-1);
            int x2 = (int) Math.min(Math.max(line.x2 + w*Ux, 0), ip.getWidth()-1);
            int y1 = (int) Math.min(Math.max(line.y1 + w*Uy, 0), ip.getHeight()-1);
            int y2 = (int) Math.min(Math.max(line.y2 + w*Uy, 0), ip.getHeight()-1);
            double[] profile = ip.getLine(x1, y1, x2, y2);
            for (int l = 0; l < Math.min(L, profile.length); l++) {
                if (w == -W / 2)
                    pixels[l] = 0;
                pixels[l] = pixels[l] + ((float) profile[l]) / W;
            }
        }
        return pixels;
    }
}
