import ij.gui.NonBlockingGenericDialog;

class LiveKymographerDialog extends NonBlockingGenericDialog {

    LiveKymographerDialog(LiveKymographerConfiguration config, LiveKymographer_ plugin) {
        super("Live Kymographer Controls");

        addCheckbox("Generate kymographs when adding to table", config.generateWhenSaving);
        addButton("Save current line to table", new ActionListenerAddROI(plugin));
        addFileField("Load a file (append to table)", "");
        addButton("Load file", new ActionListenerLoadFile(plugin));
        addButton("Remove overlays created by this plugin", new ActionListenerRemoveOverlay(plugin));
        hideCancelButton();
        setOKLabel("Quit");
    }
}
