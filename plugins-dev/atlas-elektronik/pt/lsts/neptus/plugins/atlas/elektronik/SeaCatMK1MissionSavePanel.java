/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * 02/10/2017
 */
package pt.lsts.neptus.plugins.atlas.elektronik;

import javax.swing.JButton;

import com.l2fprod.common.swing.JButtonBar;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.actions.ConsoleAction;
import pt.lsts.neptus.console.actions.CreateMissionConsoleAction;
import pt.lsts.neptus.console.actions.OpenMissionConsoleAction;
import pt.lsts.neptus.console.actions.SaveMissionAsConsoleAction;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "SeaCat-MK1 Mission Save Panel", description = "Allows the save and new mission buttons.")
public class SeaCatMK1MissionSavePanel extends ConsolePanel {
    
    /**
     * @param console
     */
    public SeaCatMK1MissionSavePanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
     * @param console
     * @param usedInsideAnotherConsolePanel
     */
    public SeaCatMK1MissionSavePanel(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);
        initialize();
    }
    
    private void initialize() {
        removeAll();
        
        ConsoleAction createMission = new CreateMissionConsoleAction(getConsole());
        ConsoleAction openMission = new OpenMissionConsoleAction(getConsole());
        ConsoleAction saveMissionAs = new SaveMissionAsConsoleAction(getConsole());
        
        JButtonBar buttonBar = new JButtonBar(JButtonBar.HORIZONTAL);
        buttonBar.add(new JButton(createMission));
        buttonBar.add(new JButton(openMission));
        buttonBar.add(new JButton(saveMissionAs));
        add(buttonBar);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
}
