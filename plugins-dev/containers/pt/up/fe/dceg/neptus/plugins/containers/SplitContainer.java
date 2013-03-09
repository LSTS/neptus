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

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;

/**
 * @author ZP
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Console Layout: Split", description="Allows to add 2 components with a flexible split bar", icon="pt/up/fe/dceg/neptus/plugins/containers/layout.png")
public class SplitContainer extends ContainerSubPanel implements ConfigurationListener {

	public enum SplitTypeEnum {Vertical, Horizontal};
	boolean addedLeftComponent = false;
	
	@NeptusProperty(name="Split orientation", description="The split bar's orientation", distribution = DistributionEnum.DEVELOPER)
	public SplitTypeEnum splitType = SplitTypeEnum.Horizontal;

	@NeptusProperty(hidden=true)
	public int splitLocation = 100;
	
	private JSplitPane splitPane = new JSplitPane();
	
	private JComponent pivot = null;
	
	public SplitContainer(ConsoleLayout console) {
		super(console);
		setLayout(new BorderLayout());
		pivot = splitPane;
		splitPane.setOrientation((splitType == SplitTypeEnum.Horizontal)? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
		splitPane.setOneTouchExpandable(true);
	    BasicSplitPaneUI paneUi = (BasicSplitPaneUI)splitPane.getUI();
	    paneUi.getDivider().addMouseListener(new MouseAdapter() {
		    public void mouseReleased(MouseEvent e) {
	            splitLocation = splitPane.getDividerLocation();
	        }
	    });

		add(pivot, BorderLayout.CENTER);
	}
	
	@Override
	public void addSubPanel(SubPanel panel) {
		panel.setBorder(BorderFactory.createEmptyBorder());
		panels.add(panel);
		
		if (!addedLeftComponent) {
			addedLeftComponent = true;
			splitPane.setLeftComponent(panel);
		}
		else
			splitPane.setRightComponent(panel);
	}
	
	@Override
	public void propertiesChanged() {
		splitPane.setOrientation((splitType == SplitTypeEnum.Horizontal)? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
		splitPane.setDividerLocation(splitLocation);
	}
}
