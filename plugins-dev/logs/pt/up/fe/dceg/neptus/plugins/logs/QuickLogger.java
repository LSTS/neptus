/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 25, 2010
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.ConsoleScript;
import pt.up.fe.dceg.neptus.console.plugins.LogBookPanel;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.conf.StringProperty;
import pt.up.fe.dceg.neptus.util.llf.NeptusMessageLogger;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "zp", name = "Quick Logbook Panel")
public class QuickLogger extends SimpleSubPanel implements ConfigurationListener {
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
                        NeptusMessageLogger.logMessage(logMsg);
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
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
