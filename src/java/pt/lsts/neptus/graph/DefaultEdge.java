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
package pt.lsts.neptus.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

public class DefaultEdge<O> implements NeptusEdgeElement<O> {

	
	private static GeneralPath arrowTip = new GeneralPath();
	
	static {		
		arrowTip.moveTo(0, 0);
		arrowTip.lineTo(0, -4);
		arrowTip.lineTo(9, 0);
		arrowTip.lineTo(0, 4);
		arrowTip.closePath();
	}
	
	private static IdGenerator idGenerator = new IdGenerator(false);
	private int myId = idGenerator.generateId();
	private String id = getElementName()+"_"+myId;
	private String sourceNode = null;
	private String targetNode = null;
	private O object = null;
	private boolean selected = false;
	
	Point2D srcPt = new Point2D.Double();
	Point2D tgtPt = new Point2D.Double();

	
	public String getSourceNodeID() {
		return sourceNode;
	}

	public String getTargetNodeID() {
		return targetNode;
	}

	public void setSourceNodeID(String sourceNodeId) {
		this.sourceNode = sourceNodeId;
	}

	public void setTargetNodeID(String targetNodeId) {
		this.targetNode = targetNodeId;
	}

	public String getElementName() {
		return "Edge";
	}

	public String getID() {
		return id;
	}

	public O getUserObject() {
		return object;
	}

	public void paint(Graphics2D g, NeptusGraph<?, ?> graph) {				
		Graphics2D g2d = (Graphics2D) g;
		g.setColor(Color.BLACK);
		g2d.setTransform(graph.getCurrentTransform());
		
		if (graph.getNode(getSourceNodeID()) == null) {
			throw new RuntimeException("Invalid edge. The source node '"+getSourceNodeID()+"' does not exist");
		}
		
		if (graph.getNode(getTargetNodeID()) == null) {
			throw new RuntimeException("Invalid edge. The target node '"+getTargetNodeID()+"' does not exist");
		}
		srcPt = graph.getNode(getSourceNodeID()).getPosition();
		tgtPt = graph.getNode(getTargetNodeID()).getPosition();
		
		double diffX = tgtPt.getX()-srcPt.getX();
		double diffY = tgtPt.getY()-srcPt.getY();
		
		double angle = Math.atan2(diffY,diffX);
		if (selected)
			g2d.setColor(Color.blue);
		
		if (!getSourceNodeID().equals(getTargetNodeID())) {
		
			g2d.translate(srcPt.getX(), srcPt.getY());
			
			g2d.rotate(angle);
			GeneralPath gp = new GeneralPath();
			gp.moveTo(DefaultNode.circleRadius, 0);
			gp.quadTo(tgtPt.distance(srcPt)/2, -tgtPt.distance(srcPt)/30, tgtPt.distance(srcPt) - DefaultNode.circleRadius, 0);
			g2d.draw(gp);
			
			g2d.translate(
					tgtPt.distance(srcPt) - DefaultNode.circleRadius-7,
					0
			);
			
			g2d.fill(arrowTip);	
		
		}
		else {
			g2d.translate(
					tgtPt.getX(),
					tgtPt.getY() - DefaultNode.circleRadius
			);
			g2d.draw(new Arc2D.Double(-12, -20, 24, 24, -50.0, 250.0, Arc2D.OPEN));
			g2d.translate(-11,-5);
			g2d.rotate((-Math.PI/1.6)+Math.PI);
			g2d.fill(arrowTip);			
		}
		
		g2d.setColor(Color.black);		
		
	}

	public void setID(String id) {
		this.id = id;
	}

	public void setUserObject(O userObject) {
		object = userObject;
	}

	public DefaultProperty[] getProperties() {
		return new DefaultProperty[] {};
	}

	public String getPropertiesDialogTitle() {
		return "Properties for "+getID()+" edge";
	}

	public String[] getPropertiesErrors(Property[] properties) {		
		return null;
	}

	public void setProperties(Property[] properties) {
	}
	
	public boolean containsPoint(Point2D point) {
		double minDist = 5;
		
		if (sourceNode.equals(targetNode)) {
			
			double x = srcPt.getX();
			double y = srcPt.getY();
						
			Ellipse2D.Double ellis1 = new Ellipse2D.Double(-14+x, -44+y, 28, 28);
			Ellipse2D.Double ellis2 = new Ellipse2D.Double(-10+x, -36+y, 20, 20);
				
			return ellis1.contains(point) && !ellis2.contains(point);
		}
		
		double minX = Math.min(srcPt.getX(), tgtPt.getX());
		double minY = Math.min(srcPt.getY(), tgtPt.getY());
		
		double maxX = Math.max(srcPt.getX(), tgtPt.getX());
		double maxY = Math.max(srcPt.getY(), tgtPt.getY());
		
		if (point.getX() < minX || point.getX() > maxX) {
			return false;
		}
		
		if (point.getY() < minY || point.getY() > maxY) {
			return false;
		}
		
		Line2D.Double myLine = new Line2D.Double(srcPt, tgtPt);
		
		if (myLine.ptLineDist(point) < minDist)
			return true;				
		else
			return false;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public void cleanup() {
		idGenerator.recycleId(myId);
	}
}
