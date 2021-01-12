/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 29/08/2016
 */
package org.necsave.sink;

import java.awt.BorderLayout;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.necsave.MeshStateWrapper;

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
@PluginDescription(name = "Necsave MeshState Panel")
@Popup(name = "Necsave MeshState Panel", pos = POSITION.BOTTOM_RIGHT, height = 230, width = 400)
public class MeshStatePanel extends ConsolePanel {

    private static final long serialVersionUID = 7428286049668789699L;
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    private JEditorPane editor = new JEditorPane("text/html", "");
    private LinkedHashMap<Integer, MeshState> states = new LinkedHashMap<>();

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
        editor.setEditable(false);
        add(new JScrollPane(editor), BorderLayout.CENTER);
    }

    @Subscribe
    public void on(PlatformInfo state) {
        platformNames.put(state.getPlatformId(), state.getPlatformName());
    }

    @Subscribe
    public void on(MeshState state) {
        try {
        states.put(state.getSrc(), state);

        updateHtml();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void updateHtml() {
        ArrayList<Integer> platforms = new ArrayList<>();
        platforms.addAll(states.keySet());
        Collections.sort(platforms);
        StringWriter html = new StringWriter();

        
        for (int p : platforms) {
            if (states.containsKey(p)) {
                MeshState state = states.get(p);
                String name = nameOf(p);
                html.append("<strong>" + name + ":</strong><blockquote>");
                html.append(new MeshStateWrapper(state).toHtml(platformNames));
                html.append("</blockquote>\n");
                html.append("<hr/>\n");
            }
        }
        editor.setText(html.toString());
    }

    private String nameOf(int platform) {
        if (platformNames.containsKey(platform)) {
            return platformNames.get(platform);
        }

        return "Platform " + platform;
    }

}
