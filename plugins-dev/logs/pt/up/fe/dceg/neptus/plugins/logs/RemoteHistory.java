/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 5 de Jul de 2010
 */
package pt.up.fe.dceg.neptus.plugins.logs;

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

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.LogBookControl;
import pt.up.fe.dceg.neptus.imc.LogBookControl.COMMAND;
import pt.up.fe.dceg.neptus.imc.LogBookEntry;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Remote History", icon = "pt/up/fe/dceg/neptus/plugins/logs/queue.png", documentation = "entity-state/remotehistory.html")
public class RemoteHistory extends SimpleSubPanel implements NeptusMessageListener, MainVehicleChangeListener {

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
        JMenu menu = console.getOrCreateJMenu(new String[] { I18n.text("View") });
        menu.add(menuItem);
    };

    @Override
    public void cleanSubPanel() {
        if (dialog.isVisible()) {
            dialog.setVisible(false);            
        }
        
        if (menuItem != null) {
            JMenu menu = console.getOrCreateJMenu(new String[] { I18n.text("View") });
            menu.remove(menuItem);
        }
    }

    protected void clearHistory() {
        p.myMessages.clear();
        p.mainPanel.removeAll();
        history.clear();
        viewEvents.putValue(AbstractAction.SMALL_ICON,
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/queue.png"));
        this.invalidate();
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "LogBookControl", "LogBookEntry" };
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
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
                        ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/queue2.png"));
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
                        ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/queue.png"));
            }
        }
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        ConsoleLayout layout = ConsoleParse.testSubPanel(RemoteHistory.class);
        layout.setMainSystem("lauv-seacon-2");
    }
}
