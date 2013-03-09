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
 * 2010/06/27
 */
package pt.up.fe.dceg.neptus.mp.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageEditorImc;

import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PlanActionElementConfig extends JPanel {
	protected Element xmlImcNode = null;
	
	protected IMCMessage message = null;
	
	protected JTextField textField = new JTextField();
	protected JButton button = new FixedButton();

	/**
	 * 
	 */
	public PlanActionElementConfig() {
		initialize();
	}
	
	/**
	 * 
	 */
	protected void initialize() {
		textField.setEditable(false);
		this.setLayout(new BorderLayout(0,0));
		this.add(textField, BorderLayout.CENTER);
		this.add(button, BorderLayout.EAST);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textField.setText(message.asJSON());
				getDialog(PlanActionElementConfig.this, "Plan Action");
			}
		});
	}

	public boolean load() {
		try {
		    message = IMCMessage
					.parseXml(((Element) xmlImcNode).detach().asXML());
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(this.getClass().getSimpleName()+
					":"+this.hashCode()+": Creating a new message");
			message = null;
		}
		textField.setText((message!=null)?message.asJSON():"Error");
		return true;
	}
		
	/**
	 * @return the xmlNode
	 */
	public Element getXmlNode() {
		return xmlImcNode;
	}
	
	/**
	 * @param xmlImcNode the xmlNode to set
	 */
	public void setXmlImcNode(Element xmlImcNode) {
		this.xmlImcNode = (Element) xmlImcNode.detach();
		load();
	}
	
	/**
     * @return the message
     */
    public IMCMessage getMessage() {
        return message;
    }
    
    /**
     * @param message the message to set
     */
    public void setMessage(IMCMessage message) {
        this.message = message;
        try {
            xmlImcNode = (Element) DocumentHelper.parseText(message.asXml(false)).getRootElement().detach();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        textField.setText((message != null) ? message.asJSON() : "Error");
    }
	
	public boolean getDialog(Component parent, String title) {
		boolean userCanceled = MessageEditorImc.showProperties(message, SwingUtilities.getWindowAncestor(parent), true);
		if (!userCanceled) {
			try {
				xmlImcNode = (Element) DocumentHelper.parseText(message.asXml(false)).getRootElement().detach();
			} catch (DocumentException e1) {
				e1.printStackTrace();
			}
			textField.setText(message.asJSON());
		}
		else {
			load(); // revert to the old xml
		}
		return !userCanceled;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() {
        PlanActionElementConfig clone = new PlanActionElementConfig();
        clone.message = message.cloneMessage();
        clone.setXmlImcNode((Element) xmlImcNode.clone());
        return clone;
    }

}
