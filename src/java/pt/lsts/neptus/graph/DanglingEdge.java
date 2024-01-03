/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
