/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsm 
 * 2016/12/05
 */
package pt.lsts.neptus.plugins.multibeam.viewers;

import java.awt.Dimension;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.multibeam.console.MultibeamRealTimeWaterfall;

/**
 * Created by tsm on 05/12/16.
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Tiago Marques", version = "0.1", name = "Multibeam: Dual Viewer", description = "Displays multibeam waterfall and cross-section viewers")
@Popup(pos = Popup.POSITION.TOP_LEFT, width = 1400, height = 800)
public class MultibeamDualViewer extends ConsolePanel {
    // GUI
    private JPanel viewersPanel;

    // data viewers
    private MultibeamCrossSection crossSection;
    private MultibeamRealTimeWaterfall waterfall;

    public MultibeamDualViewer(ConsoleLayout console) {
        super(console);
        crossSection = new MultibeamCrossSection(console, true);
        waterfall = new MultibeamRealTimeWaterfall(console, true);

        viewersPanel = new JPanel();
        viewersPanel.setLayout(new MigLayout());
        viewersPanel.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));
        viewersPanel.setLayout(new MigLayout());

        viewersPanel.add(waterfall, "w 30%, h 100%");
        viewersPanel.add(crossSection, "w 70%, h 100%");
        this.setLayout(new MigLayout("ins 0, gap 0", "[][]"));
        this.add(viewersPanel, "w 100%, h 100%,  grow");
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#init()
     */
    @Override
    public void init() { // Needed overwritten in order to proper initialize the "sub" ConsolePanels
        crossSection.init();
        waterfall.init();
        super.init();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#clean()
     */
    @Override
    public void clean() { // Needed overwritten in order to proper clean the "sub" ConsolePanels
        super.clean();
        crossSection.clean();
        waterfall.clean();
    }
}
