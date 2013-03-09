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
 * Jun 28, 2011
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.util.Vector;

import javax.swing.JComponent;

import pt.up.fe.dceg.neptus.imc.LogBookEntry;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.logs.HistoryMessage.msg_type;

/**
 * @author zp
 *
 */
@PluginDescription(author="zp", name="Log Book", icon="pt/up/fe/dceg/neptus/plugins/logs/log.png")
public class LogBookVisualization extends SimpleMRAVisualization {

    /**
     * @param panel
     */
    public LogBookVisualization(MRAPanel panel) {
        super(panel);
    }

    private static final long serialVersionUID = 1L;

    protected HistoryPanel hpanel = null;

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("LogBookEntry") != null;
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {

        if (hpanel != null)
            return hpanel;
        
        hpanel = new HistoryPanel();
        Vector<HistoryMessage> messages = new Vector<HistoryMessage>();
        
        for (LogBookEntry entry : source.getLsfIndex().getIterator(LogBookEntry.class)) {
            HistoryMessage msg = new HistoryMessage();
            msg.timestamp = entry.getTimestampMillis();
            msg.text = entry.getText();
            msg.context = entry.getContext();
            LogBookEntry.TYPE type = entry.getType();

            switch (type) {
                case INFO:
                    msg.type = msg_type.info;
                    break;
                case WARNING:
                    msg.type = msg_type.warning;
                    break;
                case ERROR:
                    msg.type = msg_type.error;
                    break;
                case CRITICAL:
                    msg.type = msg_type.critical;
                    break;                    
                default:
                    break;

            }
            msg.assynchronous = false;
            messages.add(msg);            
        }
        hpanel.setMessages(messages);
        hpanel.remove(1);
        return hpanel;
    }
    
    public Type getType() {
        return Type.VISUALIZATION;
    }
}
