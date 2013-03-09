/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2009/06/07
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BorderLayout;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ChronometerPanel;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@Popup(pos = POSITION.BOTTOM_LEFT, width = 300, height = 150, accelerator = 'K')
@PluginDescription(name = "Chronometer", description = "This is a chronometer that counts up and down", author = "Paulo Dias", icon = "images/buttons/clocksync2.png", documentation = "chronometer/chronometer.html")
public class ChronometerPlugin extends SimpleSubPanel {

    private ChronometerPanel chronoPanel = null;

    public ChronometerPlugin(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
	 * 
	 */
    private void initialize() {
        this.removeAll();
        this.setLayout(new BorderLayout());
        this.add(getChronoPanel(), BorderLayout.CENTER);
    }

    /**
     * @return the chronoPanel
     */
    public ChronometerPanel getChronoPanel() {
        if (chronoPanel == null) {
            chronoPanel = new ChronometerPanel();
        }
        return chronoPanel;
    }

    @Override
    public void initSubPanel() {
        // nothing
    }

    @Override
    public void cleanSubPanel() {
    }

}
