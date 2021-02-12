/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/06/10
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Icon;
import javax.swing.JLabel;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class OrientationIcon implements Icon {

	private double diam, margin;
	private double angleRadians = Double.NaN;

    private Color backgroundColor = Color.white;
    private Color foregroundColor = Color.black;

	private GeneralPath arrow = new GeneralPath();
	
	{
		arrow.moveTo(0, -47);
		arrow.lineTo(30, 37);
		arrow.lineTo(0, 15);
		arrow.lineTo(-30, 37);
		arrow.closePath();
	}
	public OrientationIcon(int diameter, int margin) {
		this.diam = diameter;
		this.margin = margin;
	}
	
	@Override
	public int getIconHeight() {
		return (int)(diam + margin * 2);
	}

	public int getIconWidth() {
		return getIconHeight();
	}

	@Override
	public void paintIcon(Component c, Graphics arg0, int x, int y) {
		Ellipse2D circle = new Ellipse2D.Double(-diam/2,-diam/2,diam,diam);
		Graphics2D g = (Graphics2D) arg0;
		AffineTransform t = g.getTransform();
		g.translate(x, y);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.translate(margin+diam/2,margin+diam/2);
        g.setColor(backgroundColor);
		g.fill(circle);
        g.setColor(foregroundColor);
		g.draw(circle);
		if (!Double.isNaN(angleRadians)) {
			g.rotate(angleRadians);
			g.scale(diam/100,diam/100);
			g.fill(arrow);
		}
		g.setTransform(t);
	}
	
	/**
     * @return the backgroundColor
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    /**
     * @return the foregroundColor
     */
    public Color getForegroundColor() {
        return foregroundColor;
    }
    
    /**
     * @param foregroundColor the foregroundColor to set
     */
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }
	
	public void setAngleRadians(double angleRadians) {
		this.angleRadians = angleRadians;		
	}
	
	public void setAngleDegs(double angleRadians) {
		this.angleRadians = Math.toRadians(angleRadians);		
	}

	
	public static void main(String[] args) {
		final OrientationIcon ico = new OrientationIcon(22, 2);
		final JLabel lbl = new JLabel("lauv-seacon-1");
		lbl.setText("845 m ");
		lbl.setIcon(ico);
		lbl.setVerticalAlignment(JLabel.CENTER);
		lbl.setHorizontalTextPosition(JLabel.LEFT);
		lbl.setVerticalTextPosition(JLabel.CENTER);
		GuiUtils.testFrame(lbl);
		
		TimerTask tt = new TimerTask() {
			int count = 1;
			@Override
			public void run() {
				ico.setAngleDegs(count%360);
				count++;
				lbl.repaint();
			}
		};
		Timer t = new Timer("Orientation Icon");
		t.schedule(tt, 10, 10);
	}
}
