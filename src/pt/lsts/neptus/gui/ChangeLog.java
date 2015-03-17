/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 29/Dez/2004
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import pt.lsts.neptus.util.GuiUtils;
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
