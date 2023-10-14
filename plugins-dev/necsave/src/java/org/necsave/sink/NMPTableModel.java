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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 09/08/2016
 */
package org.necsave.sink;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import javax.swing.table.AbstractTableModel;

import info.necsave.msgs.PlatformInfo;
import info.necsave.proto.Message;

/**
 * @author zp
 *
 */
public class NMPTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 2866593640476649020L;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    
    private ArrayList<Message> messages = new ArrayList<>();
    private ArrayList<Long> timeReceived = new ArrayList<>();
    private ArrayList<String> columns = new ArrayList<>();
    long startId = 0;
    {
        columns.addAll(Arrays.asList("ID", "Time", "Medium", "Source", "Destination", "Type", "Size", "Latency (ms)"));
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        platformNames.put(65535, "broadcast");
    }
    
    @Override
    public String getColumnName(int column) {
        return columns.get(column);
    }
    
    public long getId(int rowIndex) {
        return startId+rowIndex;        
    }
    public Message getMessage(int rowIndex) {
        synchronized (messages) {
            return messages.get(rowIndex);
        }
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Long.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return String.class;
            case 4:
                return String.class;
            case 5:
                return String.class;
            case 6:
                return Integer.class;
            case 7:
                return Long.class;                
            default:
                return Object.class;
        }
    }
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
    
    
    public void addMessage(Message m) {
        if (m instanceof PlatformInfo) {
            PlatformInfo pi = (PlatformInfo) m;
            synchronized (platformNames) {
                platformNames.put(pi.getPlatformId(), pi.getPlatformName());      
            }        
        }
        synchronized (messages) {
            messages.add(m);       
            timeReceived.add(System.currentTimeMillis());
        }
        fireTableRowsInserted(messages.size()-2, messages.size()-1);
    }
    
    public void clear() {
        int size;
        synchronized (messages) {
            size = messages.size()-1;
            messages.clear();
            startId = size+1;       
            timeReceived.clear();
        }
        fireTableRowsDeleted(0, size);
    }
    
    @Override
    public int getRowCount() {
        synchronized (messages) {
            return messages.size();
        }
    }
    
    @Override
    public int getColumnCount() {
        return columns.size();
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (messages) {
            Message msg = messages.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return rowIndex+startId;
                case 1:
                    return sdf.format(new Date(msg.getTimestampMillis()));
                case 2:
                    return msg.getHeader().getString("medium");
                case 3:
                    synchronized (platformNames) {
                        if (platformNames.containsKey(msg.getSrc()))
                            return platformNames.get(msg.getSrc());
                        else
                            return ""+msg.getSrc();    
                    }
                case 4:
                    synchronized (platformNames) {
                        if (platformNames.containsKey(msg.getDst()))
                            return platformNames.get(msg.getDst());
                        else
                            return ""+msg.getDst();   
                    }
                case 5:
                    return msg.getAbbrev();
                case 6:
                    return msg.getSize();
                case 7:
                    return timeReceived.get(rowIndex) - msg.getTimestampMillis();                     
                default:
                    return "";
            }
        }
    }
}
