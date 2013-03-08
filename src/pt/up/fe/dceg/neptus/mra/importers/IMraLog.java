/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/10/20
 * $Id:: IMraLog.java 9880 2013-02-07 15:23:52Z jqcorreia                 $:
 */
package pt.up.fe.dceg.neptus.mra.importers;

import java.util.Collection;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCMessageType;

/**
 * This interface represents a Log File full of data that was generated / received as messages
 * @author zp
 */
public interface IMraLog {

    public IMCMessage getEntryAtOrAfter(long timestamp);
	public IMCMessage getEntryAtOrAfter(long timestamp, String entityName);
	/**
	 * Retrieve a name that will identify this log file (like the name of the message)
	 * @return A string with the log name
	 */
	public String name();
	
	/**
	 * Retrieve the last entry in this log
	 */
	public IMCMessage getLastEntry();
	
	/**
	 * Retrieve the log format as a Neptus message type
	 * @return a MessageType indicating the various log fields. 
	 * The message type may or may not be attached to an existing IMC message. The latter happens for 
	 * new messages and also for different IMC versions.
	 */
	public IMCMessageType format();
	
	/**
	 * Retrieve meta-information associated with this log
	 * @return Log Meta-information
	 */
	public LinkedHashMap<String, Object> metaInfo();
	
	/**
	 * The timestamp of the current log entry (first entry by default)
	 * @return The time of the active log entry
	 */
	public long currentTimeMillis();
	
	/**
	 * Advance to the next Log entry and retrieve it as a IMCMessage
	 * @return The next log entry as a IMCMessage or null if no more message exist
	 */
	public IMCMessage nextLogEntry();
	
	/**
	 * Goes back to the first Log Entry and retrieves it
	 * @return Retrieves the first log entry it an entry exists in this log file or otherwise returns null
	 */
	public IMCMessage firstLogEntry();
	
	/**
	 * Advance millis milliseconds in the log
	 * @param millis The time to advance, in milliseconds
	 */
	public void advance(long millis);
	
	/**
     * Retrieve all messages that have the given timestamp
     * @param timeStampMillis Time stamp, in milliseconds
     * @return A collection of IMCMessages that have the given time stamp
     */
    public IMCMessage getCurrentEntry();
    
	
	/**
	 * Retrieve all messages that have the given timestamp
	 * @param timeStampMillis Time stamp, in milliseconds
	 * @return A collection of IMCMessages that have the given time stamp
	 */
	public Collection<IMCMessage> getExactTimeEntries(long timeStampMillis);
	
	/**
	 * Retrieve the total number of entries in this log
	 * @return the total number of entries in this log
	 */
	public int getNumberOfEntries();
}
