/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.fileeditor.SyntaxDocument;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * 
 * @author RJPG
 *
 */
public class Script {
	
	public String source;
	public static boolean flag_exit;	
	
	public Script()
	{
		
	}
	
	public Script(String scr)
	{
		source=scr;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public static Script showScriptDialog(String title, Script previousScript, Component component) {
	    return showLocationDialog(title, previousScript,true, component);
	}

	
	public static Script showLocationDialog(String title, Script previousScript, boolean editable, Component component) 
	{
		
		JEditorPane msgTextArea = null;
		//JButton resetMsgsPanelButton=null; 
		JScrollPane jScrollPane=null;
		jScrollPane = new JScrollPane();
	

	
	    msgTextArea=SyntaxDocument.getJavaScriptEditorPane();
	    msgTextArea.setEditable(editable);
	    jScrollPane.setViewportView(msgTextArea);
	    jScrollPane.setVisible(true);
	 
	    if(previousScript.getSource()=="" || previousScript.getSource()==null)
	    {
	    	
	    	msgTextArea.setText("/*********************************************************\n" +
	    			            "  To use variables from the system state just reference them \n" +
	    					    "  using \"#USE [VARIABLE]\".\n" +
	    					    "\n" +
	    			            "  To use environment persistent variables just reference them \n" +
	    					    "  using \"#ENV [VARIABLE]\".\n" +
	    					   	"**********************************************************/\n\n" +
	    					   	"\n msg.setMsg('0 is green in the alarms leds'); //return message\n" +
	    					   	"\n 0;   // return value and exit");
	    }
	    else
	    	msgTextArea.setText(previousScript.getSource());    
		
			
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
		
		setFlag_exit(false);

		JButton help=new JButton("Help");
		//help.setEnabled(false);
		help.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				GuiUtils.htmlMessage(dialog, "Script Editor", "How to use scripts.", 
						
						"<html><h1>Script Editor</h1><br>"+
						"<h2>Environment variables</h2><blockquote>You can use <em>'#ENV variableName'</em> to declare a presistent varable. Or simply use in the code <em>'env.initEnv('variableName')'</em>.</blockquote>"+
						"<h2>Variable Tree variables</h2><blockquote>You can use <em>'#USE variableName'</em> to declare them.</blockquote>"+
						""
						);
				
			}
			
			
		});
		buttons.add(help);
		help.setEnabled(false);
		
		JButton ok=new JButton("OK");
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				setFlag_exit(false);
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		buttons.add(ok);
		
		JButton cancel=new JButton("Cancel");
		cancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				setFlag_exit(true);
				dialog.setVisible(false);
				dialog.dispose();
			}
		});	
		
		buttons.add(cancel);
		
		toolbar.add(buttons,BorderLayout.EAST);
		
		dialog.add(toolbar,BorderLayout.SOUTH);
		
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setFlag_exit(true);
				/*text = previousScript.getSource();
				setUserCancel(true);
				dialog.setVisible(false);
				dialog.dispose();*/
			}
		});
		
		
		dialog.setVisible(true);
		if (isFlag_exit()==true)
			return previousScript;
		else
			return new Script(msgTextArea.getText());
	}
	
	public String toString()
	{
		return source; 
	}

	public static boolean isFlag_exit() {
		return flag_exit;
	}

	public static void setFlag_exit(boolean flag_exit2) {
		flag_exit = flag_exit2;
	}
	
	public static void main(String[] args) {
		showScriptDialog("", new Script(), (Component)null);
	}
}
