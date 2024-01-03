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
 * Apr 30, 2014
 */
package pt.lsts.neptus.plugins.sunfish.iridium;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.neptus.comm.iridium.IridiumMessage;


/**
 * @author zp
 *
 */
public class IridiumMessagesTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
    Vector<IrMsg> msgs = new Vector<>();

    public void addReception(IridiumMsgRx msg) throws Exception {
        IridiumMessage m = IridiumMessage.deserialize(msg.getData());
        IrMsg irmsg = new IrMsg();
        irmsg.size = msg.getData().length;
        irmsg.data = new String(Hex.encodeHex(msg.getData()));
        irmsg.trans = "Reception";
        irmsg.source = IMCDefinition.getInstance().getResolver().resolve(m.getSource());
        irmsg.destination = IMCDefinition.getInstance().getResolver().resolve(m.getDestination());
        irmsg.type = m.getClass().getSimpleName();        
        irmsg.time = msg.getDate();
        
        Collections.sort(msgs);
        msgs.add(irmsg);
        fireTableDataChanged();
    }
    
    public void addTransmission(IridiumMsgTx msg) throws Exception {
        IridiumMessage m = IridiumMessage.deserialize(msg.getData());
        IrMsg irmsg = new IrMsg();
        irmsg.size = msg.getData().length;
        irmsg.data = new String(Hex.encodeHex(msg.getData()));
        irmsg.trans = "Transmission Request";
        irmsg.source = IMCDefinition.getInstance().getResolver().resolve(m.getSource());
        irmsg.destination = IMCDefinition.getInstance().getResolver().resolve(m.getDestination());
        irmsg.type = m.getClass().getSimpleName();        
        irmsg.time = msg.getDate();
        msgs.add(irmsg);
        Collections.sort(msgs);
        fireTableDataChanged();        
    }
    
    public String getMessageData(int row) {
        return msgs.get(row).data;        
    }
    
    private static final int TIMESTAMP = 0, SOURCE = 1, DESTINATION = 2, TYPE = 3, SIZE = 4, DATA = 5;

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case TIMESTAMP:
                return "Time";
            case SOURCE:
                return "Source";
            case DESTINATION:
                return "Destination";
            case TYPE:
                return "Type";
            case SIZE:
                return "Size";
            case DATA:
                return "Data";
            default:
                return "??";
        }
    }

    public IridiumMessagesTableModel() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public int getRowCount() {
        return msgs.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        IrMsg m = msgs.get(rowIndex);

        switch (columnIndex) {
            case TIMESTAMP:
                return sdf.format(m.time);
            case SOURCE:
                return m.source;
            case DESTINATION:
                return m.destination;
            case TYPE:
                return m.trans;
            case SIZE:
                return m.size;
            case DATA:
                return m.data;
            default:
                return "??";
        }
    }
    
    class IrMsg implements Comparable<IrMsg> {
        public int size;
        public String trans, source, destination, type, data;
        public Date time;
        
        public int compareTo(IrMsg o) {
            return time.compareTo(o.time);
        }
    }

}
