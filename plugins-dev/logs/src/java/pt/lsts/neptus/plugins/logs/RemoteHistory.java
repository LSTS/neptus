/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 5 de Jul de 2010
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LogBookControl;
import pt.lsts.imc.LogBookControl.COMMAND;
import pt.lsts.imc.LogBookEntry;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Remote History", icon = "pt/lsts/neptus/plugins/logs/queue.png", documentation = "entity-state/remotehistory.html")
@Popup(accelerator=KeyEvent.VK_R, width=700, height=500, name="Remote History", icon="pt/lsts/neptus/plugins/logs/queue.png", pos=POSITION.CENTER)
public class RemoteHistory extends ConsolePanel implements MainVehicleChangeListener {

    protected HistoryPanel p = new HistoryPanel(null, true);
    protected boolean hasNewMessages = false;
    protected Vector<HistoryMessage> history = new Vector<HistoryMessage>();

    protected AbstractAction viewEvents = new AbstractAction(I18n.text(this.getName()), ImageUtils.getScaledIcon(this.getImageIcon(), 16, 16)) {
        @Override
        public void actionPerformed(ActionEvent e) {

            if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                clearHistory();
            }

            LogBookControl getmsgs = new LogBookControl();
            getmsgs.setCommand(COMMAND.GET);
            send(getmsgs);

            LogBookControl geterr = new LogBookControl();
            geterr.setCommand(COMMAND.GET_ERR);
            send(geterr);

            p.setMessages(history);
        }
    };

    public RemoteHistory(ConsoleLayout console) {
        super(console);
        p.console = getConsole();
    }

    @Override
    public void initSubPanel() {
        setLayout(new BorderLayout());
        add(p, BorderLayout.CENTER);
    };

    @Override
    public void cleanSubPanel() {
    }

    protected void clearHistory() {
        p.myMessages.clear();
        p.mainPanel.removeAll();
        history.clear();
        viewEvents.putValue(AbstractAction.SMALL_ICON,
                ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue.png"));
        this.invalidate();
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange e) {
        clearHistory();
        p.revalidate();
        p.reloadMessages();        
    }
    
    protected HistoryMessage getMessage(IMCMessage logBookEntry) {
        HistoryMessage msg = new HistoryMessage();
        msg.timestamp = (logBookEntry.getTimestampMillis());
        msg.text = logBookEntry.getString("text");
        msg.context = logBookEntry.getString("context");
        String type = logBookEntry.getString("type");

        if (type.equals("INFO"))
            msg.type = msg_type.info;
        else if (type.equals("WARNING"))
            msg.type = msg_type.warning;
        else if (type.equals("ERROR"))
            msg.type = msg_type.error;
        else if (type.equals("CRITICAL"))
            msg.type = msg_type.critical;
        else if (type.equals("DEBUG"))
            msg.type = msg_type.debug;

        return msg;
    }

    @Subscribe
    public void on(LogBookEntry event) {
        HistoryMessage msg = getMessage(event);
        msg.assynchronous = true;

        Vector<HistoryMessage> messages = new Vector<HistoryMessage>();

        messages.add(msg);
        p.setMessages(messages);
    }

    @Subscribe
    public void on(LogBookControl msg) {
        if (msg.getCommand() == LogBookControl.COMMAND.REPLY)
        {

            long lastTimeStamp = 0;
            for (LogBookEntry entry : msg.getMsg()) {
                HistoryMessage m = getMessage(entry);
                m.assynchronous = false;
                history.add(m);
                lastTimeStamp = m.timestamp;            
            }

            Vector<HistoryMessage> toRemove = new Vector<HistoryMessage>();

            for (HistoryMessage m : history) {
                if (m.assynchronous && m.timestamp < lastTimeStamp)
                    toRemove.add(m);
            }
            history.removeAll(toRemove);
            p.setMessages(history);
            viewEvents.putValue(AbstractAction.SMALL_ICON,
                    ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue.png"));
        }
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        ConsoleLayout layout = ConsoleParse.testSubPanel(RemoteHistory.class);
        layout.setMainSystem("lauv-seacon-2");
    }
}
