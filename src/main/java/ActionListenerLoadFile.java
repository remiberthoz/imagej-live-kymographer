import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ActionListenerLoadFile implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        String filePath = LiveKymographer_.configuration.loadFilePath;
        LiveKymographer_.kymographsCoordinatesTable.loadFromFile(filePath);
    }
}
