/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/06/25
 * $Id:: OptionsConfiguration.java 9615 2012-12-30 23:08:28Z pdias              $:
 */
package pt.up.fe.dceg.neptus.types.miscsystems.config;

import java.awt.Window;

import javax.swing.SwingUtilities;

import org.dom4j.Element;

import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageEditorImc;

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
	 * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getProperties()
	 */
	@Override
	public DefaultProperty[] getProperties() {
		return null;
	}

	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getPropertiesDialogTitle()
	 */
	@Override
	public String getPropertiesDialogTitle() {
		return "Properties for ";
	}

	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
	 */
	@Override
	public String[] getPropertiesErrors(Property[] properties) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
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
	        System.out.println(str);
	}

}
