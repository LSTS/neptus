/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/06/11
 * $Id:: SimpleAction.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.plugins.actions;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * This class eases the creation of console buttons
 * @author ZP
 */
public abstract class SimpleAction extends SimpleSubPanel implements ActionListener {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected AbstractButton btn = null;
	
	/**
	 * Default is <b>false</b>. Override and return <b>true</b> for a toggle button. 
	 * @return Whether to use a toggle button.
	 */
	protected boolean isSwitch(){
		return false;
	}
	
	/**
	 * Defaults to <b>false</b> which shows a mini-button (icon only). <br>
	 * Override if a normal button is preferred.
	 * @return Whether button text is visible to the user or not
	 */
	protected boolean showText() {
		return false;
	}
	
	/**
	 * Retrieves the current state of the associated button.
	 * @return Current state of the associated button.
	 */
	public boolean isSelected() {
		return btn.isSelected();
	}
	
	public SimpleAction(ConsoleLayout console) {
	    super(console);
		if (isSwitch())
			btn = new JToggleButton(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 20,20));
		else
			btn = new JButton(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 20,20));
		
		btn.setMargin(new Insets(0,0,0,0));
		btn.setToolTipText(PluginUtils.getPluginDescription(getClass()));
		btn.setFocusPainted(false);
		btn.addActionListener(this);		
		if (showText())
			btn.setText(getName());
		setLayout(new BorderLayout());
		add(btn, BorderLayout.CENTER);		
		setSize(getPreferredSize().width+8,getPreferredSize().height+8);
	}	
}
