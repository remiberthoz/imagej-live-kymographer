import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ActionListenerLoadFile implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        String filePath = LiveKymographer_.configuration.loadFilePath;
        ResultsTable rt = ResultsTable.open2(filePath);

        String[] labels = rt.getColumnAsStrings("Label");
        for (int row = 0; row < labels.length; row++) {
            int x1 = (int) rt.getValue("x1", row);
            int x2 = (int) rt.getValue("x2", row);
            int y1 = (int) rt.getValue("y1", row);
            int y2 = (int) rt.getValue("y2", row);
            int t1 = (int) rt.getValue("t1", row);
            int t2 = (int) rt.getValue("t2", row);
            int w = (int) rt.getValue("w", row);
            LiveKymographer_.addKymographLineToTable(LiveKymographer_.kymographsCoordinatesTable, labels[row], x1, x2, y1, y2, t1, t2, w);
            ImagePlus image = WindowManager.getImage(labels[row]);
            if (image == null) {
                IJ.log("No image with title \"+ labels[row] +\" found: skipping");
                continue;
            }
            LiveKymographer_.drawKymographLineOnImage(image, x1, x2, y1, y2, t1, t2, w);
            if (LiveKymographer_.configuration.generateWhenSaving)
                LiveKymographer_.generateFinalKymograph(image, x1, x2, y1, y2, t1, t2, w);
            LiveKymographer_.kymographsCoordinatesTable.show("Results from Live Kymographer");
        }
    }
}
