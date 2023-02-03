import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ActionListenerRemoveOverlay implements ActionListener {

    LiveKymographer_ plugin;

    ActionListenerRemoveOverlay(LiveKymographer_ plugin) {
        this.plugin = plugin;
    }

    public void actionPerformed(ActionEvent e) {
        ImagePlus image = WindowManager.getCurrentImage();
        Overlay overlay = image.getOverlay();
        if (overlay != null)
            overlay.remove(LiveKymographer_.LIVE_KYMOGRAPHER_ROI);
        image.updateAndDraw();
    }
}
