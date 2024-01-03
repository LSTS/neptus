/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui.editor;

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

import pt.lsts.neptus.fileeditor.SyntaxDocument;
import pt.lsts.neptus.util.GuiUtils;

/**
 * 
 * @author RJPG
 *
 */
public class Script {

    public String source;
    public static boolean flag_exit;

    public Script() {

    }

    public Script(String scr) {
        source = scr;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static Script showScriptDialog(String title, Script previousScript, Component component) {
        return showLocationDialog(title, previousScript, true, component);
    }
	
	public static Script showLocationDialog(String title, Script previousScript, boolean editable, Component component) {
		
		JEditorPane msgTextArea = null;
		//JButton resetMsgsPanelButton=null; 
		JScrollPane jScrollPane=null;
		jScrollPane = new JScrollPane();
	

	
	    msgTextArea=SyntaxDocument.getJavaScriptEditorPane();
	    msgTextArea.setEditable(editable);
	    jScrollPane.setViewportView(msgTextArea);
	    jScrollPane.setVisible(true);
	 
	    if("".equals(previousScript.getSource()) || previousScript.getSource() == null) {
	    	
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
	
    public String toString() {
        return source;
    }

    public static boolean isFlag_exit() {
        return flag_exit;
    }

    public static void setFlag_exit(boolean flag_exit2) {
        flag_exit = flag_exit2;
    }

    public static void main(String[] args) {
        showScriptDialog("", new Script(), (Component) null);
    }
}
