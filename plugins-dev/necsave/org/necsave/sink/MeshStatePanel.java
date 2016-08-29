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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 29/08/2016
 */
package org.necsave.sink;

import java.awt.BorderLayout;
import java.util.LinkedHashMap;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.google.common.eventbus.Subscribe;

import info.necsave.msgs.MeshState;
import info.necsave.msgs.PlatformInfo;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author zp
 *
 */
@PluginDescription(name="Necsave MeshState Panel")
@Popup(name = "Necsave MeshState Panel", pos = POSITION.CENTER, height = 500, width = 800)
public class MeshStatePanel extends ConsolePanel {

    private static final long serialVersionUID = 7428286049668789699L;
    private JTabbedPane tabs = new JTabbedPane();
    private LinkedHashMap<String, JEditorPane> states = new LinkedHashMap<>();
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    
    /**
     * @param console
     */
    public MeshStatePanel(ConsoleLayout console) {
        super(console);
    }
    
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initSubPanel() {
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }
    
    @Subscribe
    public void on(PlatformInfo state) {
        platformNames.put(state.getPlatformId(), state.getPlatformName());
    }
    
    @Subscribe
    public void on(MeshState state) {
       if (!platformNames.containsKey(state.getSrc()))
           return;
       
       String platform = platformNames.get(state.getSrc());
       
       if (!states.containsKey(platform)) {
           JEditorPane component = new JEditorPane("text/html", asHtml(state));
           component.setEditable(false);
           states.put(platform, component);
           tabs.add(platform, new JScrollPane(component));
       }
       else {
           states.get(platform).setText(asHtml(state));
       }
    }
    
    private String asHtml(MeshState state) {
        MeshStateWrapper mesh = new MeshStateWrapper(state);
        
        String html = "<html><table border='1' width='300px'>";
        
        for (int area = 0; area < mesh.numAreas; area++) {
            if (area %3 == 0)
                html +="<tr>";
            
            if (mesh.scannedAreas.contains(area))
                html += "<td bgcolor='#00AA00'>"+area+": DONE</td>";
            else if (!mesh.allocatedAreas.containsKey(area))
                html += "<td bgcolor='#EE00FF'>"+area+": N/A</td>";
            else
                html += "<td bgcolor='#CCEECC'>"+area+": "+mesh.allocatedAreas.get(area)+"</td>";
            
                
            if (area %3 == 2)
                html +="</tr>";
        }
        
        html += "</table>";
        
        return html;
    }

}
