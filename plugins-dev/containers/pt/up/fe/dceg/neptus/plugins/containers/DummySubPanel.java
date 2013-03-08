/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 4 de Ago de 2010
 * $Id:: DummySubPanel.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.plugins.containers;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

/**
 * @author zepinto
 *
 */
@PluginDescription(name="Dummy sub Panel")
public class DummySubPanel extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;
    protected JTextArea text = new JTextArea();
	
	public DummySubPanel(ConsoleLayout console) {
	    super(console);
		removeAll();
		setLayout(new BorderLayout());
		add(new JScrollPane(text), BorderLayout.CENTER);
	}
	
	@Override
	public void initSubPanel() {
		text.setText(text.getText()+System.nanoTime()+" initSubPanel() called from "+ReflectionUtil.getCallerStamp()+"\n");
	}
	
	
	@Override
	public void cleanSubPanel() {
		text.setText(text.getText()+System.nanoTime()+" clean() called from "+ReflectionUtil.getCallerStamp()+"\n");		
	}
	
}
