/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2011
 */
package pt.lsts.neptus.plugins.logs;

import java.util.Vector;

import javax.swing.JComponent;

import pt.lsts.imc.LogBookEntry;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;

/**
 * @author zp
 *
 */
@PluginDescription(author="zp", name="Log Book", icon="pt/lsts/neptus/plugins/logs/log.png")
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
		case DEBUG:
                    msg.type = msg_type.debug;
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
