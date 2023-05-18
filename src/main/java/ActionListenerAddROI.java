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
        int y1 = Math.max(Math.min(imageLine.y1, LiveKymographer_.lastImageSynchronized.getHeight()), 0);
        int ta = Math.max(Math.min(kymographLine.y1, LiveKymographer_.kymographImage.getHeight()), 0);
        int tb = Math.max(Math.min(kymographLine.y2, LiveKymographer_.kymographImage.getHeight()), 0);
        int t1 = (int) (Math.min(ta, tb) * (double) LiveKymographer_.lastImageSynchronized.getNFrames() / LiveKymographer_.kymographImage.getHeight());
        int t2 = (int) (Math.max(ta, tb) * (double) LiveKymographer_.lastImageSynchronized.getNFrames() / LiveKymographer_.kymographImage.getHeight());
        int la = Math.max(Math.min(kymographLine.x1, LiveKymographer_.kymographImage.getWidth()), 0);
        int lb = Math.max(Math.min(kymographLine.x2, LiveKymographer_.kymographImage.getWidth()), 0);
        int l1 = Math.min(la, lb);
        int l2 = Math.max(la, lb);
        int width = Line.getWidth();

        int dx = imageLine.x2 - imageLine.x1;
        int dy = imageLine.y2 - imageLine.y1;
        double ux = dx / Math.sqrt((double) dx*dx + dy*dy);
        double uy = dy / Math.sqrt((double) dx*dx + dy*dy);

        int newx1 = (int) Math.round(x1 + ux*l1);
        int newy1 = (int) Math.round(y1 + uy*l1);
        int newx2 = (int) Math.round(x1 + ux*l2);
        int newy2 = (int) Math.round(y1 + uy*l2);

        LiveKymographer_.kymographsCoordinatesTable.addLine(LiveKymographer_.lastImageSynchronized.getTitle(), newx1, newx2, newy1, newy2, t1, t2, width);
        LiveKymographerCalculator.drawKymographLineOnImage(LiveKymographer_.lastImageSynchronized, newx1, newx2, newy1, newy2, t1, t2, width);
        LiveKymographer_.kymographsCoordinatesTable.show();
        if (LiveKymographer_.configuration.generateWhenSaving)
            LiveKymographerCalculator.generateFinalKymograph(LiveKymographer_.lastImageSynchronized, newx1, newx2, newy1, newy2, t1, t2, width);
    }
}
