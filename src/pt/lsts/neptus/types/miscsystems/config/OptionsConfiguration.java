/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 2010/06/25
 */
package pt.lsts.neptus.types.miscsystems.config;

import java.awt.Window;

import javax.swing.SwingUtilities;

import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.util.comm.manager.imc.MessageEditorImc;
import pt.lsts.imc.IMCMessage;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 *
 */
public class OptionsConfiguration implements PropertiesProvider {

	protected Element configurationXMLElement = null;
	protected IMCMessage message = null;

	/**
	 * @param configurationXMLElement
	 */
	public void load(Element configurationXMLElement) {
		this.configurationXMLElement = configurationXMLElement;
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.PropertiesProvider#getProperties()
	 */
	@Override
	public DefaultProperty[] getProperties() {
		return null;
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesDialogTitle()
	 */
	@Override
	public String getPropertiesDialogTitle() {
		return "Properties for ";
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
	 */
	@Override
	public String[] getPropertiesErrors(Property[] properties) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
	 */
	@Override
	public void setProperties(Property[] properties) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return
	 */
	public IMCMessage createNewMessage() {
		return message.cloneMessage();
	}

	/**
	 * @param message2
	 * @param windowAncestor
	 */
	public boolean showDialog(IMCMessage message, Window parent) {
		return MessageEditorImc.showProperties(message, SwingUtilities.getWindowAncestor(parent), true);
	}

	public static void main(String[] args) {
	    String[] sp = "CATWRK3|VALSOU|".split("[|]");
	    for (String str : sp)
	        NeptusLog.pub().info("<###> "+str);
	}

}
