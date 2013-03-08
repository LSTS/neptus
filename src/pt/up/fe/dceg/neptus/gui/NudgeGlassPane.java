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
 * $Id:: NudgeGlassPane.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.util.ImageUtils;

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
