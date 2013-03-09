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
 */
package pt.up.fe.dceg.neptus.graph;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class DanglingEdge {

	private Point2D targetPoint = null;
	private NeptusNodeElement<?> sourceNode = null; 
	private static GeneralPath arrowTip = new GeneralPath();
	
	static {		
		arrowTip.moveTo(0, 0);
		arrowTip.lineTo(0, -4);
		arrowTip.lineTo(9, 0);
		arrowTip.lineTo(0, 4);
		arrowTip.closePath();
	}
	
	public DanglingEdge(NeptusNodeElement<?> sourceNode, Point2D targetPoint) {
		this.sourceNode = sourceNode;
		this.targetPoint = targetPoint;
	}
	
	public void paint(Graphics2D g, NeptusGraph<?, ?> graph) {
		
		g.setTransform(graph.getCurrentTransform());
		
		if (sourceNode.containsPoint(targetPoint)) {
			g.translate(
					sourceNode.getPosition().getX(),
					sourceNode.getPosition().getY() - DefaultNode.circleRadius
			);
			g.draw(new Ellipse2D.Double(-12, -20, 24, 24));
			g.translate(-11,-5);
			g.rotate((-Math.PI/1.6)+Math.PI);
			g.fill(arrowTip);
		}
		
		g.setTransform(graph.getCurrentTransform());
		
		g.draw(new Line2D.Double(sourceNode.getPosition(), targetPoint));
		g.translate(targetPoint.getX(), targetPoint.getY());
		
		double diffX = targetPoint.getX()-sourceNode.getPosition().getX();
		double diffY = targetPoint.getY()-sourceNode.getPosition().getY();
		
		double angle = Math.atan2(diffY,diffX);
		
		g.rotate(angle);
		g.translate(-7, 0);
		
		g.fill(arrowTip);
	}

	public NeptusNodeElement<?> getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(NeptusNodeElement<?> sourceNode) {
		this.sourceNode = sourceNode;
	}

	public Point2D getTargetPoint() {
		return targetPoint;
	}

	public void setTargetPoint(Point2D targetPoint) {
		this.targetPoint = targetPoint;
	}
}
