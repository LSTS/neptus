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
 * $Id:: ManeuverNode.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.planeditor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Vector;

import pt.up.fe.dceg.neptus.graph.DefaultNode;
import pt.up.fe.dceg.neptus.graph.NeptusEdgeElement;
import pt.up.fe.dceg.neptus.graph.NeptusGraph;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.mp.Maneuver;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
/**
 * 
 * @author ZP
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ManeuverNode extends DefaultNode<Maneuver> {

	
	public static boolean showManeuverName = true;
	
	@Override
	public String getElementName() {
		return "Man";
	}
	
	@Override
	public void paint(Graphics2D g, NeptusGraph graph) {
		super.paint(g, graph);
		g.setTransform(graph.getCurrentTransform());
		g.translate(getPosition().getX(), getPosition().getY());
		String manType = getUserObject().getClass().getSimpleName();
		
		Rectangle2D bounds = g.getFontMetrics().getStringBounds(manType, g);		
		g.setColor(new Color(50,100,200));					
		bounds = g.getFontMetrics().getStringBounds(manType, g);					
		g.drawString(manType, (float)-bounds.getWidth()/2, (float) circleRadius+12);		
	}
	
	public ManeuverNode(Maneuver maneuver) {		
		this.setID(maneuver.getId());
		setUserObject(maneuver);	
	}
	
	@Override
	public void setUserObject(Maneuver maneuver) {
		object = maneuver;
		this.setInitialNode(maneuver.isInitialManeuver());
		this.setPosition(new Point2D.Double(maneuver.getXPosition(), maneuver.getYPosition()));		
	}
	
	@Override
	public Maneuver getUserObject() {
		object.setInitialManeuver(isInitialNode());
		object.setXPosition((int)getPosition().getX());
		object.setYPosition((int)getPosition().getY());
		return object;
	}
	
	@Override
	public void addOutgoingEdge(NeptusEdgeElement edge) {
		super.addOutgoingEdge(edge);
		getUserObject().addTransition(edge.getTargetNodeID(), edge.getUserObject().toString());
	}
	
	@Override
	public void removeIncomingEdge(NeptusEdgeElement edge) {
		super.removeIncomingEdge(edge);
		getUserObject().removeTransition(edge.getTargetNodeID());
	}

	
	
	@Override
	public String getPropertiesDialogTitle() {
		return getID()+" properties";
	}
	
	@Override
	public DefaultProperty[] getProperties() {	
		if (getUserObject() instanceof PropertiesProvider) {
			Vector<DefaultProperty> allProps = new Vector<DefaultProperty>();
			allProps.addAll(Arrays.asList(super.getProperties()));
			allProps.addAll(Arrays.asList(((PropertiesProvider)getUserObject()).getProperties()));
			return allProps.toArray(new DefaultProperty[] {});
		}
		else
			return super.getProperties();
	}
	
	@Override
	public void setProperties(Property[] properties) {
		if (getUserObject() instanceof PropertiesProvider) {
			((PropertiesProvider)getUserObject()).setProperties(properties);
		}
		super.setProperties(properties);
	}
	
	@Override
	public int getMaxX() {
		return super.getMaxX();
	}
	
	@Override
	public int getMaxY() {
		return super.getMaxY()+12;
	}
}
