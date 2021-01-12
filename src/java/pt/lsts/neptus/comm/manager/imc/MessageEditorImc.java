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
 * Author: Paulo Dias
 * 2008/04/13
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.Window;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
public class MessageEditorImc implements PropertiesProvider {
//  @FIXME Ver melhor as Enumeration, BitMask e TupleList editors

	private IMCMessage message = null;
	private boolean editable = true;

	/**
	 * Creates a new MessageEditor which will have the message's fields as properties
	 * @param message
	 */
	public MessageEditorImc(IMCMessage message) {
		setMessage(message);
	}

	/**
	 * Sets this Editor's message
	 * @param message The Message to be viewed / edited
	 */
	public void setMessage(IMCMessage message) {
		this.message = message;

	}

	/**
	 * @see PropertiesProvider#getProperties()
	 */
	public DefaultProperty[] getProperties() {
		Vector<PluginProperty> properties = IMCUtils.getProperties(message);
		return properties.toArray(new DefaultProperty[] {});
	}

	/**
	 * @see PropertiesProvider#getPropertiesDialogTitle() 
	 */	
	public String getPropertiesDialogTitle() {		
		return "Properties for " + message.getMessageType().getShortName()
			+ " message, with ID " + message.getMessageType().getId();
	}

	/**
	 * @see PropertiesProvider#getPropertiesErrors(Property[])
	 */	
	public String[] getPropertiesErrors(Property[] properties) {
		Vector<String> errors = new Vector<String>();
		return errors.toArray(new String[] {});
	}

	/**
	 * @see PropertiesProvider#setProperties(Property[])
	 */
	public void setProperties(Property[] properties) {
		IMCUtils.setProperties(properties, message);
	}

	/**
	 * This method shows a dialog where the user can view/edit the given message's fields
	 * @param message Any Message
	 * @param editable Whether the user will be able to edit the message's fields
	 */
	public static boolean showProperties(IMCMessage message, boolean editable) {
		MessageEditorImc me = new MessageEditorImc(message);
		me.setEditable(editable);
		return PropertiesEditor.editProperties(me, true);
	}

	public static boolean showProperties(IMCMessage message, Window parent, boolean editable) {
		MessageEditorImc me = new MessageEditorImc(message);
		me.setEditable(editable);
		return PropertiesEditor.editProperties(me, parent, true);
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	/**
	 * Testing purposes
	 * @param args <b>ignored</b>
	 */
	public static void main(String[] args) {
		ConfigFetch.initialize();
		
		IMCMessage msg = new PlanControl(); 		
		msg.dump(System.out);
		showProperties(msg, null, true);
		msg.dump(System.out);

	}
}
