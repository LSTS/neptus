/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/09/16
 */
package pt.lsts.neptus.console.plugins.containers;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author ZP
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Console Layout: Split", description = "Allows to add 2 components with a flexible split bar", 
    icon = "pt/lsts/neptus/plugins/containers/layout.png", experimental = true)
public class SplitContainer extends ContainerSubPanel implements ConfigurationListener {

	public enum SplitTypeEnum {Vertical, Horizontal};
	boolean addedLeftComponent = false;
	
	@NeptusProperty(name="Split orientation", description="The split bar's orientation", distribution = DistributionEnum.DEVELOPER)
	public SplitTypeEnum splitType = SplitTypeEnum.Horizontal;

	@NeptusProperty(editable = false)
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
	
	   /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ContainerSubPanel#isAddSubPanelToPanelOrLetExtensionDoIt()
     */
    @Override
    protected boolean isAddSubPanelToPanelOrLetExtensionDoIt() {
        return false;
    }
    
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.console.ContainerSubPanel#addSubPanelExtra(pt.lsts.neptus.console.ConsolePanel)
	 */
	@Override
	public boolean addSubPanelExtra(ConsolePanel panel) {
		panel.setBorder(BorderFactory.createEmptyBorder());
		
		if (!addedLeftComponent) {
			addedLeftComponent = true;
			splitPane.setLeftComponent(panel);
			return true;
		}
		else if (splitPane.getRightComponent() == null){
			splitPane.setRightComponent(panel);
            return true;
		}
		
        return false;
	}
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.console.ContainerSubPanel#removeSubPanelExtra(pt.lsts.neptus.console.ConsolePanel)
	 */
	@Override
	protected void removeSubPanelExtra(ConsolePanel panel) {
	    if (splitPane.getLeftComponent() == panel) {
	        splitPane.setLeftComponent(splitPane.getRightComponent());
	        if (splitPane.getLeftComponent() == null)
	            addedLeftComponent = false;
	    }
	    else if (splitPane.getRightComponent() == panel) {
            splitPane.setRightComponent(null);
	    }
	}
	
	@Override
	public void propertiesChanged() {
		splitPane.setOrientation((splitType == SplitTypeEnum.Horizontal)? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
		splitPane.setDividerLocation(splitLocation);
	}
}
