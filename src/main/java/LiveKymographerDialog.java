import ij.gui.NonBlockingGenericDialog;

class LiveKymographerDialog extends NonBlockingGenericDialog {

    LiveKymographerDialog(LiveKymographerConfiguration config) {
        super("Live Kymographer Controls");
        addCheckbox("Generate kymographs when adding to table", config.generateWhenSaving);
        addButton("Save current line to table", new ActionListenerAddROI());
        addFileField("Load a file (append to table)", "");
        addButton("Load file", new ActionListenerLoadFile());
        addButton("Remove overlays created by this plugin", new ActionListenerRemoveOverlay());
        hideCancelButton();
        setOKLabel("Quit");
    }
}
