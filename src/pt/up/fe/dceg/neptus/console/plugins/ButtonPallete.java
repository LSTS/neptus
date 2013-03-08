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
 * 2009/06/02
 * $Id:: ButtonPallete.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

import pt.up.fe.dceg.neptus.gui.ToolbarButton;

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
