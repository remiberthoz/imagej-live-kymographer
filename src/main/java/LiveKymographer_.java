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
public class LiveKymographer_ implements PlugIn {

    protected static final String LIVE_KYMOGRAPHER_ROI = "LIVE_KYMOGRAPHER_ROI";

    protected static LiveKymographer_ runningInstance = null;
    protected static LiveKymographerConfiguration configuration = new LiveKymographerConfiguration();
    protected static ImagePlus lastImageSynchronized;
    protected static LiveKymographerComposite kymographImage;
    protected static LiveKymographerResultsTable kymographsCoordinatesTable;

    protected static LiveKymographerCalculator calculatorThread;
    protected static LiveKymographerDialog controlDialog;
    protected static LiveKymographerListener listener;

    private static boolean isRunning() {
        return runningInstance != null;
    }

    /**
     * This method is called when the plugin is loaded. 'arg', which may be
     * blank, is the argument specified for this plugin in IJ_Props.txt.
     */
    @Override
    public void run(String arg) {
        runPlugin(this);
    }

    private static void runPlugin(LiveKymographer_ instance) {
        if (isRunning()) {
            IJ.error("Live Kymographer", "Live Kymographer is already running!");
            return;
        }

        ImagePlus image = WindowManager.getCurrentImage();
        if (image == null) {
            IJ.noImage();
            return;
        }

        runningInstance = instance;
        kymographImage = new LiveKymographerComposite("Live kymographer preview", image.getNFrames());
        kymographsCoordinatesTable = new LiveKymographerResultsTable("Live Kymographer Coordinates Table");

        calculatorThread = LiveKymographerCalculator.make();
        calculatorThread.start();

        controlDialog = new LiveKymographerDialog(configuration);

        listener = new LiveKymographerListener(controlDialog, configuration);
        listener.createListeners();

        WindowManager.setCurrentWindow(image.getWindow());
        IJ.wait(50); // Not sure why, but waiting is required otherwise the dialog does not show
        controlDialog.showDialog();
        // ...
        // Blocks until the dialog is closed
        // ...
        stopPluginAndThread();
    }

    protected static void stopPluginAndThread() {
        runningInstance = null;
        listener.removeListeners();
        if (kymographImage.getWindow() != null)
            kymographImage.getWindow().close();
        controlDialog.dispose();
        calculatorThread.interrupt();
    }

    /** Can be called by listeners to trigger a kymograph update */
    public static void triggerKymographUpdate(ImagePlus image, boolean restoreSelectoin) {
        if ((LiveKymographerKymographSelection.getFrom(image) == null) && restoreSelectoin && image == lastImageSynchronized)
            IJ.run(image, "Restore Selection", "");
        synchronized (runningInstance) {
            runningInstance.notifyAll();
        }
    }
}
