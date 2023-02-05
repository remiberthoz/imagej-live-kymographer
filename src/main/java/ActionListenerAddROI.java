import ij.gui.Line;
import ij.gui.Roi;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class ActionListenerAddROI implements ActionListener {

    LiveKymographer_ plugin;

    ActionListenerAddROI(LiveKymographer_ plugin) {
        this.plugin = plugin;
    }

    public void actionPerformed(ActionEvent e) {
        Roi imageRoi = plugin.sLastImageSynchronized.getRoi();
        Roi kymographRoi = plugin.sKymograph.getRoi();
        if (kymographRoi == null || imageRoi == null)
            return;
        if (kymographRoi.getType() != Roi.LINE || imageRoi.getType() != Roi.LINE)
            return;

        Line kymographLine = (Line) kymographRoi;
        Line imageLine = (Line) imageRoi;
        int x1 = Math.max(Math.min(imageLine.x1, plugin.sLastImageSynchronized.getWidth()), 0);
        int x2 = Math.max(Math.min(imageLine.x2, plugin.sLastImageSynchronized.getWidth()), 0);
        int y1 = Math.max(Math.min(imageLine.y1, plugin.sLastImageSynchronized.getHeight()), 0);
        int y2 = Math.max(Math.min(imageLine.y2, plugin.sLastImageSynchronized.getHeight()), 0);
        int ta = Math.max(Math.min(kymographLine.y1, plugin.sKymograph.getHeight()), 0);
        int tb = Math.max(Math.min(kymographLine.y2, plugin.sKymograph.getHeight()), 0);
        int t1 = (int) (Math.min(ta, tb) * (double) plugin.sLastImageSynchronized.getNFrames() / plugin.sKymograph.getHeight());
        int t2 = (int) (Math.max(ta, tb) * (double) plugin.sLastImageSynchronized.getNFrames() / plugin.sKymograph.getHeight());
        int la = Math.max(Math.min(kymographLine.x1, plugin.sKymograph.getWidth()), 0);
        int lb = Math.max(Math.min(kymographLine.x2, plugin.sKymograph.getWidth()), 0);
        int l1 = Math.min(la, lb);
        int l2 = Math.max(la, lb);
        int w = imageLine.getWidth();

        int Lx = imageLine.x2 - imageLine.x1;
        int Ly = imageLine.y2 - imageLine.y1;
        double Ux = Lx / Math.sqrt(Lx*Lx + Ly*Ly);
        double Uy = Ly / Math.sqrt(Lx*Lx + Ly*Ly);

        int newx1 = (int) Math.round(x1 + Ux * (double) l1);
        int newy1 = (int) Math.round(y1 + Uy * (double) l1);
        int newx2 = (int) Math.round(x1 + Ux * (double) l2);
        int newy2 = (int) Math.round(y1 + Uy * (double) l2);

        LiveKymographer_.addKymographLineToTable(plugin.sResultsTable, plugin.sLastImageSynchronized.getTitle(), newx1, newx2, newy1, newy2, t1, t2, w);
        LiveKymographer_.drawKymographLineOnImage(plugin.sLastImageSynchronized, newx1, newx2, newy1, newy2, t1, t2, w);
        plugin.sResultsTable.show("Results from Live Kymographer");
        if (LiveKymographer_.sConfig.generateWhenSaving)
            LiveKymographer_.generateFinalKymograph(plugin.sLastImageSynchronized, newx1, newx2, newy1, newy2, t1, t2, w);
    }
}
