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
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystems;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageEditorImc;

import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PayloadConfig extends JPanel {
	protected MiscSystems baseSystem = null;
	protected Element xmlImcNode = null;
	
	protected IMCMessage message = null;
	
	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();

	/**
	 * 
	 */
	public PayloadConfig() {
		initialize();
	}
	
	/**
	 * 
	 */
	private void initialize() {
		textField.setEditable(false);
		this.setLayout(new BorderLayout(0,0));
		this.add(textField, BorderLayout.CENTER);
		this.add(button, BorderLayout.EAST);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTextField();
				getDialog(PayloadConfig.this, "Plan Actions");
			}
		});
	}
	
	/**
     * @param textField the textField to set
     */
    protected void updateTextField() {
        String txtMsg = "";
        if (baseSystem == null)
            txtMsg = "Unknown Payload";
        else
            txtMsg = baseSystem.getName();
        this.textField.setText(txtMsg + "::" + message.asJSON());
    }

//	public PayloadConfig(Element xmlNode) {
//		setXmlNode(xmlNode);
//	}
	
	public boolean load() {
		//FIXME try to use the parent super.load()
		try {
			message = IMCMessage
					.parseXml(((Element) (xmlImcNode)).detach().asXML());						
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(this.getClass().getSimpleName()+
					":"+this.hashCode()+": Creating a new message");
			
			message = baseSystem.getOptionsConfiguration().createNewMessage();
			try {
				xmlImcNode = (Element) DocumentHelper.parseText(message.asXml(false)).getRootElement().detach();
			} catch (DocumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
        updateTextField();
        return true;
	}
	
	/**
	 * @return the baseSystem
	 */
	public MiscSystems getBaseSystem() {
		return baseSystem;
	}
	
	/**
	 * @param baseSystem the baseSystem to set
	 */
	public void setBaseSystem(MiscSystems baseSystem) {
		this.baseSystem = baseSystem;
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
		boolean userCanceled;
        if (baseSystem == null)
            userCanceled = MessageEditorImc.showProperties(message,
                    SwingUtilities.getWindowAncestor(parent), true);
        else
            userCanceled = baseSystem.getOptionsConfiguration().showDialog(message,
                    SwingUtilities.getWindowAncestor(parent));
		if (!userCanceled) {
			try {
				xmlImcNode = (Element) DocumentHelper.parseText(message.asXml(false)).getRootElement().detach();
			} catch (DocumentException e1) {
				e1.printStackTrace();
			}
            updateTextField();
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
	    PayloadConfig clone = new PayloadConfig();
	    clone.baseSystem = baseSystem;
	    clone.message = message.cloneMessage();
        clone.setXmlImcNode((Element) xmlImcNode.clone());
	    return clone;
	}
}
