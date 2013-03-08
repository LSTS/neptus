/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/01
 * $Id:: MultibeamPanel.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.plugins.echosounder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
public class MultibeamPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private BufferedImage bi, aux;
	private int lastWidth = 0, lastHeight = 0;
	private int readingWidth = 3;
	private double[] vals = new double[0];
	protected ColorMap cmap = ColorMapFactory.createGreenRadarColorMap();
	private boolean paused = false;
	private double depth = 0;
	private NumberFormat format = GuiUtils.getNeptusDecimalFormat(1);
	private double bottomMargin = 25;
	private double range = 50;
	private String textToDisplay = null;
	private boolean showDepth = true;
	

	public MultibeamPanel() {
		
		addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				paused = true;
			}

			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				paused = false;
			}
		});
	}

	public void updateBeams(double[] vals, double depth) {
		this.vals = vals;
		this.depth = depth;
		if (Double.isNaN(depth))
			showDepth = false;
		else
			showDepth = true;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		//super.paint(g);
		if (bi == null || lastHeight != getHeight() || lastWidth != getWidth()) {
			bi = new BufferedImage(getWidth(), getHeight(),
					BufferedImage.TYPE_INT_RGB);
			aux = new BufferedImage(getWidth(), getHeight(),
					BufferedImage.TYPE_INT_RGB);
		}
		lastWidth = getWidth();
		lastHeight = getHeight();

		// translate image to the left
		aux.getGraphics().drawImage(bi, 0, 0, getWidth() - readingWidth,
				getHeight(), readingWidth, 0, getWidth(), getHeight(), null);
		bi = aux;

		// now draw the last (right) column based on current values
		Graphics2D g2d = (Graphics2D) bi.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		double bottom = (showDepth)? bottomMargin : 0;
		double yInc = (double) (getHeight()-bottom) / (double) vals.length;
		g2d.translate(getWidth() - readingWidth, 0);
		
		for (int i = 0; i < vals.length; i++) {
			g2d.setColor(cmap.getColor(vals[(vals.length-i)-1]*4)); 
			Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,(i*getHeight())/vals.length,
					 yInc);
			g2d.fill(rect);
			g2d.translate(0, yInc);
		}
		
		if (showDepth) {
			g2d.setColor(cmap.getColor(0.0));
			Rectangle2D.Double d = new Rectangle2D.Double(0, 0, readingWidth,
					bottomMargin);
			g2d.fill(d);
			
			g2d.setColor(cmap.getColor(1.0));
			d = new Rectangle2D.Double(0, (depth/range)*bottomMargin, readingWidth,
					readingWidth);
			
			g2d.fill(d);
			
			
		}
		if (!paused) {
			g.drawImage(bi, 0, 0, this);
			if (showDepth) {
				g.setColor(Color.yellow);			
				g.drawString("altitude: "+format.format(depth)+"m", 2, (int)(getHeight()-bottomMargin+12));
			}
		}
		//g2d.setColor(Color.green.darker());		
		//d = new Rectangle2D.Double(0, (depth/range)*bottomMargin, readingWidth, 5);
		//g2d.fill(d);
		

		// if the mouse is pressed, the contents aren't updated
		
		
		if (textToDisplay != null) {
			((Graphics2D)g).setTransform(new AffineTransform());
			g.drawString(textToDisplay, 5, 15);
		}
	}
	
	/**
	 * @param cmap the cmap to set
	 */
	public void setColorMap(ColorMap cmap) {
		this.cmap = cmap;		
	}
	
	/**
	 * @param range the range to set
	 */
	public void setRange(double range) {
		this.range = range;
	}

	/**
	 * @param textToDisplay the textToDisplay to set
	 */
	public void setTextToDisplay(String textToDisplay) {
		this.textToDisplay = textToDisplay;
	}

	public static void main(String[] args) {
		final MultibeamPanel mbp = new MultibeamPanel();
		
		
		GuiUtils.testFrame(mbp);
		
		Thread t = new Thread() {
			public void run() {
				while(true) {
					try {
						double[] vals = new double[100];
						for (int i = 0; i < vals.length; i++)
							vals[i] = Math.random();
						mbp.updateBeams(vals, Double.NaN);
						Thread.sleep(10);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		};
		t.start();
	}
}
