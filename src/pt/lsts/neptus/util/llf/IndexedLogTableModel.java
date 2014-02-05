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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Feb 5, 2014
 */
package pt.lsts.neptus.util.llf;

import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfGenericIterator;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 * 
 */
public class IndexedLogTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    // RandomAccessFile raf = null;
    private LinkedHashMap<Integer, Vector<Object>> cache = new LinkedHashMap<Integer, Vector<Object>>();

    //private LinkedHashMap<Integer, Integer> messageIndexes = new LinkedHashMap<>();
    protected int rowCount = 1;
    protected LsfIndex index;
    protected String msgType;
    protected IMraLogGroup source;
    protected long initTimeMillis = 0;
    protected long finalTimeMillis = 0;
    protected IMCMessageType imcMsgType;
    
    protected void load(double initTime, double finalTime) {
        LsfGenericIterator it = index.getIterator(msgType);
        IMCMessage msg;
        int rowIndex = 0;
        while((msg = it.next()) != null) {
            if (msg.getTimestamp() < initTime)
                continue;
            
            if (msg.getTimestamp() > finalTime)
                break;
            
            Vector<Object> values = new Vector<Object>();
            values.add(msg.getTimestampMillis());
            int src = msg.getInteger("src");
            int src_ent = msg.getInteger("src_ent");
            int dst = msg.getInteger("dst");
            int dst_ent = msg.getInteger("dst_ent");

            values.add(source.getSystemName(src));
            values.add(source.getEntityName(src, src_ent));
            values.add(source.getSystemName(dst));
            values.add(source.getEntityName(dst, dst_ent));
            
            for (String key : msg.getMessageType().getFieldNames())
                values.add(msg.getString(key));

            cache.put(rowIndex, values);
            rowIndex++;
        }
        rowCount = rowIndex;
    }

    public IndexedLogTableModel(IMraLogGroup source, String msgName) {
        this(source, msgName, (long) (source.getLsfIndex().getStartTime() * 1000), 
                (long) (source.getLsfIndex().getEndTime() * 1000));
    }

    public IndexedLogTableModel(IMraLogGroup source, String msgName, long initTime, long finalTime) {
        this.source = source;
        this.index = source.getLsfIndex();
        this.msgType = msgName;
        this.imcMsgType = index.getDefinitions().getType(msgName);
        
        try {
            load(initTime/1000.0, finalTime / 1000.0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getColumnCount() {
        if (msgType == null)
            return 1;

        return 5 + imcMsgType.getFieldNames().size();
    }

    public int getRowCount() {
        if (index == null)
            return 1;
        return rowCount;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (index == null) {
            return "Unable to load data";
        }
        if (cache.containsKey(rowIndex)) {
            return cache.get(rowIndex).get(columnIndex);
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        if (index == null)
            return "Error";

        Vector<String> names = new Vector<String>();
        names.add("time");
        names.add("src");
        names.add("src_ent");
        names.add("dst");
        names.add("dst_ent");
        names.addAll(imcMsgType.getFieldNames());
        return names.get(column);

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
