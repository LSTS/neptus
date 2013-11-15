/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.comm.manager.imc.ImcMsgManager;

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
                    ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/reload.png")) {

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
        
        ToolbarSwitch sw =  new ToolbarSwitch(new AbstractAction(I18n.text("Show all"), ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/info.png")) {

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
                return ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/info.png");
            case warning:
                return ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/warning.png");
            case error:
            case critical:
                return ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/error.png");
            default:
                return ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue2.png");
        }
    }
}
