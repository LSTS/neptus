/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/09/16
 * $Id:: BoxLayoutContainer.java 9640 2013-01-02 18:24:18Z pdias          $:
 */
package pt.up.fe.dceg.neptus.plugins.containers;

import javax.swing.BoxLayout;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;

/**
 * @author ZP
 *
 */
@PluginDescription(author="ZP", name="Console Layout: Axis", description="Lays out inner components along the horizontal or vertical axis", icon="pt/up/fe/dceg/neptus/plugins/containers/layout.png")
public class BoxLayoutContainer extends ContainerSubPanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;

    public enum AxisEnum {Horizontal, Vertical};
    
	@NeptusProperty(name="Layout Axis", description="The axis to follow when laying out components", distribution = DistributionEnum.DEVELOPER)
	public AxisEnum axis = AxisEnum.Horizontal;
		
	public BoxLayoutContainer(ConsoleLayout console) {
		super(console);
		int a = (axis == AxisEnum.Horizontal)? BoxLayout.LINE_AXIS : BoxLayout.PAGE_AXIS;
		setLayout(new BoxLayout(this, a));		
	}
	
	@Override
	public void propertiesChanged() {
		int a = (axis == AxisEnum.Horizontal)? BoxLayout.LINE_AXIS : BoxLayout.PAGE_AXIS;
		setLayout(new BoxLayout(this, a));
		doLayout();
		invalidate();
		revalidate();
	}	
}
