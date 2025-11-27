/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Objects;
import java.util.TimeZone;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.IridiumTxStatus;
import pt.lsts.imc.StateReport;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
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
    private final List<IridiumMessage> msgsList =  Collections.synchronizedList(new ArrayList<>());
    protected static final int TIMESTAMP = 0, SYSTEM = 1, STATUS = 2, MSG_TYPE = 3;
    final private String[] statusTooltips = { "Message delivered to recipient(s)", "Error sending message",
            "No confirmation of reception", "Executing a SOICOMMAND or an IRIDIUMCOMMAND" };
    private String jsonView;
    private String hexView;
    private String lastJsonData = null;
    private String lastHexData = null;
    private boolean hasJsonData;

    public enum IridiumCommsStatus {
        DELIVERED,
        ERROR,
        UNCERTAIN,
        EXECUTING // TODO Plan commanded via Iridium: SOICOMMAND or IRIDIUMCOMMAND
    }

    public IridiumStatusTableModel() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // TimeZone.getDefault().getID())
        NeptusLog.pub().info("Initialized table with timezone: {}", sdf.getTimeZone().getID());
        ImcMsgManager.getManager().addListener(this, new TypedMessageFilter(IridiumMsgRx.class.getSimpleName(),
                IridiumTxStatus.class.getSimpleName(), IridiumMsgTx.class.getSimpleName()));
    }

    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {
        if (msg.getMgid() == IridiumMsgRx.ID_STATIC) {
            IridiumMessage m;
            try {
                Date now = new Date();
                m = IridiumMessage.deserialize(msg.getRawData("data"));
                //msgs.addElement(m); // m.source == ImcMsgManager.getManager().getLocalId().intValue()
                if (!new Date(m.timestampMillis).before(now) && !Objects.equals(msg.getDate(), new Date(0))) {
                    m.timestampMillis = msg.getDate().getTime();
                }
                if (m.source == ImcId16.NULL_ID.intValue()) {
                    m.source = msg.getSrc();
                }
                if (m.destination == ImcId16.NULL_ID.intValue()) {
                    m.destination = msg.getDst();
                }
                synchronized (msgsList) {
                    msgsList.add(m);
                    fireTableRowsInserted(msgsList.size() - 1, msgsList.size() - 1);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message: " + e.getMessage()));
                m = new IridiumCommand();
                m.destination = msg.getDst();
                m.source = msg.getSrc();
                m.timestampMillis = msg.getTimestampMillis();
                m.setMessageType(0);
                ((IridiumCommand) m).setCommand(new String(((IridiumMsgRx) msg).getData()));
                synchronized (msgsList){
                    msgsList.add(m);
                    fireTableRowsInserted(msgsList.size() - 1, msgsList.size() - 1);
                }
            }
        }
        else if (msg.getMgid() == IridiumMsgTx.ID_STATIC) {
            IridiumMessage m;
            try {
                Date now = new Date();
                m = IridiumMessage.deserialize(msg.getRawData("data"));
                if (!new Date(m.timestampMillis).before(now) && !Objects.equals(msg.getDate(), new Date(0))) {
                    m.timestampMillis = msg.getDate().getTime();
                }
                if (m.source == ImcId16.NULL_ID.intValue()) {
                    m.source = msg.getSrc();
                }
                if (m.destination == ImcId16.NULL_ID.intValue()) {
                    m.destination = msg.getDst();
                }
                synchronized (msgsList) {
                    msgsList.add(m);
                    if (msg.getSrc() == ImcMsgManager.getManager().getLocalId().intValue()) { // Only keeps local
                        // requests info
                        int req_id = ((IridiumMsgTx) msg).getReqId();
                        status.put(msgsList.size(), new TransmissionStatus(req_id, IridiumCommsStatus.UNCERTAIN,
                                IridiumMsgTx.class.getSimpleName()));
                    }
                    fireTableRowsInserted(msgsList.size() - 1, msgsList.size() - 1);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message: " + e.getMessage()));
                m = new IridiumCommand();
                m.destination = msg.getDst();
                m.source = msg.getSrc();
                m.timestampMillis = msg.getTimestampMillis();
                m.setMessageType(0);
                ((IridiumCommand) m).setCommand(new String(((IridiumMsgTx) msg).getData()));

                synchronized (msgsList) {
                    msgsList.add(m);
                    int req_id = ((IridiumMsgTx) msg).getReqId();
                    if (msg.getSrc() == ImcMsgManager.getManager().getLocalId().intValue())
                        status.put(msgsList.size(),
                                new TransmissionStatus(req_id, IridiumCommsStatus.UNCERTAIN, "Custom Iridium Message")); // Only
                    fireTableRowsInserted(msgsList.size() - 1, msgsList.size() - 1);
                }
            }
        }
        else if (msg.getMgid() == IridiumTxStatus.ID_STATIC) {
            if (msg.getSrc() == ImcMsgManager.getManager().getLocalId().intValue()) { // Only local requests info
                int req_id = ((IridiumTxStatus) msg).getReqId();
                pt.lsts.imc.IridiumTxStatus.STATUS s = ((IridiumTxStatus) msg).getStatus();
                synchronized (msgsList) {
                    for (Entry<Integer, TransmissionStatus> entry : status.entrySet()) {
                        if (entry.getValue().req_id == req_id) {
                            String oldName = entry.getValue().messageType;
                            if (s.equals(pt.lsts.imc.IridiumTxStatus.STATUS.OK)) {
                                status.put(entry.getKey(),
                                        new TransmissionStatus(req_id, IridiumCommsStatus.DELIVERED, oldName));
                                fireTableRowsInserted(msgsList.size() - 1, msgsList.size() - 1);
                            }
                            else if (s.equals(pt.lsts.imc.IridiumTxStatus.STATUS.ERROR)) {
                                status.put(entry.getKey(),
                                        new TransmissionStatus(req_id, IridiumCommsStatus.ERROR, oldName));
                                fireTableRowsInserted(msgsList.size() - 1, msgsList.size() - 1);
                            }
                        }
                    }
                }
            }
        }
    }

    public String getMessageData(int row) throws Exception {
        IridiumMessage msg;
        synchronized(this.msgsList) {
            msg = (IridiumMessage)this.msgsList.get(row);
        }

        if (msg == null) {
            return "<html><b>No data</b></html>";
        } else {
            StringBuilder html = new StringBuilder("<html>");
            StringBuilder jsonBuilder = new StringBuilder();

            for(IMCMessage m : msg.asImc()) {
                String part = IMCUtil.getAsHtml(m);
                part = part.replace("<html>", "").replace("</html>", "");
                html.append(part).append("<br/><hr/>");
                jsonBuilder.append(m.asJSON()).append("\n");
            }

            String hex = new String(Hex.encodeHex(msg.serialize()));
            String formattedHex = hex.replaceAll("(.{44})", "$1<br>");
            html.append("<b>HEX Data:</b><br>");
            html.append("<div style=\"background-color:#f5f5f5;padding:10px;font-family:monospace;white-space:pre-wrap;line-height:1.4;\">" + formattedHex + "</div>");
            html.append("</html>");
            return html.toString();
        }
    }

    public void populateMessageViews(int row) throws Exception {
        IridiumMessage msg;
        synchronized(this.msgsList) {
            msg = (IridiumMessage)this.msgsList.get(row);
        }

        if (msg == null) {
            this.jsonView = "";
            this.hexView = "";
        } else {
            StringBuilder textBuilder = new StringBuilder();

            for(IMCMessage message : msg.asImc()) {
                textBuilder.append(message.toString());
                textBuilder.append('\n');
            }

            this.hexView = new String(Hex.encodeHex(msg.serialize()));
            this.jsonView = textBuilder.toString();
        }
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
        synchronized (msgsList) {
            return msgsList.size();
        }
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            IridiumMessage m = null;
            synchronized (msgsList) {
                m = msgsList.get(rowIndex);
            }

            String messageType = m.getMessageType() == 0 ? "Custom Iridium Message" : m.getClass().getSimpleName();
            int src = m.getSource();
            int dst = m.getDestination();

            switch (columnIndex) {
                case TIMESTAMP: {
                    try {
                        if (messageType.equalsIgnoreCase("ImcIridiumMessage") ||
                                messageType.equalsIgnoreCase("ImcFullIridiumMessage")) {
                            IMCMessage msg = ((ImcIridiumMessage) m).getMsg();
                            if (msg.getMgid() == StateReport.ID_STATIC) {
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
                    catch (Exception e) {
                        NeptusLog.pub().warn("?? " + messageType + " " + m + " :: " + e.getMessage());
                        return "?? " + messageType + " " + m;
                    }
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
                    if (messageType.equalsIgnoreCase("IridiumCommand")) {
                        IridiumCommand cmd = (IridiumCommand) m;
                        String txt = cmd.getCommand();
                        if (txt.startsWith("ERROR"))
                            return IridiumCommsStatus.ERROR;

                    }
                    else if (dst == 65535 || dst == ImcMsgManager.getManager().getLocalId().intValue()) { // dst - 65535 - 255 // - broadcast
                        return IridiumCommsStatus.DELIVERED;
                    }
                    return IridiumCommsStatus.UNCERTAIN;
                case MSG_TYPE:
                    try {
                        if (messageType.equalsIgnoreCase("ImcIridiumMessage") ||
                                messageType.equalsIgnoreCase("ImcFullIridiumMessage"))
                            return ((ImcIridiumMessage) m).getMsg().getClass().getSimpleName();
                        else
                            return messageType;
                    } catch (Exception e) {
                        return "::" + messageType;
                    }
                default:
                    return "??";
            }
        } catch (Exception e) {
            return "?? " + rowIndex + " " + columnIndex + "  " + e.getMessage();
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
     * @param millis - milliseconds
     */
    public void cleanupAfter(long millis) {
        synchronized (msgsList) {
            msgsList.removeIf(msg -> (System.currentTimeMillis() - msg.timestampMillis) >  millis);
            fireTableDataChanged();
        }
    }

    public String getJsonView(int row) throws Exception {
        populateMessageViews(row);
        return jsonView;
    }

    public String getHexView(int row) throws Exception {
        populateMessageViews(row);
        return hexView;
    }

    /**
     * @return The JSON representation of the last requested message, or empty string if not available
     */
    public String getLastJsonData() {
        return lastJsonData != null ? lastJsonData : "";
    }
    
    /**
     * @return The HEX representation of the last requested message, or empty string if not available
     */
    public String getLastHexData() {
        return lastHexData != null ? lastHexData : "";
    }
    
    /**
     * @return true if the last requested message has JSON data available
     */
    public boolean hasJsonData() {
        return hasJsonData;
    }

}

/**
 * Iridium Local Transmissions StatusTDefaultRowSorter
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
