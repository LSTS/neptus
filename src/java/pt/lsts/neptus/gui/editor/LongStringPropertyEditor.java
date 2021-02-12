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
 * Author: José Pinto
 * Feb 25, 2010
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.fileeditor.SyntaxDocument;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.StringProperty;

/**
 * @author zp
 *
 */
public class LongStringPropertyEditor extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();
	private String value = "";

	public LongStringPropertyEditor(String value) {
		this();
		this.value = value;
	}
	
	public LongStringPropertyEditor() {
		textField.setEditable(false);
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(textField, BorderLayout.CENTER);
		((JPanel)editor).add(button, BorderLayout.EAST);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				value = showStringDialog("Text Editor", value, true, SwingUtilities.getWindowAncestor(textField));
				textField.setText(value);
				//NeptusLog.pub().info("<###>value:\n"+value);
			}
		});
	}
	
	public Object getValue() {
		return new StringProperty(value);
	}
	
	public void setValue(Object arg0) {
		this.value = arg0.toString();
	}
	
	private boolean canceled;
	String showStringDialog(String title, String previousValue, boolean editable, Component component)  {
		canceled = true;
		
		JEditorPane msgTextArea = null;
		JScrollPane jScrollPane=null;
		jScrollPane = new JScrollPane();
	
	    msgTextArea=SyntaxDocument.getCustomEditor(new String[] {}, new String[] {}, "#","");
	    msgTextArea.setEditable(editable);
	    jScrollPane.setViewportView(msgTextArea);
	    jScrollPane.setVisible(true);
	 
	    if(previousValue==null) 
	    	previousValue = "";
	    	msgTextArea.setText(previousValue);    		
			
		final JDialog dialog;
		if (component instanceof JFrame)
			dialog=new JDialog((JFrame)component);
		else if  (component instanceof JDialog)
			dialog=new JDialog((JDialog)component);
		else
			dialog=new JDialog();
		//final JDialog dialog=new JDialog(component);
		dialog.setTitle(title);
		dialog.setSize(640,480);
		dialog.setLayout(new BorderLayout());
		dialog.setModal(true);
		dialog.setAlwaysOnTop(true);
		GuiUtils.centerOnScreen(dialog);
		dialog.setResizable(true);
		dialog.setAlwaysOnTop(true);
	
		dialog.add(jScrollPane,BorderLayout.CENTER);
		
		
		JPanel toolbar=new JPanel();
		toolbar.setLayout(new BorderLayout());
		
		JPanel buttons=new JPanel();
		buttons.setLayout(new FlowLayout());

		JButton ok=new JButton("OK");
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				canceled = false;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		buttons.add(ok);
		
		JButton cancel=new JButton("Cancel");
		cancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});	
		
		buttons.add(cancel);
		
		toolbar.add(buttons,BorderLayout.EAST);
		
		dialog.add(toolbar,BorderLayout.SOUTH);
		
		dialog.setVisible(true);
		if (canceled) {
		//	NeptusLog.pub().info("<###>CANCELED");
			return previousValue;
		}
		else {
		//	NeptusLog.pub().info("<###>NOT CANCELED: "+msgTextArea.getText());
			return msgTextArea.getText();
		}
	}
}
