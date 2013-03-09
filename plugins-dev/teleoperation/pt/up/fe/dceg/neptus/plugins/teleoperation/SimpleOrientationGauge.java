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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.text.NumberFormat;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class SimpleOrientationGauge extends JPanel {

    private static final long serialVersionUID = 1L;
    protected double angleRads = 0;
	protected double distance = 100;
	
	public double getAngleRads() {
		return angleRads;
	}


	public void setAngleRads(double angleRads) {
		this.angleRads = angleRads;
	}


	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	
	private NumberFormat nf = GuiUtils.getNeptusDecimalFormat(1);
	@Override
	public void paint(Graphics g) {
		double diameter = getWidth()-6;
		if (getHeight()-16 < getWidth()-6)
			diameter = getHeight()-16;
		Graphics2D g2 = (Graphics2D)g;
		g.setColor(Color.white);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.fill(new Ellipse2D.Double(3,3,diameter,diameter));
		g.setColor(Color.black);
		g2.draw(new Ellipse2D.Double(3,3,diameter,diameter));
		
		g.setColor(Color.black);
		String text = nf.format(distance)+"m";
		
		double width = g2.getFontMetrics(g2.getFont()).getStringBounds(text, g2).getWidth();
		System.out.println(width);
		g2.drawString(nf.format(distance)+"m", (int)((getWidth()/2)-width/2), getHeight()-10);
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 0.9);
		gp.lineTo(0.5, -0.5);
		gp.lineTo(-0.5, -0.5);
		gp.closePath();
		g2.translate(diameter/2+3, diameter/2+3);
		g2.scale(diameter*0.2, -diameter*0.2);
		g2.rotate(angleRads);
		g2.draw(gp);
		g2.scale(1/(diameter*0.2), 1/(diameter*0.2));
		g2.setColor(Color.gray);
		g2.fillOval(-4, -4, 8, 8);
	}
	
	public static void main(String[] args) {
		SimpleOrientationGauge gauge = new SimpleOrientationGauge();
		GuiUtils.testFrame(gauge);
		gauge.angleRads = Math.toRadians(45);
	}
}
