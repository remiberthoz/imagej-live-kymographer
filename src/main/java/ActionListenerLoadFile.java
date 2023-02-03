import ij.measure.ResultsTable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ActionListenerLoadFile implements ActionListener {

    LiveKymographer_ plugin;

    ActionListenerLoadFile(LiveKymographer_ plugin) {
        this.plugin = plugin;
    }

    public void actionPerformed(ActionEvent e) {
        String filePath = LiveKymographer_.sConfig.loadFilePath;
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
            LiveKymographer_.addKymographLineToTable(plugin.sResultsTable, labels[row], x1, x2, y1, y2, t1, t2, w);
            LiveKymographer_.drawKymographLineOnImage(plugin.sImage, x1, x2, y1, y2, t1, t2, w);
            if (LiveKymographer_.sConfig.generateWhenSaving)
                LiveKymographer_.generateFinalKymograph(plugin.sImage, x1, x2, y1, y2, t1, t2, w);
            plugin.sResultsTable.show("Results from Live Kymographer");
        }
    }
}
