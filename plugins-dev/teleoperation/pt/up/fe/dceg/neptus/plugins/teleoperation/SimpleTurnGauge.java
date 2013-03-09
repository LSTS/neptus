/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2010/07/01
 */
package pt.up.fe.dceg.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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
