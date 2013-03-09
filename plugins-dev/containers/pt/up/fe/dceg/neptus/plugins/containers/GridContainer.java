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
 */
package pt.up.fe.dceg.neptus.plugins.containers;

import java.awt.GridLayout;

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
@PluginDescription(name="Console Layout: Grid", description="A container that lays out components in a grid", author="ZP", icon="pt/up/fe/dceg/neptus/plugins/containers/layout.png")
public class GridContainer extends ContainerSubPanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Number of cols", description="The number of cols in the grid. Use 0 for infinite.",
            distribution = DistributionEnum.DEVELOPER)
	public int numCols = 2;
	
	@NeptusProperty(name="Number of rows", description="The number of rows in the grid. Use 0 for infinite.",
	        distribution = DistributionEnum.DEVELOPER)
	public int numRows = 0;
	
	public GridContainer(ConsoleLayout console) {
		super(console);
		setLayout(new GridLayout(numRows, numCols));		
	}
	
	@Override
	public void propertiesChanged() {
		setLayout(new GridLayout(numRows, numCols));
		doLayout();
		invalidate();
		revalidate();
	}
}
