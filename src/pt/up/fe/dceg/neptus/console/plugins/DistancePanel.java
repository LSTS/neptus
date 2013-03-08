/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: DistancePanel.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class DistancePanel extends JPanel {
	
    private static final long serialVersionUID = 1L;

    public static final int VERTICAL_UP = 0, VERTICAL_DOWN = 1, 
							HORIZONTAL_LEFT =2, HORIZONTAL_RIGHT=3;
	
	private int type=VERTICAL_UP;
	private double value=10.0;
	//private String label;
	public Graphics2D g2=null;
	private BufferedImage bi = null;
	
	public DistancePanel()
	{
		//distance = 10.0;
		//setBorder(new TitledBorder(new LineBorder(Color.YELLOW)));
	}
	
	public DistancePanel(int type,double value)
	{
		this();
		this.value=value;
		this.type=type;
	}
	
	@Override
	public void paint(Graphics arg0) {
		super.paint(arg0);
		update(arg0);
	}
	
	public void update(Graphics arg0) {

		try {

			this.g2 = (Graphics2D) arg0;

			if (g2 == null)
				return;

			// this.g2 = (Graphics2D)this.getGraphics();
			if (bi == null || bi.getWidth() < this.getWidth()
					|| bi.getHeight() < this.getHeight())
				bi = (BufferedImage) createImage(this.getWidth(), this
						.getHeight());
			Graphics2D g = (Graphics2D) bi.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			g.setPaintMode();
			g.clearRect(0, 0, bi.getWidth(), bi.getHeight());

			// Graphics2D gimage=(Graphics2D)image.getGraphics();
			// gimage.translate(10,10);
			// gimage.rotate(Math.PI/20);
			// gimage.finalize();

			// g2.translate();
			// g2.translate(image.getWidth(null)/2,image.getHeight(null)/2);

			int percent_arrow_lenght = this.getWidth() / 9;
			int percent_arrow_height = this.getHeight() / 7;
			if (type == VERTICAL_UP) {
				g.drawLine(this.getWidth() / 2, 0, this.getWidth() / 2, this
						.getHeight());

				g.drawLine(this.getWidth() / 2 - percent_arrow_lenght,
						percent_arrow_height, this.getWidth() / 2, 0);
				g.drawLine(this.getWidth() / 2 + percent_arrow_lenght,
						percent_arrow_height, this.getWidth() / 2, 0);

				Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(
						value + "m", g2);

				g.drawString(value + "m",
						(int) ((this.getWidth() / 2) - stringBounds
								.getCenterX()),
						(int) ((this.getHeight()) / 2 - stringBounds
								.getCenterY()));
			}
			if (type == VERTICAL_DOWN) {
				
				//linha central
				g.drawLine(this.getWidth() / 2, 0, this.getWidth() / 2, this
						.getHeight());
				
				//seta
				g.drawLine(this.getWidth() / 2 - percent_arrow_lenght,
						this.getHeight()-percent_arrow_height, this.getWidth() / 2, this.getHeight()-1);
				g.drawLine(this.getWidth() / 2 + percent_arrow_lenght,
						this.getHeight()-percent_arrow_height, this.getWidth() / 2, this.getHeight()-1);
				
				// valor
				Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(
						value + "m", g);

				g.drawString(value + "m",
						(int) ((this.getWidth() / 2) - stringBounds
								.getCenterX()),
						(int) ((this.getHeight()) / 2 - stringBounds
								.getCenterY()));
			}
			
			if (type == HORIZONTAL_LEFT) {
				
				//linha central
				g.drawLine(0, this.getHeight() / 2, this.getWidth(), this
						.getHeight()/2);
				
				//seta
				g.drawLine(0, this.getHeight() / 2,
					percent_arrow_lenght,this.getHeight()/2-percent_arrow_height);
				g.drawLine(0, this.getHeight() / 2,
						percent_arrow_lenght,this.getHeight()/2+percent_arrow_height);
				
				
				//				 valor
				Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(
						value + "m", g2);
				g.drawString(value + "m",
						(int) ((this.getWidth() / 2) - stringBounds
								.getCenterX()),
						(int) ((this.getHeight()) / 2 - stringBounds
								.getMinY()));
			}
			
			if (type == HORIZONTAL_RIGHT) {
				
				//linha central
				g.drawLine(0, this.getHeight() / 2, this.getWidth(), this
						.getHeight()/2);
				
				//seta
				g.drawLine(this.getWidth()-percent_arrow_lenght,this.getHeight()/2-percent_arrow_height ,
						this.getWidth()-1,this.getHeight() / 2);
				g.drawLine(this.getWidth()-1, this.getHeight() / 2,
						this.getWidth()-percent_arrow_lenght,this.getHeight()/2+percent_arrow_height);
				
				
				//				 valor
				Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(
						value + "m", g2);
				g.drawString(value + "m",
						(int) ((this.getWidth() / 2) - stringBounds
								.getCenterX()),
						(int) ((this.getHeight()) / 2 - stringBounds
								.getMinY()));
			}
			
			g2.drawImage(bi, 0, 0, this);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}


	public int getType() {
		return type;
	}


	public void setType(int type) {
		this.type = type;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.repaint();
		this.value = value;
	}
	

}
