import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.RoiListener;
import java.awt.AWTEvent;

// RoiListener: to detect changes to the selection of an ImagePlus
// ImageListener: listens to changes (updateAndDraw) and closing of an image
// DialogListener: listens to changes to the dialog control box
class LiveKymographerListener implements RoiListener, ImageListener, DialogListener {

    LiveKymographer_ plugin;
    LiveKymographerConfiguration config;
    GenericDialog dialog;

    LiveKymographerListener(LiveKymographer_ plugin, LiveKymographerDialog dialog, LiveKymographerConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.dialog = dialog;
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        config.generateWhenSaving = gd.getNextBoolean();
        config.loadFilePath = gd.getNextString();
        return true;
    }

    void createListeners() {
        Roi.addRoiListener(this);
        ImagePlus.addImageListener(this);
        dialog.addDialogListener(this);
    }

    void removeListeners() {
        Roi.removeRoiListener(this);
        ImagePlus.removeImageListener(this);
    }

    @Override
    public void imageOpened(ImagePlus imp) {}

    @Override
    public void imageClosed(ImagePlus image) {
        if (image == plugin.sKymograph)
            dialog.dispose();
    }

    @Override
    public void imageUpdated(ImagePlus image) {
        if (image == plugin.sKymograph)
            return;
        plugin.triggerKymographUpdate(image, true);
    }

    @Override
    public void roiModified(ImagePlus image, int id) {
        if (image == null || image == plugin.sKymograph)
            return;
        plugin.triggerKymographUpdate(image, true);
    }

}
