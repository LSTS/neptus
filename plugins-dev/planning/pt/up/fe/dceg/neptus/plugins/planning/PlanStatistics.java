/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 10/07/2010
 * $Id:: PlanStatistics.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension;
import pt.up.fe.dceg.neptus.planeditor.IMapPopup;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.PlanUtil;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Plan Statistics", category = CATEGORY.PLANNING)
public class PlanStatistics extends SimpleSubPanel implements IEditorMenuExtension {


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
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }	
}
