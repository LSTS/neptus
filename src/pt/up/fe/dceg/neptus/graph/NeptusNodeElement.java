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
 * $Id:: NeptusNodeElement.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.graph;

import java.awt.geom.Point2D;
import java.util.LinkedHashMap;

public interface NeptusNodeElement<O> extends NeptusGraphElement<O> {

	public void setPosition(Point2D point);
	public Point2D getPosition();
	
	public int getMaxX();
	public int getMaxY();
	
	public void addIncomingEdge(NeptusEdgeElement<O> edge);
	
	public void addOutgoingEdge(NeptusEdgeElement<O> edge);
	
	public void removeIncomingEdge(NeptusEdgeElement<O> edge);
	
	public void removeOutgoingEdge(NeptusEdgeElement<O> edge);
	
	public LinkedHashMap<String, NeptusEdgeElement<O>> getIncomingEdges();
	public LinkedHashMap<String, NeptusEdgeElement<O>> getOutgoingEdges();
}
