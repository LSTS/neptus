/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 200?/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.util.GuiUtils;

/**
 * 
 * @author Zé Carlos
 * @author pdias Fix Resize scaling
 */
@SuppressWarnings("serial")
public class AltitudePanel extends JPanel implements PropertiesProvider {

	private Color backColor = Color.black, textColor = Color.white,
			thousandsPointerColor = Color.red,
			hundredsPointerColor = Color.yellow;
	private int margin = 3;
	
	private String label = "";
	
	private int curValue = 0;
	
	public DefaultProperty[] getProperties() {
		return null;
	}
	
	public String getPropertiesDialogTitle() {		
		return "Altitude Panel Properties";
	}
	
	public String[] getPropertiesErrors(Property[] properties) {	
		return null;
	}
	
	public void setProperties(Property[] properties) {
	}
	
	
	Ellipse2D centerTh = new Ellipse2D.Double(-3, -3, 6, 6);
	Ellipse2D centerHund = new Ellipse2D.Double(-2, -2, 4, 4);
	
	@Override
	public void paint(Graphics g) {		
		super.paint(g);
		
		//pdias Fixed the FontSize
		
		int w = getWidth(), h = getHeight();
		
		double diameter = (w < h)? w : h;
		
		double scale = diameter / 120;
		w = (int) (w * 1/scale);
		h = (int) (h * 1/scale);
		
		
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		AffineTransform pre = g2d.getTransform();
		g2d.scale(scale, scale);
		diameter = (w < h)? w : h;
		double hMargin = (w-diameter)/2+margin;
		double vMargin = (h-diameter)/2+margin;
		
		diameter = diameter - margin*2;
		Ellipse2D ellis = new Ellipse2D.Double(hMargin, vMargin, diameter, diameter);
		
		g2d.setColor(backColor);
		g2d.fill(ellis);
		g2d.setColor(textColor);
		g2d.draw(ellis);
		
		double stepRotation = (Math.PI*2)/10;
		g2d.translate(w/2, h/2);	
		
//		String meters = ""+curValue;
//		Rectangle2D sBounds = g2d.getFontMetrics().getStringBounds(meters, g2d);		
//		g.drawString(meters, (int) -sBounds.getWidth()/2, 20);
//
//		if (getLabel() != null && !"".equalsIgnoreCase(getLabel())) {
//			Rectangle2D lBounds = g2d.getFontMetrics().getStringBounds(getLabel(), g2d);		
//			g.drawString(getLabel(), (int) -lBounds.getWidth()/2, -12);
//		}

		int textMargin = 12;
		Line2D stepLine = new Line2D.Double(0, (-diameter/2)+margin+textMargin, 0, (-diameter/2)+margin+textMargin+8);
		
		for (int i = 0; i < 10; i++) {
			g2d.draw(stepLine);
			
			Rectangle2D sB = g2d.getFontMetrics().getStringBounds(""+i, g2d);
			
			g2d.translate(0, (-diameter/2)+7);			
				g2d.rotate(-stepRotation*i);
					g2d.drawString(""+i, (int) -sB.getWidth()/2, 4);
				g2d.rotate(stepRotation*i);
			g2d.translate(0, (diameter/2)-7);
			
			g2d.rotate(stepRotation);		
		}
		
		Line2D substepLine = new Line2D.Double(0, (-diameter/2)+margin+textMargin, 0, (-diameter/2)+margin+textMargin+4);
		
		g2d.rotate(stepRotation/2);
		for (int i = 0; i < 10; i++) {
			g2d.draw(substepLine);
			g2d.rotate(stepRotation);
		}
		g2d.rotate(-stepRotation/2);
		
		if (curValue < 0)
			curValue = 0;
		
		while (curValue > 10000)
			curValue -= 10000;
				
		double thRotation = (curValue / 10000.0) * Math.PI*2;
		
		g2d.rotate(thRotation);
		g2d.setColor(thousandsPointerColor);
		double thHeight = diameter/2 - margin - textMargin;
		
		Polygon p = new Polygon(new int[] {-3,3,0}, new int[] {0,0,-(int)thHeight}, 3);
		g2d.fill(p);
		g2d.fill(centerTh);		
		g2d.rotate(-thRotation);
		
		double hundRotation = curValue % 1000;
		hundRotation = (hundRotation / 1000) * Math.PI*2;
		
		g2d.setColor(hundredsPointerColor);		
		g2d.rotate(hundRotation);
		double hundHeight = (diameter/2 - margin - textMargin)/1.3;
		
		p = new Polygon(new int[] {-2,2,0}, new int[] {0,0,-(int)hundHeight}, 3);
		g2d.fill(p);
		g2d.fill(centerHund);		
		g2d.rotate(-hundRotation);
		
		
		g2d.setColor(backColor);
		Ellipse2D ellisC = new Ellipse2D.Double(-1, -1, 2, 2);
		g2d.fill(ellisC);
				
		g2d.setColor(textColor);
		String meters = ""+curValue;
		Rectangle2D sBounds = g2d.getFontMetrics().getStringBounds(meters, g2d);		
		g.drawString(meters, (int) -sBounds.getWidth()/2, 20);

		if (getLabel() != null && !"".equalsIgnoreCase(getLabel())) {
			Rectangle2D lBounds = g2d.getFontMetrics().getStringBounds(getLabel(), g2d);		
			g.drawString(getLabel(), (int) -lBounds.getWidth()/2, -12);
		}

		g2d.setTransform(pre);
	}
	
	public Color getBackColor() {
		return backColor;
	}

	public void setBackColor(Color backColor) {
		this.backColor = backColor;
	}

	public Color getHundredsPointerColor() {
		return hundredsPointerColor;
	}

	public void setHundredsPointerColor(Color hundredsPointerColor) {
		this.hundredsPointerColor = hundredsPointerColor;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Color getThousandsPointerColor() {
		return thousandsPointerColor;
	}

	public void setThousandsPointerColor(Color thousandsPointerColor) {
		this.thousandsPointerColor = thousandsPointerColor;
	}

	public int getCurValue() {
		return curValue;
	}

	public void setCurValue(int curValue) {		
		int oldValue = this.curValue;
		if (curValue != oldValue) {
			this.curValue = curValue;
			repaint();
		}
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
		repaint();
	}
	
	public static void main(String[] args) {
		final AltitudePanel ap = new AltitudePanel();
		ap.setLabel("m");
		//ap.setPreferredSize(new Dimension(100,100));
		GuiUtils.testFrame(ap, "Testing the altitude panel");
		TimerTask tt = new TimerTask() {
			
			int val = 0;
			
			@Override
			public void run() {
				val += 20;
				ap.setCurValue(val);			
			}
		};
		
		Timer t = new Timer("AltitudePanel");
		t.scheduleAtFixedRate(tt, 0, 10);		
	}
	
	
}
