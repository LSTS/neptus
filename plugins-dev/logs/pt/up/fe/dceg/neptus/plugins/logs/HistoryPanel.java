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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author ZP
 *
 */
@SuppressWarnings("serial")
public class HistoryPanel extends JPanel {

    protected JPanel mainPanel = new JPanel();
    protected Vector<HistoryMessage> myMessages = new Vector<HistoryMessage>();
    protected JScrollPane scroll = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    protected ConsoleLayout console = null;
    protected boolean showInfo = true;
    protected boolean showReload = true;

    protected LinkedHashMap<msg_type, Color> bgColors = new LinkedHashMap<HistoryMessage.msg_type, Color>();
    {
        bgColors.put(msg_type.critical, Color.black);
        bgColors.put(msg_type.error, new Color(255,128,128));
        bgColors.put(msg_type.warning, new Color(255,255,128));
        bgColors.put(msg_type.info, new Color(200,255,200));        
    }

    public HistoryPanel(ConsoleLayout console, boolean showReload) {
        this.console = console;
        this.showReload = showReload;
        
        setLayout(new BorderLayout());
        mainPanel.setLayout(new GridLayout(0,1));
        mainPanel.setBackground(Color.white);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        if(showReload) {
            ToolbarButton btn = new ToolbarButton(new AbstractAction(I18n.text("Reload"),
                    ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/reload.png")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (HistoryPanel.this.console == null)
                        return;
                    IMCMessage m = IMCDefinition.getInstance().create("LogBookControl", "command", "GET");
                    ImcMsgManager.getManager().sendMessageToVehicle(m, HistoryPanel.this.console.getMainSystem(),
                            null);

                    m = IMCDefinition.getInstance().create("LogBookControl", "command", "GET_ERR");
                    ImcMsgManager.getManager().sendMessageToVehicle(m, HistoryPanel.this.console.getMainSystem(),
                            null);
                }
            });
            btn.setText(I18n.text("Reload"));

            bottom.add(btn);    
        }
        
        ToolbarSwitch sw =  new ToolbarSwitch(new AbstractAction(I18n.text("Show all"), ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/info.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (HistoryPanel.this.console == null)
                    return;
                
                showInfo = ((ToolbarSwitch)e.getSource()).isSelected();
                
                Vector<HistoryMessage> tmp = new Vector<HistoryMessage>();
                tmp.addAll(myMessages);
                myMessages.clear();
                mainPanel.removeAll();
                setMessages(tmp);
            }
        });
        sw.setText(I18n.text("Show info"));
        bottom.add(sw);
        
        //if (HistoryPanel.this.console != null)
            add(bottom, BorderLayout.SOUTH);
    }

    public HistoryPanel() {
        this(null,true);
    }

    public void setMessages(Vector<HistoryMessage> messages) {
        for (HistoryMessage m : messages) {
            if (!myMessages.contains(m)) {
                myMessages.add(m);
                
                if (m.type == msg_type.info && !showInfo)
                    continue;
                
                JLabel l = new JLabel(m.toString(), getIcon(m.type), JLabel.LEFT);
                l.setToolTipText(I18n.textf("Received on %timeStamp (%context)", new Date(m.timestamp), m.context));
                l.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 3));
                l.setOpaque(true);
                l.setBackground(bgColors.get(m.type));
                if (m.type == msg_type.critical)
                    l.setForeground(Color.yellow);
                mainPanel.add(l);                
            }
        }
        
        invalidate();
        validate();
        mainPanel.scrollRectToVisible(new Rectangle(0, mainPanel.getHeight()+22, 1, 1) );
        repaint();
    }
    
    public ImageIcon getIcon(msg_type type) {
        switch (type) {
            case info:
                return ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/info.png");
            case warning:
                return ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/warning.png");
            case error:
            case critical:
                return ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/error.png");
            default:
                return ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/logs/queue2.png");
        }
    }
}
