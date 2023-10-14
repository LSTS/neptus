/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2010/05/28
 */
package pt.lsts.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class ConnectionSymbol extends SymbolLabel {

	/**
	 * Connection strength 
	 */
	public enum ConnectionStrengthEnum {LOW, MEDIAN, HIGH, FULL};
	
	private ConnectionStrengthEnum strength = ConnectionStrengthEnum.FULL;
	
	private boolean activeAnnounce = false;
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
	}
	
	/**
	 * @return the strength
	 */
	public ConnectionStrengthEnum getStrength() {
		return strength;
	}
	
	/**
	 * @param strength the strength to set
	 */
	public void setStrength(ConnectionStrengthEnum strength) {
		this.strength = strength;
		repaint();
	}
	
	/**
	 * 
	 */
	public void setFullStrength() {
		setStrength(ConnectionStrengthEnum.FULL);
	}

	/**
	 * @return
	 */
	public ConnectionStrengthEnum reduceStrength() {
		switch (strength) {
		case FULL:
			setStrength(ConnectionStrengthEnum.HIGH);
			break;
		case HIGH:
			setStrength(ConnectionStrengthEnum.MEDIAN);
			break;
		case MEDIAN:
			setStrength(ConnectionStrengthEnum.LOW);
			break;
		default:
			break;
		}
		return strength;
	}
	
	/**
     * @return the activeAnnounce
     */
    public boolean isActiveAnnounce() {
        return activeAnnounce;
    }
    
    /**
     * @param activeAnnounce the activeAnnounce to set
     */
    public void setActiveAnnounce(boolean activeAnnounce) {
        this.activeAnnounce = activeAnnounce;
    }
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Graphics2D g2 = (Graphics2D)g.create();

		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.scale(width/10.0, height/10.0);

		if (activeAnnounce) {
	        g2.setColor(getActiveColor());
	        GeneralPath sp = new GeneralPath();
            sp.moveTo(0, 8);
            sp.lineTo(0, 2);
            //sp.lineTo(5, 0);
            g2.draw(sp);
		}
		
		g2.translate(2, 5);
		
		Ellipse2D el1 = new Ellipse2D.Double(-1d, -1d, 2d, 2d);
		g2.setColor(getActiveColor());
		g2.fill(el1);
		
		if (isActive()) {
			Arc2D.Double arc1;
			if (strength.ordinal() >= ConnectionStrengthEnum.MEDIAN.ordinal()) {
				arc1 = new Arc2D.Double(0d, -3d/2d, 3d, 3d, -45d, 90, Arc2D.OPEN);
				g2.draw(arc1);
			}
			if (strength.ordinal() >= ConnectionStrengthEnum.HIGH.ordinal()) {
				arc1 = new Arc2D.Double(0d, -5d/2d, 5d, 5d, -45d, 90, Arc2D.OPEN);
				g2.draw(arc1);
			}
			if (strength.ordinal() >= ConnectionStrengthEnum.FULL.ordinal()) {
				arc1 = new Arc2D.Double(0d, -7d/2d, 7d, 7d, -45d, 90, Arc2D.OPEN);
				g2.draw(arc1);
			}
		}
		else {
			g2.translate(4, 0);
			GeneralPath sp = new GeneralPath();
			sp.moveTo(-3, -3);
			sp.lineTo(3, 3);
			sp.moveTo(3, -3);
			sp.lineTo(-3, 3);
			//g2.setColor(Color.WHITE);
			g2.draw(sp);
		}

		if (activeAnnounce) {
		    g2.setFont(new Font("Arial", Font.BOLD, 3));
            g2.drawString("A", -1, 4);
		}
	}
	
	public static void main(String[] args) {
		ConnectionSymbol symb1 = new ConnectionSymbol();
		symb1.setSize(50, 50);
		symb1.setActive(true);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.BLACK);
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);
		
		try {
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.setFullStrength();
            Thread.sleep(2000);
            symb1.setActive(false);
            Thread.sleep(2000);
            symb1.setActiveAnnounce(true);
            Thread.sleep(2000);
            symb1.setActive(true);
            symb1.setFullStrength();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
