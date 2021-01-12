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
 * 2010/07/01
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class SimpleVerticalGauge extends JPanel {

    private static final long serialVersionUID = 1L;
    protected double value = 0;
	protected ColorMap colormap =  ColorMapFactory.createInvertedColorMap((InterpolationColorMap)ColorMapFactory.createRedYellowGreenColorMap());
	protected Color bright = new Color(255,255,255,100);
	protected Color dark = new Color(0,0,0,100);
	
	
	public SimpleVerticalGauge() {
		setMinimumSize(new Dimension(25, 25));
		setPreferredSize(new Dimension(25, 25));	
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public ColorMap getColormap() {
		return colormap;
	}

	public void setColormap(ColorMap colormap) {
		this.colormap = colormap;
	}
	
	@Override
	public void paint(Graphics g) {
		Color c = colormap.getColor(value);
		((Graphics2D)g).setPaint(new GradientPaint(0, 0, c.brighter() , getWidth(), 0, c.darker()));
		int height = (int)(value*getHeight());
		g.fillRect(1, getHeight()-height, getWidth()-1, getHeight());
		g.setColor(Color.black);
		g.drawRect(0, 0, getWidth()-1, getHeight()-1);
		g.setColor(new Color(0,0,0,128));
		g.drawLine(1, getHeight()-height, getWidth()-2, getHeight()-height);		
	}
	
	public static void main(String[] args) {
		SimpleVerticalGauge gauge = new SimpleVerticalGauge();
		gauge.setValue(0.56);
		GuiUtils.testFrame(gauge);
		
	}

	
}
