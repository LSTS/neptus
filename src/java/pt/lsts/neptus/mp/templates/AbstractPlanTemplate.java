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
 * Author: José Pinto
 * 2010/01/18
 */
package pt.lsts.neptus.mp.templates;

import java.awt.Window;

import javax.swing.ImageIcon;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
			
			AbstractPlanTemplate planTemplate = (AbstractPlanTemplate) templateClass.getDeclaredConstructor().newInstance();
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
