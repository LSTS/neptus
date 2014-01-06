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
 * Author: José Correia
 * Apr 26, 2012
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;

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
