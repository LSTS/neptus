package pt.lsts.neptus.hyperspectral;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MenuBar;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 13 May 2015
 */

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "HyperSpectral Data Viewer", author = "tsmarques", version = "0.1")
@Popup(name = "HyperSpectral Data Viewer", pos = POSITION.CENTER, width = 1000, height = 700)
public class HyperspectralViewer extends ConsolePanel {
    private RealtimeViewer realtimePanel;
    private JPanel snapshotsPanel;
    
    private JMenuBar menuBar;
    private JMenu viewMenu;
    private JRadioButtonMenuItem viewRealtimePanel;
    private JRadioButtonMenuItem viewSnapshotsPanel;
    private ButtonGroup buttonsGroup;
    
    
    public HyperspectralViewer(ConsoleLayout console) {
        super(console);   
        setLayout(new BorderLayout());
        setupMenu();
        realtimePanel = new RealtimeViewer(console);
        
        add(realtimePanel);
        add(menuBar, BorderLayout.NORTH);
    }

    private void setupMenu() {
        menuBar = new JMenuBar();
        viewMenu = new JMenu("View");
        viewRealtimePanel = new JRadioButtonMenuItem("Real-Time data");
        viewSnapshotsPanel = new JRadioButtonMenuItem("Data snapshots");
        buttonsGroup = new ButtonGroup();
        
        buttonsGroup.add(viewRealtimePanel);
        buttonsGroup.add(viewSnapshotsPanel);
        
        viewRealtimePanel.setSelected(true);
        
        viewMenu.add(viewRealtimePanel);
        viewMenu.add(viewSnapshotsPanel);
        menuBar.add(viewMenu);
    }

    @Override
    public void cleanSubPanel() {}

    @Override
    public void initSubPanel() {}

}
