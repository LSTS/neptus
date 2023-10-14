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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.messages;

public interface IMessage extends Cloneable {

	/**
	 * @return the message serial ID
	 */
	public int getMgid();

	/**
	 * @return the message abbreviated name
	 */
	public String getAbbrev();

	/**
	 * @return the message human readable name
	 */
	public String getLongName();

	/**
	 * @return the list of field names
	 */
	public String[] getFieldNames();

	/**
	 * Verifies if the message is valid by generating exceptions when the message is not valid
	 * @throws InvalidMessageException
	 */
	public void validate() throws InvalidMessageException;

	/**
	 * Retrieve the value of a field as an Object
	 * @param fieldName The name of the field to consult
	 * @return The value of the given field or <b>null</b> if the field does not exist or an error occurred
	 */
	public Object getValue(String fieldName);

	/**
	 * Retrieve the value of a field as a String
	 * @param fieldName The name of the field to consult
	 * @return The value of the field as a String or <b>null</b> in case of an error
	 */
	public String getAsString(String fieldName);

	/**
	 * Retrieve the value of the field as a Number
	 * @param fieldName The name of the field to consult
	 * @return The numeric value of the field of <b>null</b> in case the field is not numeric / does not exist
	 */
	public Number getAsNumber(String fieldName);

	/**
	 * Retrieve the field type of the given field
	 * @return The field type of given field like "uint8_t" or "plaintext"
	 */
	public String getTypeOf(String fieldName);

	/**
	 * Retrieves the units of the given field
	 * @return The units of the field or an empty String in case no units are defined
	 */
	public String getUnitsOf(String fieldName);

	/**
	 * Verify if the message has the given flag set
	 * @return <b>true</b> if the flag is set or <b>false</b> otherwise
	 */
	public boolean hasFlag(String flagName);

	/**
	 * Retrieve the long name (human readable) of a given field
	 * @param fieldName The field's (abbreviated) name
	 * @return the long name (human readable) of a given field
	 */
	public String getLongFieldName(String fieldName);

	/**
	 * Sets the value of a field to the given Object
	 * @param fieldName The name of the field to be changed
	 * @param value The new value to be given to the field
	 * @throws InvalidFieldException in case this field does not exist in the message
	 */
	public void setValue(String fieldName, Object value) throws InvalidFieldException;


	public IMessageProtocol<? extends IMessage> getProtocolFactory();

	/**
	 * @return a copy of this message
	 */
	public <M extends IMessage> M cloneMessage();
	
	public Object getHeaderValue(String field);
}
