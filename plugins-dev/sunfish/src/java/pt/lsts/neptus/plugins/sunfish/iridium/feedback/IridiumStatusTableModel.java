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
 * Author: keila
 */
package pt.lsts.neptus.plugins.sunfish.iridium.feedback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.IridiumTxStatus;
import pt.lsts.imc.StateReport;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.TypedMessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.plugins.sunfish.iridium.feedback.IridiumStatusTableModel.IridiumCommsStatus;

public class IridiumStatusTableModel extends AbstractTableModel implements MessageListener<MessageInfo, IMCMessage> {

    private static final long serialVersionUID = 1L;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS dd-MM-yyyy 'Z'");
    private Map<Integer, TransmissionStatus> status = Collections.synchronizedMap(new HashMap<>());
    List<IridiumMessage> msgs =  Collections.synchronizedList(new ArrayList<>());
    protected static final int TIMESTAMP = 0, SYSTEM = 1, STATUS = 2, MSG_TYPE = 3;
    final private String[] statusTooltips = { "Message delivered to recipient(s)", "Error sending message",
            "No confirmation of reception", "Executing a SOICOMMAND or an IRIDIUMCOMMAND" };

    public enum IridiumCommsStatus {
        DELIVERED,
        ERROR,
        UNCERTAIN,
        EXECUTING // TODO Plan commanded via Iridium: SOICOMMAND or IRIDIUMCOMMAND
    }

    public IridiumStatusTableModel() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // TimeZone.getDefault().getID())
        System.out.println("Set Timezone to: "+sdf.getTimeZone().getID());
        ImcMsgManager.getManager().addListener(this, new TypedMessageFilter(IridiumMsgRx.class.getSimpleName(),
                IridiumTxStatus.class.getSimpleName(), IridiumMsgTx.class.getSimpleName()));
    }

    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {
        if (msg.getMgid() == IridiumMsgRx.ID_STATIC) {
            IridiumMessage m;
            try {
                m = IridiumMessage.deserialize(msg.getRawData("data"));
                //msgs.addElement(m); // m.source == ImcMsgManager.getManager().getLocalId().intValue()
                msgs.add(m);
                fireTableRowsInserted(msgs.size()-1, msgs.size()-1);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message: " + e.getMessage()));
                m = new IridiumCommand();
                m.destination = msg.getDst();
                m.source = msg.getSrc();
                m.timestampMillis = msg.getTimestampMillis();
                m.setMessageType(0);
                ((IridiumCommand) m).setCommand(new String(((IridiumMsgRx) msg).getData()));
                msgs.add(m);
                fireTableRowsInserted(msgs.size()-1, msgs.size()-1);

            }
        }
        else if (msg.getMgid() == IridiumMsgTx.ID_STATIC) {
            IridiumMessage m;
            try {
                m = IridiumMessage.deserialize(msg.getRawData("data"));
                synchronized (msgs) {
                    msgs.add(m);
                    if (msg.getSrc() == ImcMsgManager.getManager().getLocalId().intValue()) { // Only keeps local
                        // requests info
                        int req_id = ((IridiumMsgTx) msg).getReqId();
                        status.put(msgs.size(), new TransmissionStatus(req_id, IridiumCommsStatus.UNCERTAIN,
                                IridiumMsgTx.class.getSimpleName()));
                    }
                }
                fireTableRowsInserted(msgs.size()-1, msgs.size()-1);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message: " + e.getMessage()));
                m = new IridiumCommand();
                m.destination = msg.getDst();
                m.source = msg.getSrc();
                m.timestampMillis = msg.getTimestampMillis();
                m.setMessageType(0);
                ((IridiumCommand) m).setCommand(new String(((IridiumMsgTx) msg).getData()));

                synchronized (msgs) {
                    msgs.add(m);
                    int req_id = ((IridiumMsgTx) msg).getReqId();
                    if (msg.getSrc() == ImcMsgManager.getManager().getLocalId().intValue())
                        status.put(msgs.size(),
                                new TransmissionStatus(req_id, IridiumCommsStatus.UNCERTAIN, "Custom Iridium Message")); // Only
                }
                fireTableRowsInserted(msgs.size()-1, msgs.size()-1);
            }
        }
        else if (msg.getMgid() == IridiumTxStatus.ID_STATIC) {
            if (msg.getSrc() == ImcMsgManager.getManager().getLocalId().intValue()) { // Only local requests info
                int req_id = ((IridiumTxStatus) msg).getReqId();
                pt.lsts.imc.IridiumTxStatus.STATUS s = ((IridiumTxStatus) msg).getStatus();
                for (Entry<Integer, TransmissionStatus> entry : status.entrySet()) {
                    if (entry.getValue().req_id == req_id) {
                        String oldName = entry.getValue().messageType;
                        if (s.equals(pt.lsts.imc.IridiumTxStatus.STATUS.OK)) {
                            status.put(entry.getKey(),
                                    new TransmissionStatus(req_id, IridiumCommsStatus.DELIVERED, oldName));
                            fireTableRowsInserted(msgs.size()-1, msgs.size()-1);
                        }
                        else if (s.equals(pt.lsts.imc.IridiumTxStatus.STATUS.ERROR)) {
                            status.put(entry.getKey(),
                                    new TransmissionStatus(req_id, IridiumCommsStatus.ERROR, oldName));
                            fireTableRowsInserted(msgs.size()-1, msgs.size()-1);
                        }
                    }
                }
            }
        }
    }

    public String getMessageData(int row) throws Exception {
        IridiumMessage msg = msgs.get(row);
        StringBuilder data = new StringBuilder();
        for (IMCMessage message : msg.asImc()) {
            data.append(message.toString());
            data.append('\n');
        }
        data.append("HEX DATA: " + new String(Hex.encodeHex(msg.serialize())) + "\n");
        return data.toString();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case TIMESTAMP:
                return "Time";
            case SYSTEM:
                return "System";
            case STATUS:
                return "Status";
            case MSG_TYPE:
                return "Message Type";
            default:
                return "??";
        }
    }

    @Override
    public int getRowCount() {
        return msgs.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        IridiumMessage m = msgs.get(rowIndex);
        
        String messageType = m.getMessageType() == 0 ? "Custom Iridium Message" : m.getClass().getSimpleName();
        int src = m.getSource();
        int dst = m.getDestination();

        switch (columnIndex) {
            case TIMESTAMP:
            {
                if(messageType.equalsIgnoreCase("ImcIridiumMessage")) {
                    IMCMessage msg = ((ImcIridiumMessage) m).getMsg();
                    if(msg.getMgid() == StateReport.ID_STATIC) {
                        long stime = ((StateReport) msg).getStime() * 1000;
                        TimeZone.getTimeZone(TimeZone.getDefault().getID());
                        StringBuilder sb = new StringBuilder("V ");
                        sb.append(sdf.format(new Date(stime)));
                        return sb.toString();
                    }
                }
                StringBuilder sb = new StringBuilder("M ");
                sb.append(sdf.format(new Date(m.timestampMillis)));
                return sb.toString();
            }
            case SYSTEM:
                return IMCDefinition.getInstance().getResolver().resolve(src);
            case STATUS:
                if (status.containsKey(rowIndex)) {
                    return status.get(rowIndex)._status;
                }
                /*
                 * else if(src == ImcMsgManager.getManager().getLocalId().intValue()) return IridiumCommsStatus.SENT;
                 */
                if(messageType.equalsIgnoreCase("IridiumCommand")) {
                    IridiumCommand cmd = (IridiumCommand) m;
                    String txt = cmd.getCommand();
                    if(txt.startsWith("ERROR"))
                        return IridiumCommsStatus.ERROR;

                }
                else if (dst == 65535 || dst == ImcMsgManager.getManager().getLocalId().intValue()) { // dst - 65535 - 255 // - broadcast
                    return IridiumCommsStatus.DELIVERED;
                }
                    return IridiumCommsStatus.UNCERTAIN;
            case MSG_TYPE:
                if(messageType.equalsIgnoreCase("ImcIridiumMessage"))
                    return ((ImcIridiumMessage) m).getMsg().getClass().getSimpleName();
                else 
                    return messageType;
            default:
                return "??";
        }
    }

    public void clear() {
        ImcMsgManager.getManager().removeListener(this);
    }

    /**
     * @param row of the status column on hover
     * @return
     */
    public String getToolTipText(int row,int col) {
        if(col == STATUS) {
            if(getValueAt(row, col) != null) {
                IridiumCommsStatus s = (IridiumCommsStatus) getValueAt(row, 2);
                int statusEnum = s.ordinal();
                return statusTooltips[statusEnum];
            }
        }
        else if(col == TIMESTAMP) {
            if(getValueAt(row, col) != null) {
                String date = (String) getValueAt(row, col);
                if(date.startsWith("V "))
                    return "Timestamp from "+getValueAt(row,SYSTEM);
                else if(date.startsWith("M ")) {
                    return "Timestamp from Message Header";
                }
            }
        }
        return "";
    }

    /**
     * @param milis - milliseconds 
     */
    public void cleanupAfter(long millis) {
        msgs.removeIf(msg -> (System.currentTimeMillis() - msg.timestampMillis) >  millis);
        fireTableDataChanged();
    }
}

/**
 * Iridium Local Transmissions Status
 *
 */
class TransmissionStatus {
    int req_id;
    IridiumCommsStatus _status;
    String messageType;

    /**
     * @return the messageType
     */
    public String getMessageType() {
        return messageType;
    }

    TransmissionStatus(int r, IridiumCommsStatus s, String name) {
        this.req_id = r;
        this._status = s;
        this.messageType = name;
    }
}
