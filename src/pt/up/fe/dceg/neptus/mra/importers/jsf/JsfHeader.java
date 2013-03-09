/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Feb 5, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

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
