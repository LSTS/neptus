package pt.lsts.neptus.plugins.multibeam.viewers;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;

/**
 * Created by tsm on 05/12/16.
 */
@PluginDescription(author = "Tiago Marques", version = "0.1", name = "Multibeam dual-viewer. Displays multibeam waterfall and cross-section viewers")
@Popup(pos = Popup.POSITION.TOP_LEFT, width = 300, height = 500)
public class MultibeamDualViewer extends ConsolePanel {
    public MultibeamDualViewer(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }
}
