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
 * $Id:: MainVehicleSymbol.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class MainVehicleSymbol extends SymbolLabel {

	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
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
			g2.setColor(getActiveColor());
			g2.setFont(new Font("Arial", Font.BOLD, 10));
			String tt = "M";
			Rectangle2D sB1 = g2.getFontMetrics().getStringBounds(tt, g2);
//			double scale;
			double sw0 = 10.0 / sB1.getWidth();
			double sh0 = 10.0 / sB1.getHeight();
//			scale = (sw0 < sh0)?sw0:sh0;
			g2.translate(5, 5);
			g2.scale(sw0, sh0);			
			g2.drawString("M", (int) (-sB1.getWidth()/2.0), (int) (sB1.getHeight()/2.0));
		}
	}
	
	public static void main(String[] args) {
		MainVehicleSymbol symb1 = new MainVehicleSymbol();
		symb1.setActive(true);
		symb1.setSize(50, 50);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.BLACK);
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);
		
		try {Thread.sleep(5000);} catch (Exception e) {}
		symb1.blink(true);
		try {Thread.sleep(5000);} catch (Exception e) {}
		symb1.blink(false);

	}

}
