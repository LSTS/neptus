/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Sep 24, 2010
 */
package pt.lsts.neptus.mp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ParallelepipedElement;


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
			lt.setLatitudeDegs(opAreaLat);
			lt.setLongitudeDegs(opAreaLon);
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
		NeptusLog.pub().info("<###> "+xml);
		OperationLimits lims2 = OperationLimits.loadXml(xml);
		NeptusLog.pub().info("<###> "+lims2.asXml());
	}
}
