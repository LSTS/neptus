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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Feb 25, 2010
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.NeptusBlob;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.ConsoleParse;

/**
 * @author zp
 * 
 */
@Popup(name = "Quick LogBook Panel", accelerator = KeyEvent.VK_Q, width = 600, height = 400, pos = POSITION.BOTTOM_RIGHT, icon = "pt/lsts/neptus/plugins/plugins/logs/log.png")
@PluginDescription(author = "zp", name = "Quick Logbook Panel")
public class QuickLogger extends ConsolePanel {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Disseminate to network", description = "Merge log entries from this log onto other console's")
    public boolean disseminateLog = true;

    private LogBookPanel lbPanel;

    @Subscribe
    public void on(NeptusBlob msg) {
        if (msg.getContentType().equals("text/log")) {
            JsonArray arr = Json.parse(new String(msg.getContent())).asArray();
            lbPanel.merge(arr);
        }
    }

    @Periodic(millisBetweenUpdates = 30_000)
    public void disseminate() {
        JsonArray log = lbPanel.toJson();
        NeptusBlob blob = new NeptusBlob("text/log", log.toString().getBytes());
        ImcMsgManager.getManager().broadcastToCCUs(blob);
    }

    public QuickLogger(ConsoleLayout console) {
        super(console);
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(QuickLogger.class);
    }

    @Override
    public void initSubPanel() {
        removeAll();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        setLayout(new BorderLayout());
        lbPanel = new LogBookPanel(new File("log/logbook_" + sdf.format(new Date()) + ".html"));
        add(lbPanel, BorderLayout.CENTER);
        doLayout();
        invalidate();
        revalidate();
    }

    @Override
    public void cleanSubPanel() {

    }
}
