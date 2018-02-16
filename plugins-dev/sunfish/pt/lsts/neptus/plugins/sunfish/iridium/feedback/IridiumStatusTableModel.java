/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.IridiumTxStatus;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.TypedMessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;


public class IridiumStatusTableModel extends AbstractTableModel implements MessageListener<MessageInfo, IMCMessage> {
    private static final long serialVersionUID = 1L;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS dd-MM-YYYY");
    private Map <Integer,IridiumCommsStatus> status = Collections.synchronizedMap(new HashMap<>());
    Vector<IridiumMessage> msgs = new Vector<>();
    private ArrayList<Integer> checksums;
    protected static final int TIMESTAMP = 0, SYSTEM = 1, STATUS = 2, MSG_TYPE = 3;
    final private String []  statusTooltips = {"Message successfully sent","Message delivered to recipient(s)","Error sending Message","No confirmation of reception","Executing a SOICOMMAND or an IRIDIUMCOMMAND"};
    
    public enum IridiumCommsStatus {
        SENT,
        DELIVERED,
        ERROR,
        UNCERTAIN,
        EXECUTING // Plan commanded via Iridium: SOICOMMAND or IRIDIUMCOMMAND
    }

    public enum IridiumInlineMsg {
        STATE_REPORT,
        DEVICE_UPDATE,
        TEXT_COMMAND,
        CUSTOM_MSG,
        SOICOMMAND_ERROR,
        SOICOMMAND_SUCCESS,
        SOICOMMAND_EXEC,  
        SOICOMMAND_RESUME, 
        SOICOMMAND_STOP, 
        SOICOMMAND_REQ_PLAN,
        SOICOMMAND_REQ_PARAMS,
        SOICOMMAND_SET_PARAMS
    }


    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {

        if (msg.getMgid() == IridiumMsgRx.ID_STATIC) {
            
            System.out.println("MESSAGE RECEIVED: "+msg);
            IridiumMessage m;
            try {
                m = IridiumMessage.deserialize(msg.getRawData("data"));
                msgs.addElement(m); //m.source == ImcMsgManager.getManager().getLocalId().intValue()
                fireTableDataChanged();
            }
            catch (Exception e)
            {
                NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message: "+e.getMessage()));
                m = new IridiumCommand();
                m.destination = msg.getDst();
                m.source = msg.getSrc();
                m.timestampMillis = msg.getTimestampMillis();
                m.setMessageType(0);
                ((IridiumCommand) m).setCommand(new String(((IridiumMsgRx) msg).getData()));
                msgs.addElement(m);
                fireTableDataChanged();
            }
        }
        else if (msg.getMgid() == IridiumMsgTx.ID_STATIC) {
            IridiumMessage m;
            try {
                 m = IridiumMessage.deserialize(msg.getRawData("data"));
                msgs.addElement(m); //m.source == ImcMsgManager.getManager().getLocalId().intValue()
                fireTableDataChanged();
            }
            catch (Exception e)
            {
                NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message: "+e.getMessage()));
                m = new IridiumCommand();
                m.destination = msg.getDst();
                m.source = msg.getSrc();
                m.timestampMillis = msg.getTimestampMillis();
                m.setMessageType(0);
                ((IridiumCommand) m).setCommand(new String(((IridiumMsgTx) msg).getData()));
                msgs.addElement(m);
                fireTableDataChanged();
            }
        }
        else if (msg.getMgid() == IridiumTxStatus.ID_STATIC) {
            //TODO
            try {
                NeptusLog.pub().warn(I18n.text("LISTENER IS WORKING FOR TXStatus "+msg.asJSON()));
                fireTableDataChanged();

            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
        }

    }

    public void addReception(IridiumMsgRx iRx){
        IridiumMessage m;
        try {
            m = IridiumMessage.deserialize(iRx.getData());
            //String data = new String(Hex.encodeHex(iRx.getData()));
            msgs.addElement(m);
        }
        catch (Exception e1) {
            NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message with data: "+iRx.asJSON()));
            e1.printStackTrace();
        }
        fireTableDataChanged();
    }

    public void addTransmission(IridiumMsgTx iTx) {
        IridiumMessage m;
        status.put(iTx.getInteger("req_id"), IridiumCommsStatus.DELIVERED);
        //TODO filter own messages and make status sent until further info
        try {
            m = IridiumMessage.deserialize(iTx.getData());
            msgs.addElement(m);

        }
        catch (Exception e) {
            NeptusLog.pub().warn(I18n.text("Unable to deserialize incoming Iridium Message with data: "+iTx.getData()));
            m = new IridiumCommand();
            m.destination = iTx.getDst();
            m.source = iTx.getSrc();
            m.timestampMillis = iTx.getTimestampMillis();
            m.setMessageType(0);
            ((IridiumCommand) m).setCommand(new String(iTx.getData()));
            msgs.addElement(m);
        }
        fireTableDataChanged();        
    }
    
    /**
     * @return 
     */
    private String statusDescription(int row, int column) {
        // TODO Auto-generated method stub
        return null;
    }


    public void addStatus(IridiumTxStatus iStatus){
        //TODO -> check Map for status
        fireTableDataChanged();        
    }

    public String getMessageData(int row) throws Exception {
        IridiumMessage msg = msgs.get(row);
        //msg.toString()+"DATA:\n"+new String(Hex.encodeHex(msg.serialize()));
        return new String(Hex.encodeHex(msg.serialize()));        
    }

    @Override 
    public boolean isCellEditable(int row, int column)
    {
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

    public IridiumStatusTableModel() {
        sdf.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID())); //"UTC"
        ImcMsgManager.getManager().addListener(this,
                new TypedMessageFilter(IridiumMsgRx.class.getSimpleName(), IridiumTxStatus.class.getSimpleName(),IridiumMsgTx.class.getSimpleName()));
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
        m.getMessageType();
        String messageType = m.getMessageType()== 0 ? "Custom Iridium Message" : m.getClass().getSimpleName();
        int req_id = -1; 
        if(messageType.equalsIgnoreCase("IridiumMsgTx")){
            req_id = 0;
        }

        switch (columnIndex) {
            case TIMESTAMP:
                return sdf.format(new Date(m.timestampMillis));
            case SYSTEM:
                return IMCDefinition.getInstance().getResolver().resolve(m.getSource());
            case STATUS: 
                if(status.containsKey(req_id))
                    return status.get(req_id);
                //TODO Filter src
                else                 
                    return IridiumCommsStatus.UNCERTAIN;
            case MSG_TYPE:
                return messageType;
            default:
                return "??";
        }
    }

    /*  private String parseType(String simpleName, IridiumMessage msg) {
        if(simpleName.equalsIgnoreCase("ImcIridiumMessage"))  {
            ImcIridiumMessage m = (ImcIridiumMessage) msg;
            m.getMsg();      
        } 
        else if(simpleName.equalsIgnoreCase("IridiumCommand"))  {
            IridiumCommand m = (IridiumCommand) msg;
            m = new IridiumCommand();
            //TODO
        } 
            return simpleName; 
    }*/

    /**
     * 
     */
    public void clear() {
        // TODO Auto-generated method stub
        ImcMsgManager.getManager().removeListener(this);
    }

    /**
     * @param row of the status column on hover
     * @return
     */
    public String getToolTipText(int row) {
        IridiumCommsStatus s = (IridiumCommsStatus) getValueAt(row,2);
        int statusEnum = s.ordinal();
        return statusTooltips[statusEnum];
    }
}
