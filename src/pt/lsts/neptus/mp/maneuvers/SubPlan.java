/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * May 11, 2005
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.plan.PlanType;


/**
 * @author zepinto
 */
public class SubPlan extends Maneuver implements IMCSerialization {
	
	protected String planId = "";
	protected String startNodeId = "";
	protected boolean drawSubPlan = true;
	protected PlanElement subplan;

	public void loadManeuverFromXML(String xml) {
		try {
			Document doc = DocumentHelper.parseText(xml);
			setPlanId(doc.selectSingleNode("SubPlan/@planId").getText());
			setStartNodeId(doc.selectSingleNode("SubPlan/@startNodeId")
					.getText());
		} catch (Exception e) {
			NeptusLog.pub().error(this, e);
			return;
		}
	}

	@Override
	public void paintOnMap(Graphics2D g2d, PlanElement planElement,
			StateRenderer2D renderer) {
		NeptusLog.pub().info("<###> "+subplan);
		if (drawSubPlan) {
			if (subplan == null) {
				PlanType plan = planElement.getMissionType().getIndividualPlansList().get(planId);
				
				if (plan != null) {
					subplan = new PlanElement(planElement.getMapGroup(), planElement.getParentMap());
					subplan.setPlan(plan);
					subplan.setTransp2d(0.6);
				}
			}
			
			if (subplan != null) {
				Graphics2D g2 = (Graphics2D) g2d.create();
				g2.setTransform(new AffineTransform());
				subplan.paint(g2, renderer);
			}
			
		}
		super.paintOnMap(g2d, planElement, renderer);
		
	}
	
	@Override
	protected Vector<DefaultProperty> additionalProperties() {
		Vector<DefaultProperty> properties = new Vector<DefaultProperty>();
    	DefaultProperty plan_id = PropertiesEditor.getPropertyInstance("Plan to execute", String.class, getPlanId(), true);
    	
    	NeptusLog.pub().info("<###> "+getMissionType());
    	//String[] planIds = getMissionType().getIndividualPlansList().keySet().toArray(new String[0]);
    	//PropertiesEditor.getPropertyEditorRegitry().registerEditor(plan_id, new ComboEditor(planIds));
    	properties.add(plan_id);
    	
    	DefaultProperty man_id = PropertiesEditor.getPropertyInstance("Starting Maneuver", String.class, getStartNodeId(), true);
    	man_id.setShortDescription("Select starting maneuver inside the plan or leave empty for default");
    	properties.add(man_id);
    	
    	return properties;
	}
	
	@Override
	public void setProperties(Property[] properties) {
		super.setProperties(properties);
    	
    	for (Property p : properties) {
    		if (p.getName().equals("Plan to execute")) {
    			setPlanId((String)p.getValue());
    			subplan = null;
    			
    		}
    		else if (p.getName().equals("Starting Maneuver")) {
    			setStartNodeId((String)p.getValue());
    		}
    	}
	}

	public String getType() {
		return "SubPlan";
	}

	public Object clone() {
		SubPlan u = new SubPlan();
		super.clone(u);
		u.setPlanId(getPlanId());
		u.setStartNodeId(getStartNodeId());
		return u;
	}

	public Document getManeuverAsDocument(String rootElementName) {
	    Document document = DocumentHelper.createDocument();
	    Element root = document.addElement( rootElementName );
	    root.addAttribute("kind", "automatic");
	    root.addAttribute("planId", planId);
	    root.addAttribute("startNodeId", startNodeId);
	    return document;
	}
	
	@Override
	public void parseIMCMessage(IMCMessage message) {
		setPlanId(message.getAsString("plan_id"));
		setStartNodeId(message.getAsString("node_id"));		
	}
	
	public IMCMessage serializeToIMC() {
		IMCMessage man = IMCDefinition.getInstance().create("SubPlan");
		man.setValue("plan_id", getPlanId());
		man.setValue("node_id", getStartNodeId());
		return man;
	}

	/**
	 * @return the planId
	 */
	public String getPlanId() {
		return planId;
	}

	/**
	 * @param planId the planId to set
	 */
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	/**
	 * @return the startNodeId
	 */
	public String getStartNodeId() {
		return startNodeId;
	}

	/**
	 * @param startNodeId the startNodeId to set
	 */
	public void setStartNodeId(String startNodeId) {
		this.startNodeId = startNodeId;
	}
}
