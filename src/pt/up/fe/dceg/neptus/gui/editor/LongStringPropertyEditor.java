/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 25, 2010
 */
package pt.up.fe.dceg.neptus.gui.editor;

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

import pt.up.fe.dceg.neptus.fileeditor.SyntaxDocument;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.StringProperty;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

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
				//System.out.println("value:\n"+value);
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
		//	System.out.println("CANCELED");
			return previousValue;
		}
		else {
		//	System.out.println("NOT CANCELED: "+msgTextArea.getText());
			return msgTextArea.getText();
		}
	}
}
