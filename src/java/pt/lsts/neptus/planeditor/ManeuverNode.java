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
package pt.lsts.neptus.planeditor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.graph.DefaultNode;
import pt.lsts.neptus.graph.NeptusEdgeElement;
import pt.lsts.neptus.graph.NeptusGraph;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.mp.Maneuver;
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
		    try {
                ((PropertiesProvider) getUserObject()).setProperties(properties);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
            }
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
