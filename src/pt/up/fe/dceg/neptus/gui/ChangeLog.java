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
 * 29/Dez/2004
 * $Id:: ChangeLog.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.util.GuiUtils;
/**
 * This class shows this application's changelog. The changelog file is downloaded over 
 * the internet and shown in the form of a HTML page.
 * @author Zé Carlos
 */
public class ChangeLog extends JFrame{

	static final long serialVersionUID = 5;

	/**
	 * Class constructor - Creates a frame with the change log
	 */
	public ChangeLog() {
		this.setTitle("Mission Planner change log");
		JComponent component;
		try {
			JEditorPane browser = new JEditorPane("http://whale.fe.up.pt/neptusapps/mplanner_log.html");
			browser.setEditable(false);
			component = new JScrollPane(browser);
		} catch (Exception e) {
			component =  new JLabel("Unable to download the changelog file", JLabel.CENTER);
		}
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(component, BorderLayout.CENTER);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setSize(400, 500);
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource("/images/neptus-icon.png")));
		GuiUtils.centerOnScreen(this);
	}
	
	/**
	 * Unitary text - loads the changelog and shows it
	 * @param args
	 */
	public static void main(String args[]) {
		ChangeLog cl = new ChangeLog();
		cl.setVisible(true);
	}
	
}
