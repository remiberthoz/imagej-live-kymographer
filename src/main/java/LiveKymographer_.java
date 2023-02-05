import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;

/**
 * Live_Kymographer by Rémi Berthoz
 *
 * Inspired from "Dynamic_Profiler" by Wayne Rasband and Michael Schmid.
 */

// Runnable: for background thread
public class LiveKymographer_ implements PlugIn, Runnable {

    protected static String LIVE_KYMOGRAPHER_ROI = "LIVE_KYMOGRAPHER_ROI";

    protected static LiveKymographerConfiguration sConfig = new LiveKymographerConfiguration();
    private LiveKymographerDialog sDialog;

    protected ImagePlus sLastImageSynchronized;
    protected CompositeImage sKymograph;
    protected ResultsTable sResultsTable;

    private Thread bgThread;  // Thread for plotting (in the background)

    /** Called at plugin startup */
    public void run(String arg) {
        ImagePlus image = WindowManager.getCurrentImage();
        if (image == null) {
            IJ.noImage();
            return;
        }

        sKymograph = newKymographComposite(image, "Live kymographer preview", true);
        WindowManager.setCurrentWindow(image.getWindow());
        sResultsTable = new ResultsTable(0);

        bgThread = new Thread(this, "Live Kymographer Computation Thread");
        bgThread.setPriority(Math.max(bgThread.getPriority() - 3, Thread.MIN_PRIORITY));  // Copied from Dynamic_Profiler
        bgThread.start();

        sDialog = new LiveKymographerDialog(sConfig, this);
        positionDialogWindow(image.getWindow(), sKymograph.getWindow(), sDialog);

        LiveKymographerListener listener = new LiveKymographerListener(this, sDialog, sConfig);
        listener.createListeners();

        sDialog.showDialog();

        listener.removeListeners();

        sKymograph.getWindow().close();
        sDialog.dispose();
        bgThread.interrupt();
    }

    static void addKymographLineToTable(ResultsTable table, String label, int x1, int x2, int y1, int y2, int t1, int t2, int w) {
        table.addRow();
        table.addLabel(label);
        table.addValue("x1", x1);
        table.addValue("x2", x2);
        table.addValue("y1", y1);
        table.addValue("y2", y2);
        table.addValue("t1", t1);
        table.addValue("t2", t2);
        table.addValue("w", w);
    }

    static void drawKymographLineOnImage(ImagePlus image, int x1, int x2, int y1, int y2, int t1, int t2, int w) {
        Overlay overlay = image.getOverlay();
        if (overlay == null)
            overlay = new Overlay();

        PointRoi point = new PointRoi(x1, y1);
        Line line = new Line(x1, y1, x2, y2);

        line.setStrokeWidth(w);
        Color strokeColor = Toolbar.getForegroundColor();
        strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 128);
        line.setStrokeColor(strokeColor);
        point.setStrokeColor(strokeColor);

        for (int t = t1; t <= t2; t++) {
            for (int c = 0; c <= image.getNChannels(); c++) {
                for (int z = 0; z <= image.getNSlices(); z++) {
                    Roi lineRoi = (Roi) line.clone();
                    Roi pointRoi = (Roi) point.clone();
                    if (image.isHyperStack()) {
                        lineRoi.setPosition(c+1, z+1, t+1);
                        pointRoi.setPosition(c+1, z+1, t+1);
                    } else {
                        lineRoi.setPosition(image.getStackIndex(c+1, z+1, t+1));
                        pointRoi.setPosition(image.getStackIndex(c+1, z+1, t+1));
                    }
                    overlay.add(lineRoi, LIVE_KYMOGRAPHER_ROI);
                    overlay.add(pointRoi, LIVE_KYMOGRAPHER_ROI);
                }
            }
        }
        image.setOverlay(overlay);
    }

    static void generateFinalKymograph(ImagePlus image, int x1, int x2, int y1, int y2, int t1, int t2, int w) {

        String title = "Kymograph (x1=" + x1 + " x2=" + x2 + " y1=" + y1 + " y2=" + y2 + " t1=" + t1 + " t2=" + t2 + ") of" + image.getShortTitle();
        CompositeImage kymograph = newKymographComposite(image, title, false);

        PolygonRoi line = new PolygonRoi(new float[] {x1, x2}, new float[] {y1, y2}, Roi.POLYLINE);
        syncKymographTo(image, line, kymograph);

        ImageStack kymographStack = kymograph.getStack();
        ImageStack croppedStack = kymographStack.crop(0, t1, 0, kymograph.getWidth(), t2-t1, 1);
        kymograph.setStack(croppedStack);

        Calibration imageCal = image.getCalibration();
        Calibration kymographCal = kymograph.getCalibration();
        kymographCal.pixelHeight = imageCal.frameInterval;
        kymographCal.setYUnit(imageCal.getTimeUnit());
        kymographCal.pixelWidth = imageCal.pixelWidth;
        kymographCal.setXUnit(imageCal.getXUnit());

        kymograph.show();
    }

    static ImageStack makeKymographData(ImagePlus image, PolygonRoi line, int height) {

        int L = (int) line.getUncalibratedLength();
        if (L <= 1)
            return null;

        int T = image.getNFrames();
        int H = (height == 0) ? T : height;
        int C = image.getNChannels();
        int D = image.getBitDepth();
        int z = image.getSlice();

        ImagePlus kymograph = IJ.createHyperStack("", L, H, C, 1, 1, D);
        ImageStack kymographStack = kymograph.getStack();

        for (int c = 0; c < C; c++) {
            ImageProcessor kymographProcessor = kymographStack.getProcessor(image.getStackIndex(c+1, 1, 1));
            Object pixels = kymographProcessor.getPixels();

            for (int h = 0; h < H; h++) {
                // TODO: Optimize by looping on frame instead of h
                int frame = h * T / H;
                ImageProcessor ip = image.getImageStack().getProcessor(image.getStackIndex(c+1, z, frame+1));
                float[] floatPixels = getPixelsOnPolyline(ip, line, c);
                for (int l = 0; l < L; l++) {
                    switch (D) {
                        case 8:
                            ((byte[]) pixels)[h * L + l] = (byte) Math.round(floatPixels[l]);
                            break;
                        case 16:
                            ((short[]) pixels)[h * L + l] = (short) Math.round(floatPixels[l]);
                            break;
                        case 24:
                            ((int[]) pixels)[h * L + l] = (int) Math.round(floatPixels[l]);
                            break;
                        case 32:
                            ((float[]) pixels)[h * L + l] = floatPixels[l];
                            break;
                    }
                }
            }
        }
        return kymograph.getStack();
    }

    static float[] getPixelsOnPolyline(ImageProcessor ip, PolygonRoi polyline, int c) {
        int L = (int) polyline.getUncalibratedLength();
        Polygon polygon = polyline.getPolygon();
        float[] pixels = new float[L];
        int l = 0;
        for (int i = 0; i < polygon.npoints-1; i++) {
            Line line = new Line(polygon.xpoints[i], polygon.ypoints[i], polygon.xpoints[i+1], polygon.ypoints[i+1]);
            line.setWidth((int) polyline.getStrokeWidth());
            float[] linePixels = getPixelsOnLine(ip, line, c);
            for (int j = 0; j < linePixels.length; j++) {
                if (l >= L)
                    IJ.error(LIVE_KYMOGRAPHER_ROI);
                pixels[l] = linePixels[j];
                l+=1;
            }
        }
        return pixels;
    }

    static float[] getPixelsOnLine(ImageProcessor ip, Line line, int c) {
        // NOTE: Since we rely on ImageProcessor.getLine(), we run into
        // trouble when part the line goes outside of the image boundaries.
        int W = line.getWidth();
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

    /** Create an ImagePlus (CompositeImage) for the kymograph, and display it on screen */
    static CompositeImage newKymographComposite(ImagePlus image, String title, boolean position) {
        int L = 512;  // Arbitrary length, will be dynamic when ImagePlus.setStack() is called
        int T = image.getNFrames();
        int C = image.getNChannels();
        CompositeImage kymograph = new CompositeImage(IJ.createHyperStack(title, L, T, C, 1, 1, 32));
        kymograph.show();
        IJ.wait(50);
        positionKymographWindow(image.getWindow(), kymograph.getWindow(), position);
        return kymograph;
    }

    /** Place the dialog window next to the image window and below the kymograph window */
    static void positionDialogWindow(ImageWindow image, ImageWindow kymograph, GenericDialog dialog) {
        int kymographWidth = kymograph.getSize().width;
        int kymographHeight = kymograph.getSize().height;
        int imageWidth = image.getSize().width;
        if (kymographWidth == 0 || imageWidth == 0)
            return;

        Point imageLoc = image.getLocation();

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int x = imageLoc.x + imageWidth + 10;
        if (x + kymographWidth > screenWidth)
            x = screenWidth - kymographWidth;
        int y = imageLoc.y + kymographHeight + 10;

        dialog.setLocation(x, y);
        image.requestFocus();
    }

    /** Place the kymograph window next to the image window (the boolean flag adds a vertical offset) */
    static void positionKymographWindow(ImageWindow image, ImageWindow kymograph, boolean position) {
        // TODO: Replace boolean with typed flag
        int kymographWidth = kymograph.getSize().width;
        int imageWidth = image.getSize().width;
        if (kymographWidth == 0 || imageWidth == 0)
            return;

        Point imageLoc = image.getLocation();

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int x = imageLoc.x + imageWidth + 10;
        if (x + kymographWidth > screenWidth)
            x = screenWidth - kymographWidth;

        if (position) {
            kymograph.setLocation(x, imageLoc.y);
        } else {
            kymograph.setLocation(x, imageLoc.y + 10);
        }

        image.requestFocus();
    }

    /** Get the current line selection (or null if there is none) */
    static PolygonRoi getPolyineSelection(ImagePlus image) {
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
        return polyline;
    }

    /** Can be called by listeners to trigger a kymograph update */
    public void triggerKymographUpdate(ImagePlus image, boolean restoreSelectoin) {
        if ((getPolyineSelection(image) == null) && restoreSelectoin)
            IJ.run(image, "Restore Selection", "");
        synchronized(this) {
            notify();
        }
    }

    public void triggerKymographUpdate(ImagePlus image) {
        triggerKymographUpdate(image, false);
    }

    static public void syncKymographTo(ImagePlus image, PolygonRoi selection, CompositeImage kymograph) {

        if (selection == null)
            return;

        ImageStack data = makeKymographData(image, selection, kymograph.getHeight());
        if (data == null)
            return;
        kymograph.setStack(data);

        kymograph.setPosition(image.getChannel(), 1, 1);
        kymograph.setMode(image.getDisplayMode());
        kymograph.updateAndDraw();  // Required here, such that IJ.COMPOSITE is synchronized on image and kymograph for the check below

        ImageProcessor ip = image.getProcessor();
        ImageProcessor kp = kymograph.getProcessor();
        kymograph.setLuts(image.getLuts());
        kp.setMinAndMax(ip.getMin(), ip.getMax());

        if (image.getDisplayMode() == IJ.COMPOSITE) {
            for (int c = 0; c < image.getNChannels(); c++) {
                ip = ((CompositeImage) image).getProcessor(c+1);
                kp = kymograph.getProcessor(c+1);
                kp.setLut(ip.getLut());
                kp.setMinAndMax(ip.getMin(), ip.getMax());
            }
        }

        kymograph.updateAndDraw();

        Overlay kymographOverlay = new Overlay();

        int frame = (int) ((double) (image.getFrame() - 0.5) / image.getNFrames() * kymograph.getHeight());
        Line line = new Line(0, frame, kymograph.getWidth(), frame);

        line.setStrokeWidth(Math.max(1, kymograph.getHeight() / image.getNFrames()));

        Color strokeColor = Roi.getColor();
        strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 128);
        line.setStrokeColor(strokeColor);

        kymographOverlay.addElement(line);
        kymograph.setOverlay(kymographOverlay);
    }

    /** The background thread for plotting */
    public void run() {
        while (true) {
            ImagePlus image = WindowManager.getCurrentImage();
            if (image != sKymograph) {
                PolygonRoi selection = getPolyineSelection(image);
                syncKymographTo(image, selection, sKymograph);
                sLastImageSynchronized = image;
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
