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
 * Author: Paulo Dias
 * 2010/05/28
 */
package pt.lsts.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
public class SystemTypeSymbol extends SymbolLabel {

	private String systemType = "";
	private boolean showSymbolOrText = true;
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		active = true;
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
		blinkOnChange = false;
	}
	
	/**
	 * @return the systemType
	 */
	public String getSystemType() {
		return systemType;
	}
	
	/**
	 * @param systemType the systemType to set
	 */
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}
	
	/**
	 * @return the showSymbolOrText
	 */
	public boolean isShowSymbolOrText() {
		return showSymbolOrText;
	}
	
	/**
	 * @param showSymbolOrText the showSymbolOrText to set
	 */
	public void setShowSymbolOrText(boolean showSymbolOrText) {
		this.showSymbolOrText = showSymbolOrText;
	}
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
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
			if ("UUV".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
					g2.translate(5, 5);
					GeneralPath sp = new GeneralPath();
					sp.moveTo(-3, 0);
					sp.lineTo(-1, -1);
					sp.lineTo(2.7, -1);
					sp.lineTo(2.7, 1);
					sp.lineTo(-1, 1);
					sp.closePath();
					g2.setColor(getActiveColor());
					g2.fill(sp);
					sp.moveTo(-4, -.5);
					sp.lineTo(-4, .5);
					g2.draw(sp);
	
					sp.reset();
					sp.moveTo(-2.5, -1);
					sp.lineTo(-2.5, 1);
					g2.draw(sp);
	
					Ellipse2D.Double el = new Ellipse2D.Double(-1d, -1d, 2d, 2d);
					g2.translate(3, 0);
					g2.draw(el);
					g2.fill(el);
				}
				else
					drawText(g2, "UUV");
			}
			else if ("UAV".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
//					g2.translate(5, 2.7);
					GeneralPath sp = new GeneralPath();
//					sp.moveTo(0, 0);
//					sp.lineTo(-3, 4);
//					sp.lineTo(0, 2.8);
//					sp.lineTo(3, 4);
//					sp.closePath();
					
					g2.translate(5, 5);
					g2.scale(0.5, 0.5);
			        int width1 = 20, height1 = width1 / 2;
//			        double sc = width1 / 20.0;
			        sp.moveTo(width1 / 4, height1 / 3);

			        // plane's nose
			        sp.lineTo(width1 / 4, height1 / 3);
			        sp.curveTo(width1 / 2 + 1, height1 / 3, width1 / 2 + 1, -height1 / 8, width1 / 4, -height1 / 8);
			        // plane's wing
			        sp.lineTo(width1 / 4 - 2, -height1 / 8);
			        sp.lineTo(width1 / 4 - 6, -height1 / 8 - 6);
			        sp.lineTo(width1 / 4 - 8, -height1 / 8 - 6);
			        sp.lineTo(width1 / 4 - 8, -height1 / 8);
			        // plane's tail
			        sp.lineTo(-width1 / 4 - 1, -height1 / 8);
			        sp.lineTo(-width1 * 5 / 12, -height1 / 2);
			        sp.lineTo(-width1 / 2, -height1 / 2);
			        sp.lineTo(-width1 / 2, 0);
			        sp.lineTo(-width1 / 4, height1 / 3);
			        sp.closePath();
					
					g2.setColor(getActiveColor());
					g2.fill(sp);
					g2.draw(sp);
				}
				else
					drawText(g2, "UAV");
			}
			else if ("UGV".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
					g2.translate(5, 5);
					GeneralPath sp = new GeneralPath();
					sp.moveTo(-3.8, -.3);
					sp.lineTo(3.8, -.3);
					sp.lineTo(3.8, 1);
					sp.lineTo(-3.8, 1);
					sp.closePath();
					g2.setColor(getActiveColor());
					g2.fill(sp);
					g2.draw(sp);

					sp.reset();
					sp.moveTo(-2, -3);
					sp.lineTo(1, -3);
					sp.lineTo(2.1, 0);
					sp.lineTo(-2.7, 0);
					sp.closePath();
					g2.draw(sp);

					Ellipse2D.Double el = new Ellipse2D.Double(-.5d, -.5d, 1d, 1d);
					g2.translate(2.5, 2);
					g2.draw(el);
					g2.fill(el);
					g2.translate(-5, 0);
					g2.draw(el);
					g2.fill(el);
				}
				else
					drawText(g2, "UGV");
			}
			else if ("USV".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
					g2.translate(5, 5);
					GeneralPath sp = new GeneralPath();
					sp.moveTo(-3, -.1);
					sp.lineTo(3.2, -.1);
					sp.lineTo(2, 1);
					sp.lineTo(-3, 1);
					sp.closePath();
					g2.setColor(getActiveColor());
					g2.fill(sp);
					sp.moveTo(-4, .5);
					sp.lineTo(-4, 1.5);
					g2.draw(sp);

					sp.reset();
					sp.moveTo(-2.7, -3);
					sp.lineTo(-1, -3);
					sp.lineTo(-1, 0);
					sp.lineTo(-2.7, 0);
					sp.closePath();
					g2.draw(sp);
				}
				else
					drawText(g2, "USV");
			}
			else if ("CCU".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
					GeneralPath sp = new GeneralPath();
					sp.moveTo(1, 5);
					sp.lineTo(1, 1);
					sp.lineTo(6, 1);
					sp.lineTo(6, 5);
					sp.lineTo(1, 5);
					sp.lineTo(6, 5);
					sp.lineTo(8, 8);
					sp.lineTo(3, 8);
					sp.closePath();
					g2.setColor(getActiveColor());
					g2.draw(sp);
	
					Ellipse2D.Double el = new Ellipse2D.Double(-.1d, -.1d, .2d, .2d);
					g2.translate(2.3, 5.7);
					for(int i = 0; i < 6; i++) {
						for (int j = 0; j < 12; j++) {
							g2.fill(el);
							g2.translate(.3, 0);
						}
						g2.translate(-.3*12+.2, .3);
					}
				}
				else
					drawText(g2, "CCU");
			}
			else if ("STATICSENSOR".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
					drawText(g2, "SS");
				}
				else
					drawText(g2, "SS");
			}
			else if ("MOBILESENSOR".equalsIgnoreCase(systemType)) {
				if (showSymbolOrText) {
					drawText(g2, "MS");
				}
				else
					drawText(g2, "MS");
			}
			else {
				drawText(g2, "?");
			}
		}
	}
	
//	/**
//	 * @param g2
//	 * @param string
//	 */
//	private void drawText(Graphics2D g2, String text) {
//		g2.setColor(getActiveColor());
//		g2.setFont(new Font("Arial", Font.BOLD, 10));
//		String tt = text;
//		Rectangle2D sB1 = g2.getFontMetrics().getStringBounds(tt, g2);
//		double sw0 = 10.0 / sB1.getWidth();
//		double sh0 = 10.0 / sB1.getHeight();
//		g2.translate(5, 5);
//		g2.scale(sw0, sh0);			
//		g2.drawString(text, (int) (-sB1.getWidth()/2.0), (int) (sB1.getHeight()/2.0));
//	}

	public static void main(String[] args) {
		SystemTypeSymbol symb1 = new SystemTypeSymbol();
		symb1.setActive(true);
		symb1.setSize(50, 50);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.BLACK);
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);
		
//		try {Thread.sleep(5000);} catch (Exception e) {}
//		symb1.blink(true);
//		try {Thread.sleep(5000);} catch (Exception e) {}
//		symb1.blink(false);
//
//        try {Thread.sleep(5000);} catch (Exception e) {}
//        symb1.setSystemType("UAV");
//        symb1.repaint();
//        try {Thread.sleep(5000);} catch (Exception e) {}
//        symb1.setSystemType("UUV");
//        symb1.repaint();
//        try {Thread.sleep(5000);} catch (Exception e) {}
//        symb1.setSystemType("USV");
//        symb1.repaint();
//        try {Thread.sleep(5000);} catch (Exception e) {}
//        symb1.setSystemType("UGV");
//        symb1.repaint();
//        try {Thread.sleep(5000);} catch (Exception e) {}
//        symb1.setSystemType("CCU");
//        symb1.repaint();

	}

}
