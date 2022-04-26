/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jun 27, 2014
 */
package pt.lsts.neptus.plugins.europa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.europa.gui.SolverPanel;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription
public class EuropaPlugin extends ConsolePanel {

    private static final long serialVersionUID = -9214738581059126395L;
    private SolverPanel solverPanel = null;
    private JFrame solverFrame = null;
    /**
     * @param console
     */
    public EuropaPlugin(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        if (solverPanel != null)
            solverPanel.reset();
    }

    @Override
    public void initSubPanel() {
        addMenuItem("Tools>Mission Planner", null, new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                generateMission();
            }
        });
    }
    
    private void generateMission() {
        if (solverPanel == null) {
            solverPanel = new SolverPanel(getConsole());
        }
        if (solverFrame == null) {
            solverFrame = new JFrame("Mission Planner");    
            solverFrame.setContentPane(solverPanel);
            solverFrame.setIconImage(ImageUtils.getImage(PluginUtils.getPluginIcon(getClass())));
            solverFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            solverFrame.pack();
            GuiUtils.centerOnScreen(solverFrame);
        }
        solverFrame.setVisible(true);
        solverFrame.toFront();
    }
}
