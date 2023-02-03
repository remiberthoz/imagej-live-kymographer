import ij.gui.Overlay;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ActionListenerRemoveOverlay implements ActionListener {

    LiveKymographer_ plugin;

    ActionListenerRemoveOverlay(LiveKymographer_ plugin) {
        this.plugin = plugin;
    }

    public void actionPerformed(ActionEvent e) {
        Overlay overlay = plugin.sImage.getOverlay();
        if (overlay != null)
            overlay.remove(LiveKymographer_.LIVE_KYMOGRAPHER_ROI);
        plugin.sImage.updateAndDraw();
    }
}
