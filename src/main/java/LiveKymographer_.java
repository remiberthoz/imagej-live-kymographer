import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/**
 * Live_Kymographer by Rémi Berthoz
 *
 * Inspired from "Dynamic_Profiler" by Wayne Rasband and Michael Schmid.
 */

// Runnable: for background thread
public class LiveKymographer_ implements PlugIn, Runnable {

    protected static final String LIVE_KYMOGRAPHER_ROI = "LIVE_KYMOGRAPHER_ROI";

    private static LiveKymographer_ runningInstance = null;
    protected static LiveKymographerConfiguration configuration = new LiveKymographerConfiguration();
    protected static ImagePlus lastImageSynchronized;
    protected static LiveKymographerComposite kymographImage;
    protected static LiveKymographerResultsTable kymographsCoordinatesTable;

    private static Thread bgThread;
    private static LiveKymographerDialog controlDialog;
    private static LiveKymographerListener listener;

    private static boolean isRunning() {
        return runningInstance != null;
    }

    /** Called at plugin startup */
    @Override
    public void run(String arg) {
        if (isRunning()) {
            IJ.error("Live Kymographer", "Live Kymographer is already running!");
            return;
        }

        ImagePlus image = WindowManager.getCurrentImage();
        if (image == null) {
            IJ.noImage();
            return;
        }

        runningInstance = this;
        kymographImage = new LiveKymographerComposite("Live kymographer preview", image.getNFrames());
        kymographsCoordinatesTable = new LiveKymographerResultsTable("Live Kymographer Coordinates Table");

        bgThread = new Thread(this, "Live Kymographer Computation Thread");
        bgThread.setPriority(Math.max(bgThread.getPriority()-3, Thread.MIN_PRIORITY)); // Copied from Dynamic_Profiler
        bgThread.start();

        controlDialog = new LiveKymographerDialog(configuration);

        listener = new LiveKymographerListener(controlDialog, configuration);
        listener.createListeners();

        kymographImage.show();
        WindowManager.setCurrentWindow(image.getWindow());
        IJ.wait(50); // Not sure why, but waiting is required otherwise the dialog does not show
        controlDialog.showDialog();
        // Blocks until the dialog is closed

        quit();
    }

    protected static void quit() {
        runningInstance = null;
        listener.removeListeners();
        kymographImage.getWindow().close();
        controlDialog.dispose();
        bgThread.interrupt();
    }

    static void drawKymographLineOnImage(ImagePlus image, int x1, int x2, int y1, int y2, int t1, int t2, int w) {
        Overlay overlay = image.getOverlay();
        if (overlay == null)
            overlay = new Overlay();
        Line line = makeKymographLine(x1, x2, y1, y2, w);
        PointRoi point = makeKymographPoint(x1, y1);
        for (int t = t1; t <= t2; t++) {
            for (int c = 0; c <= image.getNChannels(); c++) {
                for (int z = 0; z <= image.getNSlices(); z++) {
                    Roi lineRoi = (Roi) line.clone();
                    Roi pointRoi = (Roi) point.clone();
                    positionRoiOnImagePlus(image, lineRoi, c, z, t);
                    positionRoiOnImagePlus(image, pointRoi, c, z, t);
                    overlay.add(lineRoi, LIVE_KYMOGRAPHER_ROI);
                    overlay.add(pointRoi, LIVE_KYMOGRAPHER_ROI);
                }
            }
        }
        image.setOverlay(overlay);
    }

    static PointRoi makeKymographPoint(int x, int y) {
        PointRoi point = new PointRoi(x, y);
        Color strokeColor = Toolbar.getForegroundColor();
        strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 128);
        point.setStrokeColor(strokeColor);
        return point;
    }

    static Line makeKymographLine(int x1, int x2, int y1, int y2, int w) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStrokeWidth(w);
        Color strokeColor = Toolbar.getForegroundColor();
        strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 128);
        line.setStrokeColor(strokeColor);
        return line;
    }

    static void positionRoiOnImagePlus(ImagePlus image, Roi roi, int c, int z, int t) {
        if (image.isHyperStack())
            roi.setPosition(c+1, z+1, t+1);
        else
            roi.setPosition(image.getStackIndex(c+1, z+1, t+1));
    }

    static void generateFinalKymograph(ImagePlus image, int x1, int x2, int y1, int y2, int t1, int t2, int w) {
        String title = "Kymograph (x1=" + x1 + " x2=" + x2 + " y1=" + y1 + " y2=" + y2 + " t1=" + t1 + " t2=" + t2 + ") of " + image.getShortTitle();
        LiveKymographerComposite kymograph = new LiveKymographerComposite(title, image.getNFrames());
        syncKymographTo(image, new LiveKymographerKymographSelection(x1, x2, y1, y2), kymograph);
        kymograph.setOverlay(null); // Remove the time indicator added by syncKymographTo()
        Roi cropRoi = new Roi(1, t1, kymograph.getWidth(), t2-t1);
        kymograph.setRoi(cropRoi);
        kymograph.setStack(kymograph.crop("stack").getStack());
        kymograph.show();
    }

    static ImageStack makeKymographData(ImagePlus image, LiveKymographerKymographSelection selection, int height) {
        int L = selection.getLength();
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
                int frame = h * T/H;
                ImageProcessor ip = image.getImageStack().getProcessor(image.getStackIndex(c+1, z, frame+1));
                float[] floatPixels = selection.getPixels(ip);
                for (int l = 0; l < L; l++) {
                    switch (D) {
                        case 8:
                            ((byte[]) pixels)[h*L + l] = (byte) Math.round(floatPixels[l]);
                            break;
                        case 16:
                            ((short[]) pixels)[h*L + l] = (short) Math.round(floatPixels[l]);
                            break;
                        case 24:
                            ((int[]) pixels)[h*L + l] = Math.round(floatPixels[l]);
                            break;
                        case 32:
                            ((float[]) pixels)[h*L + l] = floatPixels[l];
                            break;
                        default:
                            throw new IllegalStateException("Unknown image type");
                    }
                }
            }
        }
        return kymograph.getStack();
    }

    /** Can be called by listeners to trigger a kymograph update */
    public static void triggerKymographUpdate(ImagePlus image, boolean restoreSelectoin) {
        if ((LiveKymographerKymographSelection.getFrom(image) == null) && restoreSelectoin && image == lastImageSynchronized)
            IJ.run(image, "Restore Selection", "");
        synchronized (runningInstance) {
            runningInstance.notifyAll();
        }
    }

    public static void syncKymographTo(ImagePlus image, LiveKymographerKymographSelection selection, LiveKymographerComposite kymograph) {
        if (selection == null)
            return;
        ImageStack data = makeKymographData(image, selection, kymograph.getHeight());
        if (data == null)
            return;
        kymograph.setStack(data, image.getNChannels(), 1, 1);
        kymograph.updateAndDraw();
        kymograph.syncChannelDisplay(image);
        kymograph.syncTimeIndicator(image);
        kymograph.syncCalibration(image);
        kymograph.updateAndDraw();
    }

    /** The background thread for plotting */
    @Override
    public void run() {
        while (true) {
            ImagePlus image = WindowManager.getCurrentImage();
            if (image != kymographImage && image.getNFrames() > 1) {
                LiveKymographerKymographSelection selection = LiveKymographerKymographSelection.getFrom(image);
                syncKymographTo(image, selection, kymographImage);
                lastImageSynchronized = image;
            }
            synchronized (runningInstance) {
                try {
                    runningInstance.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
