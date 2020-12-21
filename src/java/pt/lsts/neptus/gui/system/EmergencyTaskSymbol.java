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
 * Author: Paulo Dias
 * 8 de Dez de 2010
 */
package pt.lsts.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.swingx.JXPanel;

import pt.lsts.neptus.util.GuiUtils;

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
	 * @see pt.lsts.neptus.gui.system.TaskSymbol#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
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
