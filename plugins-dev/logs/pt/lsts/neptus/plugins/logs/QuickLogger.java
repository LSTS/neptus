/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.ConsoleScript;
import pt.lsts.neptus.console.plugins.LogBookPanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.conf.StringProperty;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "zp", name = "Quick Logbook Panel")
public class QuickLogger extends ConsolePanel implements ConfigurationListener {
    private ConsoleScript conScript = new ConsoleScript();

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Log Shortcuts", description = "Enter the shortcuts as <Text to display>,<Text to log>(,<icon filename>)?")
    public StringProperty shortcuts = new StringProperty(
            "Mission Start,Mission Started\nScript Example,\"VehicleStatePos.ref=\"+$(VehicleStatePos.ref)");

    @NeptusProperty(name = "Number of Rows", description = "Number of rows in the panel. 0 for infinite.")
    public int numRows = 0;

    @NeptusProperty(name = "Number of Columns", description = "Number of columns in the panel. 0 for infinite.")
    public int numCols = 1;

    protected IMCMessage logMsg = IMCDefinition.getInstance().create("LogBookEntry");

    public QuickLogger(ConsoleLayout console) {
        super(console);
        // logMsg.setValue("op", System.getProperty("user.name"));
        logMsg.setValue("context", System.getProperty("user.name") + " quick log");
        logMsg.setValue("type", 0);
        propertiesChanged();
    }

    @Override
    public void propertiesChanged() {
        removeAll();
        setLayout(new GridLayout(numRows, numCols));
        String[] lines = shortcuts.toString().split("\n");
        for (String s : lines) {
            String[] parts = s.split(",");
            final String logText = parts[1].trim();

            JButton btn = new JButton(parts[0].trim());
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    String txt;

                    try {
                        conScript.setScript(logText);
                        txt = conScript.evaluate(getState()).toString();
                    }
                    catch (Exception ex) {
                        txt = logText;
                        ex.printStackTrace();
                    }

                    logMsg.setValue("htime", DateTimeUtil.timeStampSeconds());
                    logMsg.setValue("text", txt);
                    try {
                        LsfMessageLogger.log(logMsg);
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(ex);
                    }
                    LogBookPanel.logPlain(txt);
                }
            });
            add(btn);
        }
        // doLayout();
        invalidate();
        revalidate();
        validate();
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(QuickLogger.class);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
