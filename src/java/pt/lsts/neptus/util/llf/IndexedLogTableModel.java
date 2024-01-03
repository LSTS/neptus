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
 * Feb 5, 2014
 */
package pt.lsts.neptus.util.llf;

import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.collections4.map.LRUMap;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 * 
 */
public class IndexedLogTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    // map from rows to message index in the log
    private final LinkedHashMap<Integer, Integer> rowToIndex = new LinkedHashMap<>();
    // cache of recently retrieved messages from the log (heavy operation)
    private final LRUMap cache = new LRUMap(100);
    private int rowCount = 1;
    private final LsfIndex index;
    private final IMCMessageType imcMsgType;
    private Vector<String> names = null;
    private Vector<String> msgNames = null;

    // This method returns the message that should go into the given table row
    public synchronized IMCMessage getMessage(int row) {
        if (!rowToIndex.containsKey(row))
            return null;

        int idx = rowToIndex.get(row);

        if (cache.containsKey(idx)) {
            return (IMCMessage) cache.get(idx);
        }
        else {            
            try {
                IMCMessage m = index.getMessage(idx);
                cache.put(idx, m);
                return m;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                return null;
            }
        }
    }

    private void loadIndexes(double initTime, double finalTime) {
        int rowIndex = 0;
        int mgid = imcMsgType.getId();
        int curIndex = index.getFirstMessageOfType(mgid);

        while (curIndex != -1) {
            double time = index.timeOf(curIndex);

            if (time > finalTime)
                break;
            else if (time >= initTime || initTime < 0) {
                rowToIndex.put(rowIndex++, curIndex);
            }

            curIndex = index.getNextMessageOfType(mgid, curIndex);
        }
        rowCount = rowIndex;
    }

    protected void load(double initTime, double finalTime) {

    }

    public IndexedLogTableModel(IMraLogGroup source, String msgName) {
        this(source, msgName, -1l, Long.MAX_VALUE);
    }

    public IndexedLogTableModel(IMraLogGroup source, String msgName, long initTime, long finalTime) {
        this.index = source.getLsfIndex();
        this.imcMsgType = index.getDefinitions().getType(msgName);

        // column names
        names = new Vector<String>();
        names.add("time");
        names.add("src");
        names.add("src_ent");
        names.add("dst");
        names.add("dst_ent");

        for (String element : imcMsgType.getFieldNames()) {
            if (imcMsgType.getFieldUnits(element) != null && !imcMsgType.getFieldUnits(element).isEmpty()) {
                names.add(element + " " + "(" + imcMsgType.getFieldUnits(element) + ")");
            }
            else
                names.add(element);
        }

        // msg names to use on getValueAt() method
        msgNames = new Vector<String>();
        msgNames.add("time");
        msgNames.add("src");
        msgNames.add("src_ent");
        msgNames.add("dst");
        msgNames.add("dst_ent");
        msgNames.addAll(imcMsgType.getFieldNames());

        // load the "row <-> msg index" table
        loadIndexes(initTime/1000.0, finalTime/1000.0);
    }

    @Override
    public int getColumnCount() {
        if (names == null)
            return 0;

        return names.size();
    }

    @Override
    public int getRowCount() {
        if (index == null)
            return 1;
        return rowCount;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (index == null) {
            return "Unable to load data";
        }

        // retrieve the message that should go into this column from the cache
        IMCMessage m = getMessage(rowIndex);
        // given the column name show the resulting value
        if (m != null) {
            if (columnIndex <= 4) {
                switch (msgNames.get(columnIndex)) {
                    case "time":
                        return m.getTimestampMillis();
                    case "src":
                        return m.getSourceName();
                    case "src_ent":
                        return index.getEntityName(m.getSrc(), m.getSrcEnt());
                    case "dst":
                        return index.getSystemName(m.getDst());
                    case "dst_ent":
                        return index.getEntityName(m.getDst(), m.getDstEnt());
                    default:
                        return null;
                }
            }
            else {
                String type = m.getTypeOf(msgNames.get(columnIndex));
                if (type.startsWith("uint") || type.startsWith("int") || type.startsWith("fp")) {
                    String unit = m.getUnitsOf(msgNames.get(columnIndex));
                    Number nb = null;
                    nb = m.getAsNumber(msgNames.get(columnIndex));
                    if (nb != null
                            && (unit == null || !"enumerated".equalsIgnoreCase(unit.toLowerCase())
                            && !"bitmask".equalsIgnoreCase(unit.toLowerCase())
                            && !"bitfield".equalsIgnoreCase(unit.toLowerCase())))
                        return nb;
                }
                return ""+m.getString(msgNames.get(columnIndex), false);
            }
        }
        return null;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        if (getValueAt(0, c) == null)
            return String.class;
        return getValueAt(0, c).getClass();
    }

    @Override
    public String getColumnName(int column) {
        return names.get(column);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
