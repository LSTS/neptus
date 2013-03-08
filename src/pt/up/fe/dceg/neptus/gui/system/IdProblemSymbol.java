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
 * 2010/05/28
 * $Id:: IdProblemSymbol.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class IdProblemSymbol extends SymbolLabel {

	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		active = true;
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
		blinkOnChange = false;
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.scale(width/10.0, height/10.0);
		
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);
		
		if (isActive()) {
			drawText((Graphics2D) g2.create(), "2 ID");

//			g2.translate(5, 5);
//			GeneralPath sp = new GeneralPath();
//			sp.moveTo(-4, 4);
//			sp.lineTo(4, -4);
//			g2.setColor(getActiveColor());
//			g2.draw(sp);
//			sp.reset();
//			sp.moveTo(-4, -4);
//			sp.lineTo(4, 4);
//			g2.setColor(getActiveColor());
//			g2.draw(sp);

			g2.translate(5, 1.5);
			g2.scale(0.27, 0.27);

			GeneralPath sp = new GeneralPath();

			sp.moveTo(1.45, 3);
			sp.lineTo(4, 3);
			sp.lineTo(0, -3.5);
			sp.lineTo(-4, 3);
			sp.lineTo(-1.45, 3);
			g2.setColor(getActiveColor());
			g2.draw(sp);

			sp.reset();
			sp.moveTo(0, .5);
			sp.lineTo(0, 2.35);
			g2.draw(sp);
			Ellipse2D.Double el = new Ellipse2D.Double(-.5d, -.5d, 1d, 1d);
			g2.translate(0, 0);
			g2.fill(el);
			g2.translate(0, 2.85);
			g2.fill(el);
			g2.translate(0, 1.45);
			g2.fill(el);

		}
	}
	
	public static void main(String[] args) {
		IdProblemSymbol symb1 = new IdProblemSymbol();
		symb1.setActive(true);
		symb1.setSize(50, 50);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.GRAY.darker());
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);
		
		try {Thread.sleep(5000);} catch (Exception e) {}
		symb1.blink(true);
		try {Thread.sleep(5000);} catch (Exception e) {}
		symb1.blink(false);

	}

}
