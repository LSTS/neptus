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
 * Apr 22, 2018
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;

import javax.swing.JDialog;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.envdisp.gui.LayersListPanel;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "netCDF Data Visualization", icon = "pt/lsts/neptus/plugins/envdisp/netcdf-radar.png")
public class NetCDFDataVisualization extends ConsoleLayer implements ConfigurationListener {
    
    private static final String CATEGORY_TEST = "Test";
    private static final String CATEGORY_DATA_UPDATE = "Data Update";

    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    private int dateLimitHours = 30;
    @NeptusProperty(name = "Use data x hour in the future (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    private int dateHoursToUseForData = 1;
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    private boolean ignoreDateLimitToLoad = true;

    @NeptusProperty(name = "Show visible data date-time interval", userLevel = LEVEL.ADVANCED, category = CATEGORY_TEST, 
            description = "Draws the string with visible curents data date-time interval.")
    private boolean showDataDebugLegend = false;

    private LayersListPanel layerList;
    private JDialog dialog = null;
    
    public NetCDFDataVisualization() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        layerList = new LayersListPanel(getConsole());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        if (dialog != null)
            dialog.dispose();
    }

    @NeptusMenuItem("Tools > netCDF Data Visualization Config")
    public void configMenuAction() {
        if (dialog == null) {
            dialog = new JDialog(getConsole());
            dialog.setModalityType(ModalityType.MODELESS);
            dialog.setLayout(new BorderLayout());
            dialog.add(layerList);
            dialog.setSize(layerList.getPreferredSize());
        }
        dialog.setVisible(true);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (GenericNetCDFDataPainter l : layerList.getVarLayersList()) {
            if (!l.isShowVar())
                continue;
            
            Graphics2D g2 = (Graphics2D) g.create();
            l.paint(g2, renderer, ignoreDateLimitToLoad, dateLimitHours, showDataDebugLegend);
            g2.dispose();
        }
    }
}
