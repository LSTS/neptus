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
 * Jan 30, 2014
 */
package pt.lsts.neptus.util.llf;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.collections4.map.LRUMap;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;

/**
 * @author zp
 *
 */
public class RawMessagesTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private LsfIndex index;
    private SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
    
    LRUMap map = new LRUMap(100);
    
    private static final int NUM = 0, TIMESTAMP = 1, MGID = 2, SRC = 3, SRC_ENT = 4, DST = 5, DST_ENT = 6, SIZE = 7;  
    
    @Override
    public String getColumnName(int column) {
        switch (column) {
            case NUM:
                return "Index";
            case TIMESTAMP:
                return "Time";
            case MGID:
                return "Type";
            case SRC:
                return "Source";
            case SRC_ENT:
                return "Source Entity";
            case DST:
                return "Destination";
            case DST_ENT:
                return "Destination entity";
            case SIZE:
                return "Size";                
            default:
                return "??";
        }
    }
    public RawMessagesTableModel(LsfIndex index) {
        this.index = index;
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    @Override
    public int getRowCount() {
        return index.getNumberOfMessages();
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
         if (!map.containsKey(rowIndex))
            map.put(rowIndex, index.getMessage(rowIndex));
        
        switch (columnIndex) {
            case NUM:
                return String.format("%8d", rowIndex);
            case TIMESTAMP:
                return sdf.format(new Date((long)(1000.0*index.timeOf(rowIndex))));
            case SRC:
                return index.sourceNameOf(rowIndex);
            case SRC_ENT:
                return index.entityNameOf(rowIndex);
            case DST:
                return index.getDefinitions().getResolver().resolve(((IMCMessage) map.get(rowIndex)).getDst());
            case DST_ENT:
                IMCMessage m = (IMCMessage) map.get(rowIndex);
                if (m.getSrc() == m.getDst())
                    return index.entityNameOf(m.getDstEnt());
                else
                    return m.getDstEnt();
            case MGID:
                return index.getDefinitions().getMessageName(index.typeOf(rowIndex));
            case SIZE:
                return index.sizeOf(rowIndex);
            default:
                return "??";
        }
    }

}
