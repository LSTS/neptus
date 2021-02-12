/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 19, 2013
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
public class LogBookHistory extends AbstractListModel<HistoryMessage> implements ListCellRenderer<HistoryMessage> {

    private static final long serialVersionUID = 2382030731540409061L;
    protected LinkedList<HistoryMessage> messages = new LinkedList<>();
    protected String sysname;
    protected int maxSize = 250;
    
    protected LinkedHashMap<msg_type, Color> bgColors = new LinkedHashMap<HistoryMessage.msg_type, Color>();
    {
        bgColors.put(msg_type.critical, Color.black);
        bgColors.put(msg_type.error, new Color(255, 128, 128));
        bgColors.put(msg_type.warning, new Color(255, 255, 128));
        bgColors.put(msg_type.info, new Color(200, 255, 200));
        bgColors.put(msg_type.debug, new Color(217,217,217));
    }

    public LogBookHistory(String sysname) {
        this.sysname = sysname;
    }

    public void add(HistoryMessage msg) {
        if (!messages.contains(msg)) {
            messages.add(msg);
            Collections.sort(messages);
            int idx = Collections.binarySearch(messages, msg);
            fireIntervalAdded(this, idx, idx);
        }
        if (getSize() > maxSize) {
            messages.removeFirst();
            fireIntervalRemoved(this, 0, 0);
        }
    }
    
    public void clear() {
        int size = getSize();
        messages.clear();
        fireIntervalRemoved(this, 0, size);
    }
    
    public Collection<HistoryMessage> add(Collection<HistoryMessage> msgs) {
        
        Vector<HistoryMessage> notExisting = new Vector<>();
        
        for (HistoryMessage msg : msgs) {
            if (!messages.contains(msg)) {
                messages.add(msg);
                notExisting.add(msg);
            }
        }
        
        if (notExisting.isEmpty())
            return notExisting;
        
        Collections.sort(messages);
        
        while (getSize() > maxSize) {
            messages.removeFirst();
        }
        
        fireContentsChanged(this, 0, getSize());
        Collections.sort(notExisting);
        return notExisting;
        
    }

    public long lastMessageTimestamp() {
        return messages.getLast().timestamp;
    }

    @Override
    public HistoryMessage getElementAt(int index) {
        return messages.get(index);
    }

    @Override
    public int getSize() {
        return messages.size();
    }

    JLabel l = new JLabel("", JLabel.LEFT);
    @Override
    public Component getListCellRendererComponent(JList<? extends HistoryMessage> list, HistoryMessage value,
            int index, boolean isSelected, boolean cellHasFocus) {

        l.setText(value.toString());
        
        //JLabel l = new JLabel(value.toString(), JLabel.LEFT);
        l.setToolTipText(I18n.textf("Received on %timeStamp (%context)", new Date(value.timestamp), value.context));
        l.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 3));
        l.setOpaque(true);
        l.setBackground(bgColors.get(value.type));
        if (value.type == msg_type.critical)
            l.setForeground(Color.yellow);

        return l;
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
	    case debug:
                return ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/unknown.png");
            default:
                return ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue2.png");
        }
    }

    public static void main(String[] args) throws Exception {
        LogBookHistory hist = new LogBookHistory("lauv");
        JList<HistoryMessage> panel = new JList<>(hist);
        panel.setCellRenderer(hist);
        GuiUtils.testFrame(new JScrollPane(panel));
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(30);
            
            hist.add(new HistoryMessage(System.currentTimeMillis(), "teste1", "ctx", true, HistoryMessage.msg_type.error));
            Thread.sleep(30);
            
            hist.add(new HistoryMessage(System.currentTimeMillis(), "teste2", "ctx", true, HistoryMessage.msg_type.info));
            Thread.sleep(30);
            hist.add(new HistoryMessage(System.currentTimeMillis()-5000, "teste3", "ctx", true, HistoryMessage.msg_type.critical));
            Thread.sleep(30);
            hist.add(new HistoryMessage(System.currentTimeMillis()-5000, "teste3", "ctx", true, HistoryMessage.msg_type.warning));
        }
    }

}
