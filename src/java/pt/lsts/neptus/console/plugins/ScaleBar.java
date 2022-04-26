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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.console.plugins;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import javax.swing.JPanel;

import pt.lsts.neptus.util.GuiUtils;

/**
 * 
 * @author RJPG
 *
 */
public class ScaleBar extends JPanel {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    public Graphics2D g2 = null;
	public float maxvalue = 200.f;
	public float minvalue = 00.f;
	public float minredlight = -150.f;
	public float maxredlight = 150.f;
	public float value = 0.f;
	public float step = 25.f;
	public float substep = 5;
	public String label = "Motor";
	public boolean digital = true;
	public boolean hor = false;

    public int precision = 0;
    protected NumberFormat nf = GuiUtils.getNeptusDecimalFormat(precision);
    
	public boolean isDigital() {
		return digital;
	}

	public void setDigital(boolean digital) {
		this.digital = digital;
	}

	public int getDigprecision() {
		return digprecision;
	}

	public void setDigprecision(int digprecision) {
		this.digprecision = digprecision;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
		repaint();
	}

	public float getSubstep() {
		return substep;
	}

	public void setSubstep(float substep) {
		this.substep = substep;
	}

	/**
	 * This is the default constructor
	 */
	public ScaleBar() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(100, 100);
	}

	@Override
	public void paint(Graphics arg0) {
		super.paint(arg0);
		update(arg0);
	}

	private BufferedImage bi = null;//ew BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

	Graphics2D g = null;

	public void update(Graphics arg0) {

		if (getWidth() == 0)
			return;

		//NeptusLog.pub().info("<###>chamou newvalues");
		//falha a 1 acaba  
		//se passa à segunda condição ok -->
		//NumberFormat nf = GuiUtils.getNeptusDecimalFormat(precision);
		try {

			this.g2 = (Graphics2D) arg0;

			if (g2 == null)
				return;

			int width;
			int height;
			width = this.getWidth();
			height = this.getHeight();

			int bordery = height / 30;
			int borderx = width / 30;

			int axisx = width / 2;
			int axisy = height / 2;

			//-------------initialize gr------------------------
			//Image bi = (BufferedImage)createImage(width,height);
			if (bi == null || bi.getWidth() < width || bi.getHeight() < height)
				bi = (BufferedImage) createImage(width, height);
			//		bi =new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

			//	MediaTracker mt = new MediaTracker(this);
			//	mt.addImage(bi, 0);
			//	mt.waitForAll();

			g = (Graphics2D) bi.getGraphics();

			//	g.setBackground(new Color(1,1,1));
			//	g.setColor(new Color(1,1,1));
			g.clearRect(0, 0, width, height);

			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			g.setPaintMode();
			//g.clearRect(0,0,width,height);

			//---------------------------------------------------

			int radius = axisy - (bordery);
			if (radius > (axisx - borderx))
				radius = axisx - borderx;
			//double length = ((maxvalue - minvalue));
			//double passos = length / this.step;

			if (hor) {

				Font font = new Font("Arial", Font.PLAIN,
						2 * (int) ((axisx - borderx) + this.step) / 40);
				g.setFont(font);

				//angle-=Math.toRadians((200*this.minvalue)/(maxvalue-minvalue));
				//((this.value)*Math.toRadians(200.))
				//		/(maxvalue-minvalue);

				//angle=angle-Math.toRadians(180);
				Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(
						"" + this.maxvalue, g);
				for (float d = this.minvalue; d <= this.maxvalue; d += this.step) {
					if (d < this.minredlight || d > this.maxredlight)
						g.setColor(Color.red);
					else
						g.setColor(Color.black);
					int w = width - (2 * borderx);
					int x = (int) ((d * (w)) / (this.maxvalue - this.minvalue));
					x -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
					x += borderx;

					g.drawLine(x, bordery, x, height
							- (bordery + (int) (stringBounds.getWidth())));
					g
							.translate((x + (int) (stringBounds.getCenterY())),
									bordery
											+ height
											- (bordery + (int) (stringBounds
													.getWidth())));
					g.rotate(Math.PI / 2);
					nf = GuiUtils.getNeptusDecimalFormat(digprecision);
					g.drawString(nf.format(d), 0, 0);
					g.rotate(-Math.PI / 2);
					g.translate(-(x + (int) (stringBounds.getCenterY())),
							-(bordery + height - (bordery + (int) (stringBounds
									.getWidth()))));
				}

				for (float d = this.minvalue; d <= this.maxvalue; d += this.step
						/ this.substep) {
					if (d < this.minredlight || d > this.maxredlight)
						g.setColor(Color.red);
					else
						g.setColor(Color.black);
					int w = width - (2 * borderx);
					int x = (int) ((d * (w)) / (this.maxvalue - this.minvalue));
					x -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
					x += borderx;
					g.drawLine(x, bordery, x, height
							- (bordery * 10 + (int) (stringBounds.getWidth())));
				}

				g.setColor(Color.green);
				g.setXORMode(Color.RED);
				if (label != null) {
					stringBounds = g.getFontMetrics().getStringBounds(
							this.label, g);

					g.drawString(this.label, borderx, height - bordery);

				}

				if (digital) {
					nf = GuiUtils.getNeptusDecimalFormat(digprecision);
					stringBounds = g.getFontMetrics().getStringBounds(
							nf.format(this.value), g);

					g.drawString(nf.format(this.value), axisx
							- (int) stringBounds.getCenterX(), bordery
							+ (int) stringBounds.getHeight());

				}

				int w = width - (2 * borderx);
				double d = this.value;
				int x = (int) ((d * (w)) / (this.maxvalue - this.minvalue));
				x -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
				x += borderx;
				int zero = (int) ((0. * (w)) / (this.maxvalue - this.minvalue));
				zero -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
				zero += borderx;
				stringBounds = g.getFontMetrics().getStringBounds(
						"" + this.maxvalue, g);
				if(zero<x)
				for (int i = zero; i < x; i++)
					g.drawLine(i, bordery + bordery, i, height
							- (bordery * 10 + (int) (stringBounds.getWidth())));
				else
				for (int i = zero; i > x; i--)
					g.drawLine(i, bordery + bordery, i, height
							- (bordery * 10 + (int) (stringBounds.getWidth())));
					

				//g.drawArc(borderx,bordery,width,height,-190,190);
				//NeptusLog.pub().info("<###> "+g2);
				//NeptusLog.pub().info("<###> "+bi);
			} else {
				Font font = new Font("Arial", Font.PLAIN,
						2 * (int) ((axisy - bordery) + this.step) / 20);
				g.setFont(font);

				//angle-=Math.toRadians((200*this.minvalue)/(maxvalue-minvalue));
				//((this.value)*Math.toRadians(200.))
				//		/(maxvalue-minvalue);

				//angle=angle-Math.toRadians(180);
				Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(
						"" + this.maxvalue, g);
				for (float d = this.minvalue; d <= this.maxvalue; d += this.step) {
					if (d < this.minredlight || d > this.maxredlight)
						g.setColor(Color.red);
					else
						g.setColor(Color.black);
					int w = height - (2 * bordery);
					int x = (int) ((d * (w)) / (this.maxvalue - this.minvalue));
					x -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
					x += bordery;

					g.drawLine(borderx,height-x,width
							- (borderx + (int) (stringBounds.getWidth())), height-x );
					
					g.translate(borderx	+ width - (borderx + (int) (stringBounds.getWidth()))
							,height-(x + (int) (stringBounds.getCenterY())));
		
					nf = GuiUtils.getNeptusDecimalFormat(digprecision);
					g.drawString(nf.format(d), 0, 0);
					g.translate(-(borderx	+ width - (borderx + (int) (stringBounds.getWidth())))
							,-(height-(x + (int) (stringBounds.getCenterY()))));
				}

				for (float d = this.minvalue; d <= this.maxvalue; d += this.step
						/ this.substep) {
					if (d < this.minredlight || d > this.maxredlight)
						g.setColor(Color.red);
					else
						g.setColor(Color.black);

					int w = height - (2 * bordery);
					int x = (int) ((d * (w)) / (this.maxvalue - this.minvalue));
					x -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
					x += bordery;
					g.drawLine(borderx,height-x,width
							- (borderx*10 + (int) (stringBounds.getWidth())), height-x );
					
				}

				g.setColor(Color.blue);
				//g.setXORMode(Color.RED);
				if (label != null) {
					stringBounds = g.getFontMetrics().getStringBounds(
							this.label, g);

					g.drawString(this.label, borderx, bordery);

				}

				if (digital) {
					nf = GuiUtils.getNeptusDecimalFormat(digprecision);
					stringBounds = g.getFontMetrics().getStringBounds(
							nf.format(this.value), g);

					g.drawString(nf.format(this.value), 
							 
							borderx	+ (int) stringBounds.getWidth(),
							axisy - (int) stringBounds.getCenterY());

				}

				g.setColor(Color.green);
				g.setXORMode(Color.RED);
				int w = height - (2 * bordery);
				double d = this.value;
				int x = (int) ((d * (w)) / (this.maxvalue - this.minvalue));
				x -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
				x += bordery;

				int zero = (int) ((0. * (w)) / (this.maxvalue - this.minvalue));
				zero -= (int) ((this.minvalue * (w)) / (this.maxvalue - this.minvalue));
				zero += bordery;
				
				stringBounds = g.getFontMetrics().getStringBounds(
						"" + this.maxvalue, g);
				if(zero<x)
				for (int i = zero; i < x; i++)
					g.drawLine( borderx + borderx, height-i, 
							width - (borderx * 10 + (int) (stringBounds.getWidth())),height-i);
				else
				for (int i = zero; i > x; i--)
					g.drawLine( borderx + borderx, height-i, 
							width - (borderx * 10 + (int) (stringBounds.getWidth())),height-i);

				//g.drawArc(borderx,bordery,width,height,-190,190);
				//NeptusLog.pub().info("<###> "+g2);
				//NeptusLog.pub().info("<###> "+bi);
			}

			g2.drawImage(bi, 0, 0, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] arg) {

		ScaleBar m = new ScaleBar();
		GuiUtils.testFrame(m, "ScaleBar");
		m.setHorizontal();
		for (int i = 00;; i += 2) {
			//motor.reDraw();
			try {//nada
				//NeptusLog.pub().info("<###>espera...");
				m.setValue(i);
				Thread.sleep(100);
				//NeptusLog.pub().info("<###>esperou");
			} catch (Exception e) {
			    e.printStackTrace();
			}

		}
	}

	public float getMaxvalue() {
		return maxvalue;
	}

	public void setMaxvalue(float maxvalue) {
		this.maxvalue = maxvalue;
		this.repaint();
	}

	public float getMinvalue() {
		return minvalue;
	}

	public void setMinvalue(float minvalue) {
		this.minvalue = minvalue;
		this.repaint();

	}

	public float getStep() {
		return step;
	}

	public void setStep(float step) {
		this.step = step;
		this.repaint();

	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
		if(value>maxvalue)
			this.value=maxvalue;
		if(value<minvalue)
			this.value=minvalue;
		this.repaint();
	}

	public float getMaxredlight() {
		return maxredlight;
	}

	public void setMaxredlight(float maxredlight) {
		this.maxredlight = maxredlight;
	}

	public float getMinredlight() {
		return minredlight;
	}

	public void setMinredlight(float minredlight) {
		this.minredlight = minredlight;
	}

	
	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
        nf = GuiUtils.getNeptusDecimalFormat(precision);
		this.repaint();

	}

	public int digprecision = 0;

	public int getDigPrecision() {
		return digprecision;
	}

	public void setDigPrecision(int precision) {
		this.digprecision = precision;
		this.repaint();

	}
	public void setHorizontal()
	{
		hor=true;
	}
	public void setVertical()
	{
		hor=false;
	}

	public boolean isHor() {
		return hor;
	}

	public void setHor(boolean hor) {
		this.hor = hor;
	}
}
