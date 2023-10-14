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
 * Author: Paulo Dias
 * 2010/06/27
 */
package pt.lsts.neptus.mp.actions;

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

import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.MessageEditorImc;

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
