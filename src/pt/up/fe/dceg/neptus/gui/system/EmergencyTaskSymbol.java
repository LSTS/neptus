/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 8 de Dez de 2010
 * $Id:: EmergencyTaskSymbol.java 9615 2012-12-30 23:08:28Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class EmergencyTaskSymbol extends TaskSymbol {

	public enum EmergencyStatus {NOT_CONFIGURED, DISABLED, ENABLED, ARMED, ACTIVE, STOPPING};
	
	private EmergencyStatus status = EmergencyStatus.NOT_CONFIGURED;

	/**
	 * @return the status
	 */
	public EmergencyStatus getStatus() {
		return status;
	}
	
	/**
	 * @param status the status to set
	 */
	public void setStatus(EmergencyStatus status) {
		boolean changeValue = (this.status != status);
		this.status = status;
		if (blinkOnChange && changeValue)
			blink(true);
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.TaskSymbol#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Graphics2D g2 = (Graphics2D)g.create();
		super.paint(g, c, width, height);
		if (isActive()) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.scale(width/10.0, height/10.0);
			g2.setColor(getActiveColor());
			g2.setFont(new Font("Arial", Font.BOLD, 5));
			g2.drawString("E", 0, 4);
		
			g2.setFont(new Font("Arial", Font.BOLD, 4));
			String st = "NC";
			switch (status) {
			case ENABLED:
				st = "EN";
				break;
			case DISABLED:
				st = "NO";
				break;
			case ACTIVE:
				st = "AC";
				break;
			case ARMED:
				st = "AR";
				break;
			case STOPPING:
				st = "ST";
				break;
			default:
				st = "NC";
			}
			g2.drawString(st, 4, 10);
		}
	}
	
	public static void main(String[] args) {
		EmergencyTaskSymbol symb1 = new EmergencyTaskSymbol();
		symb1.setSize(50, 50);
		symb1.setActive(true);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.GRAY.darker());
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);

	}
}
