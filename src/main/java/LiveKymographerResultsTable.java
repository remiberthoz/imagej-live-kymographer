import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;

public class LiveKymographerResultsTable {

    private ResultsTable resultsTable;
    private String title;

    protected LiveKymographerResultsTable(String title) {
        resultsTable = new ResultsTable(0);
        this.title = title;
    }

    protected void addLine(String label, int x1, int x2, int y1, int y2, int t1, int t2, int w) {
        resultsTable.addRow();
        resultsTable.addLabel(label);
        resultsTable.addValue("x1", x1);
        resultsTable.addValue("x2", x2);
        resultsTable.addValue("y1", y1);
        resultsTable.addValue("y2", y2);
        resultsTable.addValue("t1", t1);
        resultsTable.addValue("t2", t2);
        resultsTable.addValue("w", w);
    }

    protected void loadFromFile(String filePath) {
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
            this.addLine(labels[row], x1, x2, y1, y2, t1, t2, w);
            ImagePlus image = WindowManager.getImage(labels[row]);
            if (image == null) {
                IJ.log("No image with title \"+ labels[row] +\" found: skipping");
                continue;
            }
            LiveKymographer_.drawKymographLineOnImage(image, x1, x2, y1, y2, t1, t2, w);
            if (LiveKymographer_.configuration.generateWhenSaving)
                LiveKymographer_.generateFinalKymograph(image, x1, x2, y1, y2, t1, t2, w);
            LiveKymographer_.kymographsCoordinatesTable.show();
        }
    }

    public void show() {
        resultsTable.show(title);
    }
}
