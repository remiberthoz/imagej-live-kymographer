import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import java.awt.AWTEvent;

class LiveKymographerDialogListener implements DialogListener {

    LiveKymographerConfiguration config;
    LiveKymographer_ plugin;

    LiveKymographerDialogListener(LiveKymographerConfiguration config, LiveKymographer_ plugin) {
        super();
        this.config = config;
        this.plugin = plugin;
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        config.generateWhenSaving = gd.getNextBoolean();
        config.loadFilePath = gd.getNextString();
        return true;
    }
}
