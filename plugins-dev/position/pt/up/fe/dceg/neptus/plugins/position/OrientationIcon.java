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
 * 2009/06/10
 */
package pt.up.fe.dceg.neptus.plugins.position;

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

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class OrientationIcon implements Icon {

	private double diam, margin;
	private double angleRadians = Double.NaN;
	
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
		g.setColor(Color.white);
		g.fill(circle);
		g.setColor(Color.black);
		g.draw(circle);
		if (angleRadians != Double.NaN) {
			g.rotate(angleRadians);
			g.scale(diam/100,diam/100);
			g.fill(arrow);
		}
		g.setTransform(t);
	}
	
	public void setAngleRadians(double angleRadians) {
		this.angleRadians = angleRadians;		
	}
	
	public void setAngleDegs(double angleRadians) {
		this.angleRadians = Math.toRadians(angleRadians);		
	}

	
	public static void main(String[] args) {
		final OrientationIcon ico = new OrientationIcon(22, 2);
		final DisplayPanel lbl = new DisplayPanel("lauv-seacon-1");
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
