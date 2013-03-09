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
 * 2008/04/13
 */
package pt.up.fe.dceg.neptus.util.comm.manager.imc;

import java.awt.Window;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.PluginProperty;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

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
		JFrame frame = GuiUtils.testFrame(new JButton("teste"));
		frame.setSize(100,100);
		
		
		IMCMessage msg;
		
		msg = IMCDefinition.getInstance().create("PlanSpecification");
		msg.dump(System.out);
		showProperties(msg, frame, true);
		msg.dump(System.out);

	}
}
