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
 * Sep 24, 2010
 * $Id:: OperationLimits.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.plugins.oplimits;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;

import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.ParallelepipedElement;


/**
 * @author zp
 *
 */
@XmlRootElement(name="OperationLimits")
@LayerPriority(priority=35)
public class OperationLimits implements Renderer2DPainter {
	
	protected Double maxDepth 		= null;
	protected Double minAltitude	= null;
	protected Double maxAltitude 	= null;
	protected Double maxSpeed 		= null;
	protected Double minSpeed 		= null;
	protected Double maxVertRate 	= null;
	
	protected Double opAreaWidth 	= null;
	protected Double opAreaLength 	= null;
	protected Double opAreaLat 		= null;
	protected Double opAreaLon	 	= null;
	protected Double opRotationRads	= null;
	
	protected boolean editing = false;
	
	public Double getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(Double maxDepth) {
		this.maxDepth = maxDepth;
	}

	public Double getMinAltitude() {
		return minAltitude;
	}

	public void setMinAltitude(Double minAltitude) {
		this.minAltitude = minAltitude;
	}

	public Double getMaxAltitude() {
		return maxAltitude;
	}

	public void setMaxAltitude(Double maxAltitude) {
		this.maxAltitude = maxAltitude;
	}

	public Double getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(Double maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public Double getMinSpeed() {
		return minSpeed;
	}

	public void setMinSpeed(Double minSpeed) {
		this.minSpeed = minSpeed;
	}

	public Double getMaxVertRate() {
		return maxVertRate;
	}

	public void setMaxVertRate(Double maxVertRate) {
		this.maxVertRate = maxVertRate;
	}

	public Double getOpAreaWidth() {
		return opAreaWidth;
	}

	public void setOpAreaWidth(Double opAreaWidth) {
		this.opAreaWidth = opAreaWidth;
	}

	public Double getOpAreaLength() {
		return opAreaLength;
	}

	public void setOpAreaLength(Double opAreaLength) {
		this.opAreaLength = opAreaLength;
	}

	public Double getOpAreaLat() {
		return opAreaLat;
	}

	public void setOpAreaLat(Double opAreaLat) {
		this.opAreaLat = opAreaLat;
	}

	public Double getOpAreaLon() {
		return opAreaLon;
	}

	public void setOpAreaLon(Double opAreaLon) {
		this.opAreaLon = opAreaLon;
	}

	public Double getOpRotationRads() {
		return opRotationRads;
	}

	public void setOpRotationRads(Double opRotationRads) {
		this.opRotationRads = opRotationRads;
	}
	
	public void setArea(ParallelepipedElement selection) {
		if (selection == null) {
			opAreaLat = opAreaLon = opAreaLength = opAreaWidth = opRotationRads = null;
		}
		else {
			double[] lld = selection.getCenterLocation().getAbsoluteLatLonDepth();
			opAreaLat = lld[0];
			opAreaLon = lld[1];
			opAreaLength = selection.getLength();
			opAreaWidth = selection.getWidth();
			opRotationRads = selection.getYawRad();
		}
	}
	
	
	
	
	@Override
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		
	    if (opAreaLat != null && opAreaLength != null && opAreaLon != null && opAreaWidth != null && opRotationRads != null) {
			LocationType lt = new LocationType();
			lt.setLatitude(opAreaLat);
			lt.setLongitude(opAreaLon);
			Point2D pt = renderer.getScreenPosition(lt);
			g.translate(pt.getX(), pt.getY());
			g.scale(1, -1);
			g.rotate(renderer.getRotation());	
			g.rotate(-opRotationRads+Math.PI/2);
			g.setColor(Color.red.brighter());
			double length = opAreaLength * renderer.getZoom();
			double width = opAreaWidth * renderer.getZoom();
			
			g.draw(new Rectangle2D.Double(-length/2, -width/2, length, width));
		}
		
		
	}
	
	public boolean showDialog() {
		
		return false;
	}
	
	public String asXml() {
		StringWriter writer = new StringWriter();
		JAXB.marshal(this, writer);
		return writer.toString();
	}
	
	public static OperationLimits loadXml(String xml) {
		return JAXB.unmarshal(new StringReader(xml), OperationLimits.class);		
	}

    public static void main(String[] args) {
		OperationLimits lims = new OperationLimits();
		lims.setMaxAltitude(200d);
		lims.setMinAltitude(100d);
		String xml = lims.asXml();
		System.out.println(xml);
		OperationLimits lims2 = OperationLimits.loadXml(xml);
		System.out.println(lims2.asXml());
	}
}
