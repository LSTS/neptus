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

import javax.swing.BoxLayout;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author ZP
 *
 */
@PluginDescription(author="ZP", name="Console Layout: Axis", description="Lays out inner components along the horizontal or vertical axis", icon="pt/lsts/neptus/plugins/containers/layout.png")
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
