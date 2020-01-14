/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.PinstripePainter;

import pt.lsts.neptus.util.ImageUtils;

@SuppressWarnings("serial")
public class NudgeGlassPane extends JPanel {

	private Color backColor = new Color(255, 100, 100, 75);
	private Color foreColor = new Color(100, 25, 0);
	private static ImageIcon icon = ImageUtils.getIcon("images/critical-transparent.png");
	
	private NudgeGlassPane(String messageToShow) {
		setLayout(new BorderLayout());
		setOpaque(false);		
		JXLabel msgLabel = new JXLabel(messageToShow, icon, JLabel.CENTER);
		msgLabel.setBackgroundPainter(new PinstripePainter(new Color(100,50,0,128)));
		msgLabel.setFont(new Font("Arial", Font.PLAIN, 20));
		msgLabel.setForeground(foreColor);
		msgLabel.setBackground(backColor);
		msgLabel.setOpaque(true);	
		
		add(msgLabel);
		setVisible(true);
	}
	
	public static void nudge(JRootPane rootpane, String message, int delay) {
		
		NudgeGlassPane nudgePane = new NudgeGlassPane(message);
		final JRootPane root = rootpane;
		
		root.setGlassPane(nudgePane);
		nudgePane.setVisible(true);
		nudgePane.revalidate();		
		
		final Timer nudgeTimer = new Timer("Nudge Timer");
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
			    try { // making this run in the AWT event dispatching thread
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            root.setGlassPane(new JRootPane().getGlassPane());
                            root.repaint();
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
				nudgeTimer.cancel();
			}
		};
		nudgeTimer.schedule(tt, delay*1000);
	}
}
