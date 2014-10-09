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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * 5 de Jul de 2010
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LogBookControl;
import pt.lsts.imc.LogBookControl.COMMAND;
import pt.lsts.imc.LogBookEntry;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
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
public class RemoteHistory extends ConsolePanel implements NeptusMessageListener, MainVehicleChangeListener {

    // @NeptusProperty(name = "Show popup messages when receiveing events")
    // private final boolean showPopups = true;

    private final JMenuItem menuItem;
    private final JDialog dialog;

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
            dialog.setVisible(!dialog.isVisible());
        }
    };

    public RemoteHistory(ConsoleLayout console) {
        super(console);
        removeAll();
        // TODO change this to the annotation popup but for that needs to refactor the plugin
        dialog = new JDialog(console);
        dialog.setTitle(I18n.text("Remote History"));
        dialog.setIconImage(ImageUtils.getImage(PluginUtils.getPluginIcon(RemoteHistory.class)));
        dialog.setSize(700, 300);
        dialog.setLocationRelativeTo(null);
        dialog.add(p);
        JRootPane rootPane = dialog.getRootPane();
        InputMap globalInputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        globalInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK, true), "pressed");
        rootPane.getActionMap().put("pressed", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        setLayout(new BorderLayout());
        ToolbarButton btn = new ToolbarButton(viewEvents);
        add(btn, BorderLayout.CENTER);

        menuItem = new JMenuItem(viewEvents);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK, true));
    }

    @Override
    public void initSubPanel() {
        p.console = getConsole();
        JMenu menu = getConsole().getOrCreateJMenu(new String[] { I18n.text("View") });
        menu.add(menuItem);
    };

    @Override
    public void cleanSubPanel() {
        if (dialog.isVisible()) {
            dialog.setVisible(false);            
        }
        
        if (menuItem != null) {
            JMenu menu = getConsole().getOrCreateJMenu(new String[] { I18n.text("View") });
            menu.remove(menuItem);
        }
    }

    protected void clearHistory() {
        p.myMessages.clear();
        p.mainPanel.removeAll();
        history.clear();
        viewEvents.putValue(AbstractAction.SMALL_ICON,
                ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue.png"));
        this.invalidate();
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "LogBookControl", "LogBookEntry" };
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange e) {
        clearHistory();
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

    @Override
    public void messageArrived(IMCMessage message) {

        if (message.getMgid() == LogBookEntry.ID_STATIC) {
            HistoryMessage msg = getMessage(message);
            msg.assynchronous = true;

            Vector<HistoryMessage> messages = new Vector<HistoryMessage>();

            messages.add(msg);
            p.setMessages(messages);
            return;
        }

        else if (message.getMgid() == LogBookControl.ID_STATIC) {
            if (message.getAsString("command").equals("STORE")) {
                IMCMessage inline = message.getMessage("msg");
                HistoryMessage msg = getMessage(inline);
                msg.assynchronous = true;

                history.add(msg);
                Vector<HistoryMessage> tmp = new Vector<HistoryMessage>();
                tmp.add(msg);
                p.setMessages(tmp);
                viewEvents.putValue(AbstractAction.SMALL_ICON,
                        ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue2.png"));
            }
            else {
                long lastTimeStamp = 0;
                IMCMessage pivot = message.getMessage("msg");
                while (pivot != null) {
                    IMCMessage hist = pivot.getMessage("msg");
                    HistoryMessage msg = getMessage(hist);
                    msg.assynchronous = false;
                    history.add(msg);
                    lastTimeStamp = msg.timestamp;
                    pivot = pivot.getMessage("next");
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
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        ConsoleLayout layout = ConsoleParse.testSubPanel(RemoteHistory.class);
        layout.setMainSystem("lauv-seacon-2");
    }
}
