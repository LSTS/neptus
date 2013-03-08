/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Apr 26, 2012
 * $Id:: EventsPanel.java 10008 2013-02-21 11:47:51Z zepinto                    $:
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.logs.HistoryMessage.msg_type;

/**
 * @author jqcorreia
 *
 */
public class EventsPanel extends HistoryPanel {
    
    private static final long serialVersionUID = 1L;

    public EventsPanel(ConsoleLayout console) {
        super(console,false); // False meaning no reload capability
    }
    
    public void addMessage(HistoryMessage hmessage) {
        myMessages.add(hmessage);
        setMessages(myMessages);
    }
    
    public void setMessages(Vector<HistoryMessage> messages) {
        mainPanel.removeAll();
        for (HistoryMessage m : messages) {
            if (m.type == msg_type.info && !showInfo)
                continue;

            JLabel l = new JLabel(m.toString(),
                    getIcon(m.type), JLabel.LEFT);
            l.setToolTipText("Received on " + new Date(m.timestamp) + " (" + m.context + ")");
            l.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 3));
            l.setOpaque(true);
            l.setBackground(bgColors.get(m.type));
            if (m.type == msg_type.critical)
                l.setForeground(Color.yellow);
            mainPanel.add(l);
        }
        invalidate();
        validate();
        mainPanel.scrollRectToVisible(new Rectangle(0, mainPanel.getHeight() + 22, 1, 1));
        repaint();
    }
}
