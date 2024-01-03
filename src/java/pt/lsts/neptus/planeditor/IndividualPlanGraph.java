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
package pt.lsts.neptus.planeditor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import pt.lsts.neptus.graph.NeptusGraph;
import pt.lsts.neptus.graph.VehiclePainter;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
/**
 * 
 * @author ZP
 *
 */
public class IndividualPlanGraph extends NeptusGraph<ManeuverNode, ManeuverTransition> {

	private static final long serialVersionUID = 651868708935847095L;
	//private MissionType dummyMission = new MissionType();
	ManeuverGraphFactory factory;// = new ManeuverGraphFactory(dummyMission, new IndividualPlanType(dummyMission));
	private PlanType plan;
//	private 
	public IndividualPlanGraph(PlanType plan) {		
		super();
		this.plan = plan;
		setFactory(new ManeuverGraphFactory(plan.getMissionType(), plan));		
		addPreRenderPainter(new VehiclePainter(plan));
	}
	
	@Override
	public AbstractAction[] getClickActions(MouseEvent evt) {
		if (evt.getButton() == MouseEvent.BUTTON3 && selection.size() == 1 && ! isEditable()) {
			AbstractAction propsAction = new AbstractAction("Properties...") {					
				/**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent arg0) {
					editSelectionProperties();						
				}
			};
			return new AbstractAction[] {propsAction};
		}
		
		if (!isEditable())
			return new AbstractAction[] {};
		
		
		Vector<AbstractAction> actions = new Vector<AbstractAction>();
		for (AbstractAction action : super.getClickActions(evt)) {
			actions.add(action);
		}
		
		return actions.toArray(new AbstractAction[] {});
	}
	
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1 && arg0.getClickCount() == 2 && getFirstNodeUnder(arg0.getPoint()) != null) {
			PropertiesEditor.editProperties(getFirstNodeUnder(arg0.getPoint()), isEditable());
			repaint();			
		}
		else {
			JPopupMenu popup = new JPopupMenu();
			AbstractAction[] actions = getClickActions(arg0);
			if (actions.length == 0)
				return;			
			for (AbstractAction action : actions) {
				popup.add(action);
			}
			popup.show(this, arg0.getX(), arg0.getY());
		}
		arg0.consume();
	}
	
	public void setFactory(ManeuverGraphFactory factory) {	
		super.setFactory(factory);
		this.factory = factory;
	}
	
	public ManeuverGraphFactory getFactory() {	
		return this.factory;
	}
	
	
	public static void main(String[] args) {
		IndividualPlanGraph graph = new IndividualPlanGraph(new PlanType(new MissionType()));
		graph.setBackground(Color.white);
		GuiUtils.testFrame(new JScrollPane(graph), "Individual Plan Editor");

		ManeuverNode node = new ManeuverNode(new FollowPath());
		node.getUserObject().setInitialManeuver(true);
		node.setPosition(new Point2D.Double(30, 56));
		node.setFinalNode(true);
		graph.addNode(node);
		
		ManeuverNode node2 = new ManeuverNode(new Goto());
		node2.getUserObject().setInitialManeuver(false);
		node2.setPosition(new Point2D.Double(120, 56));
		node2.setFinalNode(false);
		node2.setInitialNode(true);
		graph.addNode(node2);
		
		ManeuverTransition transition = new ManeuverTransition();
		transition.setSourceNodeID(node2.getID());
		transition.setTargetNodeID(node.getID());
		graph.addEdge(transition);
		
		ManeuverTransition transition2 = new ManeuverTransition();
		transition2.setSourceNodeID(node.getID());
		transition2.setTargetNodeID(node2.getID());
		graph.addEdge(transition2);
		
		ManeuverTransition loop = new ManeuverTransition();
		loop.setSourceNodeID(node.getID());
		loop.setTargetNodeID(node.getID());
		graph.addEdge(loop);
		
		ManeuverNode n1 = graph.addNode();
		ManeuverNode n2 = graph.addNode();
		graph.addEdge(n1.getID(), n2.getID());
	}

	public PlanType getPlan() {
		return plan;
	}
}
