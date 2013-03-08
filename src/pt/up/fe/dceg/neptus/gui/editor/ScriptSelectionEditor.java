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
 * 24/Jun/2005
 * $Id:: ScriptSelectionEditor.java 9616 2012-12-30 23:23:22Z pdias       $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

/**
 * @author RJPG
 */
public class ScriptSelectionEditor extends AbstractPropertyEditor {

	private JButton button = new JButton("Script");
	Script script = new Script();

	public ScriptSelectionEditor() {
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(button, BorderLayout.CENTER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Script oldsrc = new Script();
				oldsrc.setSource(script.getSource());
				
				Script newsrc = Script.showScriptDialog("Script",script, editor/*ConfigFetch.getSuperParentFrame()*/);
				if (newsrc != null) {
					setValue(newsrc);
					firePropertyChange(oldsrc, newsrc);
				}
			}
		});
	}
	
	public Object getValue() {
		return script;
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof Script) {
			script=(Script)arg0;
		}
	}
	
	public static void main(String[] args) {
	}
}