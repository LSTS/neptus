/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 \* Alternatively, this file may be used under the terms of the Modified EUPL,
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
 * Author: jfortuna
 * Jun 20, 2014
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.RemoteActions;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;

/**
 * Shows buttons to do various calibrations for Piksi. 
 *
 * @author krisklau
 *
 */
@PluginDescription(name = "Piksi Panel", author = "krisklau", version = "0.1", category = CATEGORY.INTERFACE)
public class PiksiPanel extends ConsolePanel implements MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    // GUI
    private JPanel titlePanel = null;
    private JPanel buttonPanel = null;
  
    /**
     * @param console
     */
    public PiksiPanel(ConsoleLayout console) {
        super(console);
        
        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    @Override
    public void initSubPanel() {
        titlePanelSetup();
        buttonPanelSetup();
        
        // panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(titlePanel, "w 100%, h 30%, wrap");
        this.add(buttonPanel, "w 100%, h 70%, wrap");
    }

    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        JLabel titleLabel = new JLabel(I18n.text("Piksi Interface"), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9));
        titlePanel.add(titleLabel, "w 100%, h 100%");
    }

    private void buttonPanelSetup() {
        buttonPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        // Calibrate
        JButton initBaselineButton = new JButton(I18n.text("Init Baseline"));
        initBaselineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteActions action = new RemoteActions();
                action.setActions("piksiInitZeroBaseline=1");
                send(action);
            }
        });
        buttonPanel.add(initBaselineButton, "w 34%, h 100%");

        // Arm
        JButton resetFilterButton = new JButton(I18n.text("Reset Filter"));
        resetFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteActions action = new RemoteActions();
                action.setActions("piksiResetFilters=1");
                send(action);
            }
        });
        buttonPanel.add(resetFilterButton, "w 33%, h 100%");

        // Disarm
        JButton resetIARButton = new JButton(I18n.text("Reset IAR"));
        resetIARButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteActions action = new RemoteActions();
                action.setActions("piksiResetIARs=1"); 
                send(action);
            }
        });
        buttonPanel.add(resetIARButton, "w 33%, h 100%");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
}
