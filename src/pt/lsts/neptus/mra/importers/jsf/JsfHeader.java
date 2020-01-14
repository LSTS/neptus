/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Feb 5, 2013
 */
package pt.lsts.neptus.mra.importers.jsf;

import java.nio.ByteBuffer;

/**
 * @author jqcorreia
 *
 */
public class JsfHeader {
    private short type;
    private byte commandType;
    private byte subsystem;
    private byte channel;
    private byte sequenceNumber;
    private int messageSize;
    /**
     * @return the type
     */
    public short getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(short type) {
        this.type = type;
    }
    /**
     * @return the commandType
     */
    public byte getCommandType() {
        return commandType;
    }
    /**
     * @param commandType the commandType to set
     */
    public void setCommandType(byte commandType) {
        this.commandType = commandType;
    }
    /**
     * @return the subsystem
     */
    public byte getSubsystem() {
        return subsystem;
    }
    /**
     * @param subsystem the subsystem to set
     */
    public void setSubsystem(byte subsystem) {
        this.subsystem = subsystem;
    }
    /**
     * @return the channel
     */
    public byte getChannel() {
        return channel;
    }
    /**
     * @param channel the channel to set
     */
    public void setChannel(byte channel) {
        this.channel = channel;
    }
    /**
     * @return the sequenceNumber
     */
    public byte getSequenceNumber() {
        return sequenceNumber;
    }
    /**
     * @param sequenceNumber the sequenceNumber to set
     */
    public void setSequenceNumber(byte sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    /**
     * @return the messageSize
     */
    public int getMessageSize() {
        return messageSize;
    }
    /**
     * @param messageSize the messageSize to set
     */
    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }
    
    void parse(ByteBuffer buffer) {
        setType(buffer.getShort(4));
        setCommandType(buffer.get(6));
        setSubsystem(buffer.get(7));
        setChannel(buffer.get(8));
        setSequenceNumber(buffer.get(9));
        setMessageSize(buffer.getInt(12));
    }
}
