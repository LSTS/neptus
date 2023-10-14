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
 * Author: José Pinto
 * 2010/07/01
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class SimpleTurnGauge extends JPanel {

    private static final long serialVersionUID = 1L;
    protected double value = 0;
	protected ColorMap colormap =  ColorMapFactory.createInvertedColorMap((InterpolationColorMap)ColorMapFactory.createRedYellowGreenColorMap());
	protected double ballRadius = 7;
	public SimpleTurnGauge() {
		setBackground(Color.white);
		setMinimumSize(new Dimension(25, 25));
		setPreferredSize(new Dimension(25, 25));	
	}
	
	public ColorMap getColormap() {
		return colormap;
	}

	public void setColormap(ColorMap colormap) {
		this.colormap = colormap;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Color c = colormap.getColor(Math.abs(value));
		Graphics2D g2 = (Graphics2D) g;
		double xCenter = getWidth()/2 + (getWidth()/2 - ballRadius)*value;
		g2.setColor(c);
		Ellipse2D ellis = new Ellipse2D.Double(xCenter-ballRadius, getHeight()/2-ballRadius, ballRadius*2, ballRadius*2);
		g2.fill(ellis);
		g2.setColor(Color.black);
		g2.draw(ellis);
		
	}
	
	public static void main(String[] args) throws Exception {
		SimpleTurnGauge gauge = new SimpleTurnGauge();
		
		GuiUtils.testFrame(gauge);
		
		for (double i = -1; i < 1; i+=0.01) {
			gauge.setValue(i);
			gauge.repaint();
			Thread.sleep(50);
		}
	}
	
	
}
