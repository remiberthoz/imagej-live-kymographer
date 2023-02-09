import ij.gui.Line;
import ij.gui.Roi;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ActionListenerAddROI implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        Roi imageRoi = LiveKymographer_.lastImageSynchronized.getRoi();
        Roi kymographRoi = LiveKymographer_.kymographImage.getRoi();
        if (kymographRoi == null || imageRoi == null)
            return;
        if (kymographRoi.getType() != Roi.LINE || imageRoi.getType() != Roi.LINE)
            return;

        Line kymographLine = (Line) kymographRoi;
        Line imageLine = (Line) imageRoi;
        int x1 = Math.max(Math.min(imageLine.x1, LiveKymographer_.lastImageSynchronized.getWidth()), 0);
        int x2 = Math.max(Math.min(imageLine.x2, LiveKymographer_.lastImageSynchronized.getWidth()), 0);
        int y1 = Math.max(Math.min(imageLine.y1, LiveKymographer_.lastImageSynchronized.getHeight()), 0);
        int y2 = Math.max(Math.min(imageLine.y2, LiveKymographer_.lastImageSynchronized.getHeight()), 0);
        int ta = Math.max(Math.min(kymographLine.y1, LiveKymographer_.kymographImage.getHeight()), 0);
        int tb = Math.max(Math.min(kymographLine.y2, LiveKymographer_.kymographImage.getHeight()), 0);
        int t1 = (int) (Math.min(ta, tb) * (double) LiveKymographer_.lastImageSynchronized.getNFrames() / LiveKymographer_.kymographImage.getHeight());
        int t2 = (int) (Math.max(ta, tb) * (double) LiveKymographer_.lastImageSynchronized.getNFrames() / LiveKymographer_.kymographImage.getHeight());
        int la = Math.max(Math.min(kymographLine.x1, LiveKymographer_.kymographImage.getWidth()), 0);
        int lb = Math.max(Math.min(kymographLine.x2, LiveKymographer_.kymographImage.getWidth()), 0);
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

        LiveKymographer_.kymographsCoordinatesTable.addLine(LiveKymographer_.lastImageSynchronized.getTitle(), newx1, newx2, newy1, newy2, t1, t2, w);
        LiveKymographer_.drawKymographLineOnImage(LiveKymographer_.lastImageSynchronized, newx1, newx2, newy1, newy2, t1, t2, w);
        LiveKymographer_.kymographsCoordinatesTable.show();
        if (LiveKymographer_.configuration.generateWhenSaving)
            LiveKymographer_.generateFinalKymograph(LiveKymographer_.lastImageSynchronized, newx1, newx2, newy1, newy2, t1, t2, w);
    }
}
