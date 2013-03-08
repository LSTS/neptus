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
 * $Id:: CloseTabbedPane.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.gui;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JTabbedPane;


public class CloseTabbedPane extends JTabbedPane{
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private TabCloseUI closeUI;
	public CloseTabbedPane(){			
		closeUI = new TabCloseUI();
		closeUI.setTabbedPane(this);
		addMouseMotionListener(closeUI);
		addMouseListener(closeUI);
	}

	public void paint(Graphics g){
		super.paint(g);
		closeUI.paint(g);
	}
	
	public void addTab(String title, Component component) {
		super.addTab(title+"   ", component);
	}
	
	public void addTab(String title, Icon icon, Component component) {
		super.addTab(title+"   ", icon, component);
	}	
	
	public Dimension getMinimumSize() {
		return new Dimension(100,100);
	}
}
