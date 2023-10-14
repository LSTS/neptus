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
 * 2009/06/02
 */
package pt.lsts.neptus.console.plugins;

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

import pt.lsts.neptus.gui.ToolbarButton;

/**
 * @author zp
 * 
 */
public class ButtonPallete extends JToolBar {
    private static final long serialVersionUID = 1L;
    private ToolbarButton selected = null;
	private Color defaultColor = null;

	public ButtonPallete(LayoutManager layout) {
		//super(layout);
		setLayout(layout);
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	}

	/**
	 * @param selected
	 *            the selected to set
	 */
	public void setSelected(ToolbarButton selected) {

		if (defaultColor == null && selected != null)
			defaultColor = selected.getBackground();

		if (getSelected() != null && getSelected() != selected) {
			getSelected().setBackground(defaultColor);
			getSelected().setBorder(BorderFactory.createEmptyBorder());
		}

		if (selected == null) {
			this.selected = selected;
			return;
		}
		else {
			selected.setBackground(Color.red.brighter().brighter().brighter());
			selected.setBorder(BorderFactory.createLineBorder(Color.red
					.darker()));
		}

		this.selected = selected;
	}

	/**
	 * @return the selected
	 */
	public ToolbarButton getSelected() {
		return selected;
	}
}
