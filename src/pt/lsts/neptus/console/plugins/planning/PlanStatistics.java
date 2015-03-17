/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 10/07/2010
 */
package pt.lsts.neptus.console.plugins.planning;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Plan Statistics", category = CATEGORY.PLANNING)
public class PlanStatistics extends ConsolePanel implements IEditorMenuExtension {


    NumberFormat format = GuiUtils.getNeptusDecimalFormat(0);
	boolean initCalled = false;
	
	@NeptusProperty(name="Maximum Top Speed (100%)")
	public double maxSpeed = 1.3;
	
	@NeptusProperty(name="Maximum Number of RPMs")
	public double maxRpms = 1000;

    /**
     * @param console
     */
    public PlanStatistics(ConsoleLayout console) {
        super(console);
    }
	
	@Override
	public void initSubPanel() {
		if (initCalled)
			return;
		initCalled = true;
		
		setVisibility(false);
		
		Vector<IMapPopup> r = getConsole().getSubPanelsOfInterface(IMapPopup.class);
		for (IMapPopup str2d : r) {
			str2d.addMenuExtension(this);
		}
	}
	
	@Override
	public Collection<JMenuItem> getApplicableItems(LocationType loc,
			IMapPopup source) {
		Vector<JMenuItem> items = new Vector<JMenuItem>();
		
		if (getConsole().getPlan() != null) {
//			JMenu menu = new JMenu("Plan Statistics");
			PlanType plan = getConsole().getPlan();
//            Vector<LocationProvider> mans = PlanUtil.getLocationsAsSequence(plan);
//            menu.add("length: " + MathMiscUtils.parseToEngineeringNotation(PlanUtil.getPlanLength(mans), 2) + "m");
//            menu.add("est. time: " + PlanUtil.estimatedTime(mans, maxSpeed, maxRpms));
//            menu.add("max depth: " + format.format(PlanUtil.getMaxPlannedDepth(mans)) + " m");
//            menu.add("# maneuvers: " + PlanUtil.numManeuvers(plan));
//            menu.addSeparator();
//            menu.add("using max speed: " + maxSpeed + "m/s (" + maxRpms + "RPM)");

            JMenu menu = PlanUtil.getPlanStatisticsAsJMenu(plan, I18n.text("Active Plan Statistics"), maxSpeed, maxRpms);
            items.add(menu);
			return items;
		}
		return null;
	}

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }	
}
