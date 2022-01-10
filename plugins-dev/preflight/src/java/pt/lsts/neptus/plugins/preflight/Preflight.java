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
 * Author: tsmarques
 * 13 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.preflight.utils.PreflightBuilder;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Preflight", author = "tsmarques", version = "0.1")
@Popup(name = "Preflight", pos = POSITION.CENTER, width = 550, height = 500)
public class Preflight extends ConsolePanel {
    public static final int WIDTH = 550;
    public static final int HEIGHT = 500;
    public static final int MAX_COMPONENT_WIDTH = WIDTH - 20; /* Maximum child component width */
    
    public static ConsoleLayout CONSOLE;
    
    
    private PreflightPanel contentPanel; /* main panel */
    private JScrollPane scrollMainPanel;
    
    private String mainSysName; /* Gets changed when main vehicle changes */
    
    private PreflightBuilder builder;
   
    public Preflight(ConsoleLayout console) {
        super(console);
        CONSOLE = getConsole();
        setResizable(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        
        builder = new PreflightBuilder();
        initMainPanel();
    }
    
    private void initMainPanel() {
        initContentPanel();
        scrollMainPanel = new JScrollPane(contentPanel);
        
        final Dimension d = new Dimension(MAX_COMPONENT_WIDTH, HEIGHT);
        scrollMainPanel.setMaximumSize(d);
        scrollMainPanel.setMinimumSize(d);
        scrollMainPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollMainPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMainPanel.setBorder(BorderFactory.createEmptyBorder());
        add(scrollMainPanel);
    }
    
    private void initContentPanel() {
        mainSysName = CONSOLE.getMainSystem();
        
        contentPanel = builder.buildPanel(mainSysName);
        contentPanel.setSysName(mainSysName);
    }
       
    @Subscribe
    public void on(ConsoleEventMainSystemChange ev) { /* When a diff vehicle has been selected as main Vehicle */
        mainSysName = CONSOLE.getMainSystem();
        switchPreflightPanel(mainSysName);
    }
    
    private void switchPreflightPanel(String systemId) {
        cleanUp();
        scrollMainPanel.remove(contentPanel);
        
        contentPanel = builder.buildPanel(mainSysName);
        contentPanel.setSysName(systemId);
        scrollMainPanel.setViewportView(contentPanel);
        
        scrollMainPanel.repaint();
    }
    
    @Override
    public void cleanSubPanel() {
        cleanUp();
    }
    
    private void cleanUp() {
        for(PreflightCheck check : contentPanel.getPanelChecks()) {
            if(check.isPeriodic())
                check.stopPeriodicUpdates();
            if(check.isRegistered())
                check.unregister();
        }
    }

    @Override
    public void initSubPanel() {

    }
}
