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
 * $Id:: SimpleVerticalGauge.java 9615 2012-12-30 23:08:28Z pdias               $:
 */
package pt.up.fe.dceg.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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
