/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 2010/01/18
 */
package pt.up.fe.dceg.neptus.mp.templates;

import java.awt.Window;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zepinto
 *
 */

public abstract class AbstractPlanTemplate implements PropertiesProvider {
    public boolean editProperties(Window parentComp, MissionType mission) {
        return PropertiesEditor.editProperties(this, parentComp, true);
    }
    
	protected MissionType mission = null;
	/**
	 * @return the mission
	 */
	public MissionType getMission() {
		return mission;
	}

	/**
	 * @param mission the mission to set
	 */
	public void setMission(MissionType mission) {
		this.mission = mission;
	}

	protected MapGroup map = null;
	
	public abstract PlanType generatePlan() throws Exception;
		
	@Override
	public DefaultProperty[] getProperties() {
		return PluginUtils.getPluginProperties(this);
	}
	
	@Override
	public void setProperties(Property[] properties) {
		PluginUtils.setPluginProperties(this, properties);
	}
	
	@Override
	public String getPropertiesDialogTitle() {
		return PluginUtils.getPluginName(this.getClass())+" parameters";
	}
	
	@Override
	public String[] getPropertiesErrors(Property[] properties) {
		return PluginUtils.validatePluginProperties(this, properties);
	}
	
	public String getName() {
		return PluginUtils.getPluginName(this.getClass());
	}
	
	public String getDescription() {
		return PluginUtils.getPluginDescription(this.getClass());
	}
	
	public ImageIcon getImageIcon() {
		return ImageUtils.getIcon(PluginUtils.getPluginIcon(this.getClass()));
	}
	
	public static PlanType addTemplateToMission(Window parentComp, MissionType mt, Class<?> templateClass) {
		try {
			
			AbstractPlanTemplate planTemplate = (AbstractPlanTemplate) templateClass.newInstance();
			planTemplate.mission = mt;
			boolean canceled = planTemplate.editProperties(parentComp, mt);
			
			if (canceled)
				return null;
			
			PlanType plan = planTemplate.generatePlan();
			return plan;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		MissionType mt = new MissionType();
		addTemplateToMission(null, mt, InfiniteRectTemplate.class);
	}
}
