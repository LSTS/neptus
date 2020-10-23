/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * Mar 27, 2019
 */
package pt.lsts.neptus.plugins.dev;

import java.awt.BorderLayout;

import pt.lsts.neptus.comm.manager.imc.ImcMessageSenderPanel;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.MonitorIMCComms;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "IMC Comms Monitor",
    icon = "images/imc.png",
    description = "Monitors IMC comms manager.")
@Popup(pos = POSITION.TOP_LEFT, width = 616, height = 631)
@SuppressWarnings("serial")
public class ImcCommsMonitorPanel extends ConsolePanel {

    private MonitorIMCComms panel;

    /**
     * @param console
     */
    public ImcCommsMonitorPanel(ConsoleLayout console) {
        super(console);
    }

    /**
     * @param console
     * @param usedInsideAnotherConsolePanel
     */
    public ImcCommsMonitorPanel(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        this.removeAll();
        this.setLayout(new BorderLayout());
        
        panel = new MonitorIMCComms(getConsole().getImcMsgManager());
        this.add(panel);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
}
