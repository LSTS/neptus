/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/10/20
 */
package pt.lsts.neptus.mra.importers;

import java.util.Collection;
import java.util.LinkedHashMap;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;

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
