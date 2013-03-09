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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

@SuppressWarnings("rawtypes")
public class DefaultNode<O> implements NeptusNodeElement<O>{

	private Point2D position = new Point2D.Double(0,0);
	protected O object; 
	public static double circleRadius = 20;
	private boolean initialNode = false;
	private boolean finalNode = false;
	private boolean selected = false;
	private static IdGenerator idGenerator = new IdGenerator(false);
	private int myId = idGenerator.generateId();
	private String id = getElementName()+myId;
	
	LinkedHashMap<String, NeptusEdgeElement<O>> outgoingEdges = new LinkedHashMap<String, NeptusEdgeElement<O>>();
	LinkedHashMap<String, NeptusEdgeElement<O>> incomingEdges = new LinkedHashMap<String, NeptusEdgeElement<O>>();
	
	private static GeneralPath arrowTip = new GeneralPath();
	
	static {		
		arrowTip.moveTo(0, 0);
		arrowTip.lineTo(0, -4);
		arrowTip.lineTo(9, 0);
		arrowTip.lineTo(0, 4);
		arrowTip.closePath();
	}

	public Point2D getPosition() {		
		return position;
	}

	public void setPosition(Point2D point) {
		this.position.setLocation(point);
	}

	public String getElementName() {		
		return "S";
	}

	public String getID() {		
		return id;		
	}

	public O getUserObject() {		
		return object;
	}

	public void paint(Graphics2D g, NeptusGraph<?, ?> graph) {
		g.setTransform(graph.getCurrentTransform());
		g.translate(position.getX(), position.getY());
		
		if (!selected)
			g.setPaint(new GradientPaint(-25f,-25f, new Color(255,255,255), 50f, 50f,new Color(200,200,250)));
		else
			g.setPaint(new GradientPaint(-25f,-25f, new Color(200,230,255), 50f, 50f,new Color(150,150,250)));
		
		g.fill(new Ellipse2D.Double(-circleRadius, -circleRadius, circleRadius*2, circleRadius*2));
		g.setColor(Color.BLACK);
		g.draw(new Ellipse2D.Double(-circleRadius, -circleRadius, circleRadius*2, circleRadius*2));
		
		Rectangle2D bounds = g.getFontMetrics().getStringBounds(getID(), g);

		if ((bounds.getWidth()+2.0) > circleRadius*2) {
			g.scale(circleRadius*2/(bounds.getWidth()+2.0), circleRadius*2/(bounds.getWidth()+2.0));
		}
		
		g.drawString(toString(), (int)(-bounds.getWidth()/2+1), 6);
		
		g.setTransform(graph.getCurrentTransform());
		g.translate(position.getX(), position.getY());
		
		if (isInitialNode()) {
			g.translate(-circleRadius-7, 0);
			g.drawLine(-9, 0, 0, 0);
			g.drawLine(-9, -1, 0, -1);
			g.fill(arrowTip);
			g.translate(circleRadius+7, 0);
		}
		
		if (isFinalNode()) {
			int diff = (int) (circleRadius*0.15);
			g.draw(new Ellipse2D.Double(-circleRadius+diff, -circleRadius+diff, circleRadius*2-diff*2, circleRadius*2-diff*2));
		}
	}

	public void setID(String id) {
		this.id = id;
	}

	public void setUserObject(O userObject) {
		this.object = userObject;
	}

	public DefaultProperty[] getProperties() {
		DefaultProperty p1 = PropertiesEditor.getPropertyInstance("ID", String.class, getID(), false);
		DefaultProperty p2 = PropertiesEditor.getPropertyInstance("Initial", Boolean.class, isInitialNode(), true);
		DefaultProperty p3 = PropertiesEditor.getPropertyInstance("Final", Boolean.class, isFinalNode(), true);
		return new DefaultProperty[] {p1,p2,p3};
	}

	public String getPropertiesDialogTitle() {
		return "Properties for "+getID()+" node";
	}

	public String[] getPropertiesErrors(Property[] properties) {		
		return null;
	}

	public void setProperties(Property[] properties) {
		for (Property p : properties) {
			if (p.getName().equals("Initial")) {
				setInitialNode((Boolean)p.getValue());
			}
			if (p.getName().equals("Final")) {
				setFinalNode((Boolean)p.getValue());
			}
		}
	}

	public boolean isFinalNode() {
		return finalNode;
	}

	public void setFinalNode(boolean finalNode) {
		this.finalNode = finalNode;
	}

	public boolean isInitialNode() {
		return initialNode;
	}

	public void setInitialNode(boolean initialNode) {
		this.initialNode = initialNode;
	}
	
	public boolean containsPoint(Point2D point) {
		return position.distance(point) <= circleRadius;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public int getMaxX() {
		return (int)(getPosition().getX()+circleRadius+2);
	}
	
	public int getMaxY() {
		return (int)(getPosition().getY()+circleRadius+2);
	}
	
	public void cleanup() {
		idGenerator.recycleId(myId);
	}
	
	@Override
	public String toString() {
		return getID();
	}
	
	public void addIncomingEdge(NeptusEdgeElement<O> edge) {
		incomingEdges.put(edge.getID(), edge);
	}
	
	public void addOutgoingEdge(NeptusEdgeElement<O> edge) {
		outgoingEdges.put(edge.getID(), edge);
	}
	
	public void removeIncomingEdge(NeptusEdgeElement edge) {
		incomingEdges.remove(edge.getID());
	}
	
	public void removeOutgoingEdge(NeptusEdgeElement edge) {
		outgoingEdges.remove(edge.getID());
	}
	
	public LinkedHashMap<String, NeptusEdgeElement<O>> getIncomingEdges() {
		return incomingEdges;
	}
	
	public LinkedHashMap<String, NeptusEdgeElement<O>> getOutgoingEdges() {
		return outgoingEdges;
	}
	
	
}
