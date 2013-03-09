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
 * Apr 29, 2005
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zecarlos
 */
@LayerPriority(priority=100)
public class MapLegend implements Renderer2DPainter {

	private double zoomValue = 1;
	private LocationType centerLocation = new LocationType();
	private double mapRotation = 0;
	
	private int width = 70, height = 80;
	private NumberFormat nf;
	

	public MapLegend() {
		 nf = DecimalFormat.getInstance(Locale.US);
		 nf.setGroupingUsed(false);
		 nf.setMinimumFractionDigits(2);
		 nf.setMaximumFractionDigits(2);
	}
	
	public void paint(Graphics2D g, StateRenderer2D renderer) {		
		int screenWidth = renderer.getWidth();
		int screenHeight = renderer.getHeight();
		zoomValue = renderer.getZoom();
		centerLocation = renderer.getCenter();
		
		setMapRotation(renderer.getRotation());
		paint(g, screenWidth, screenHeight, false);
	}
	
	public void paint(Graphics2D g, int screenWidth, int screenHeight, boolean simple) {
		AffineTransform identity = g.getTransform();
		int cornerX = screenWidth - 10 - width;
		int cornerY = 10;
		double realWidth = (width - 20) / getZoomValue();
		String units = "m";
		
		if (realWidth < 1) {
			units = "cm";
			realWidth *= 100;
		}
		else {
			if (realWidth > 1000) {
				units = "Km";
				realWidth /= 1000;
			}
		}
		
		String text = nf.format(realWidth) +" "+units;
		
		// Reinicializa a transformação
		//g.setTransform(new AffineTransform());
		
		if (!simple) {
		
			// Desenha o rectangulo de fundo da legenda
			g.setColor(new Color(255,255,255,100));
			g.fillRect(cornerX, cornerY, width, height);
			g.setColor(Color.BLACK);
			g.drawRect(cornerX, cornerY, width, height);
		}
		else
			g.setColor(Color.BLACK);
		
		// Desenha a linha de orientacao da legenda: |-----|
		g.drawLine(cornerX + 10, cornerY + 10, cornerX + width - 10, cornerY + 10);
		g.drawLine(cornerX + 10, cornerY + 5, cornerX + 10, cornerY + 15);
		g.drawLine(cornerX + width - 10, cornerY + 5, cornerX + width - 10, cornerY + 15);
		
		// Desenha uma String com a distancia ocupada pela linha desenhada anteriormente
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, g);
		// Se a String for maior que a legenda, escala-a
		if (stringBounds.getWidth() >= width - 20) {
			g.translate(cornerX + 10, cornerY + 25);
			g.scale((double)(width-20)/(double)stringBounds.getWidth(), 
					(double)(width-20)/(double)stringBounds.getWidth());
			g.drawString(text, 0,0);
			// depois de desenhada a string, volta à escala normal
			g.setTransform(identity);
		}
		else {
			// Senão desenha a String centrada no rectangulo da legenda
			int advance = (int) (width - 20 - stringBounds.getWidth()) / 2;
			g.drawString(text, advance + cornerX + 10, cornerY + 25);
		}
		
		if (!simple) {
			g.translate(cornerX + width / 2, cornerY + (height)/ 2 + 13);
			
			g.rotate(-getMapRotation());
			GeneralPath gp = new GeneralPath();
			gp.moveTo(0, -15);
			gp.lineTo(-8, 10);
			gp.lineTo(0, 7);
			gp.lineTo(8, 10);
			gp.closePath();
			
			g.setColor(new Color(0,0,0,200));
			g.fill(gp);
			
			g.setColor(Color.BLACK);
			g.draw(gp);
			/*
			g.drawLine(0,-15,0,15);
			g.drawLine(-15,0,15,0);
			*/
			Rectangle2D nBounds = g.getFontMetrics().getStringBounds("N", g);
			
			g.drawString("N", -(int)nBounds.getWidth()/2, -16);
		}
	}

	/**
	 * @return Returns the centerLocation.
	 */
	public LocationType getCenterLocation() {
		return centerLocation;
	}
	/**
	 * @param centerLocation The centerLocation to set.
	 */
	public void setCenterLocation(LocationType centerLocation) {
		this.centerLocation = centerLocation;
	}
	/**
	 * @return Returns the zoomValue.
	 */
	public double getZoomValue() {
		return zoomValue;
	}
	/**
	 * @param zoomValue The zoomValue to set.
	 */
	public void setZoomValue(double zoomValue) {
		this.zoomValue = zoomValue;
	}
	
	public double getMapRotation() {
		return mapRotation;
	}
	
	public void setMapRotation(double mapRotation) {
		this.mapRotation = mapRotation;
	}
}
