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
 * May 11, 2005
 * $Id:: SubPlan.java 9880 2013-02-07 15:23:52Z jqcorreia                 $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.map.PlanElement;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;


/**
 * @author zepinto
 */
public class SubPlan extends Maneuver implements IMCSerialization {

	
	protected String planId = "";
	protected String startNodeId = "";
	protected boolean drawSubPlan = true;
	protected PlanElement subplan;
	
	

	public void loadFromXML(String xml) {
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
		System.out.println(subplan);
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
    	
    	System.out.println(getMissionType());
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

	public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
		endManeuver();
		//JOptionPane.showMessageDialog(new JFrame(), "<html>The current maneuver is unconstrained (tele-operation)<br>"+
		//		"Click to proceed to the next maneuver", "Unconstrained Maneuver", JOptionPane.INFORMATION_MESSAGE
		//	);
		return lastVehicleState;		
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
	
	public IMCMessage serializeToIMC()
	{
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
