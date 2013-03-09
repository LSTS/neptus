/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 3, 2011
 */
package pt.up.fe.dceg.neptus.plugins.actions;

import java.awt.event.ActionEvent;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Exec", icon = "pt/up/fe/dceg/neptus/plugins/actions/full_toggle.png")
public class ExecuteCommandPanel extends SimpleMenuAction {
    private static final long serialVersionUID = 1L;
    @NeptusProperty(name = "Command to execute")
    public String command = "gedit&";

    /**
     * @param console
     */
    public ExecuteCommandPanel(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Runtime.getRuntime().exec(command);
        }
        catch (Exception ex) {
            GuiUtils.showErrorPopup("Error executing command", ex.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
