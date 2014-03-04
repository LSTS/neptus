/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Mar 4, 2014
 */
package pt.lsts.neptus.plugins.s57;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@PluginDescription
public class S57SoundingsExporter extends ConsolePanel {

    public S57SoundingsExporter(ConsoleLayout cl) {
        super(cl);
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initSubPanel() {
        addMenuItem("Tools>Export Depth soundings", ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())),
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        Vector<MapPanel> maps = getConsole().getSubPanelsOfClass(MapPanel.class);
                        if (maps.isEmpty()) {
                            GuiUtils.errorMessage(getConsole(), "Export soundings", "Cannot export soundings because there is no map in the console");
                            return;
                        }
                        
                        StateRenderer2D renderer = maps.firstElement().getRenderer();
                        //renderer.getWorldMapPainter().get
                        System.out.println("Export soundings!!");
                    }
                });
    }
}
