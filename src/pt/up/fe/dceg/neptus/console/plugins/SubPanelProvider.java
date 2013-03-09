/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by: Paulo Dias
 * 13-04-2008
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.Dimension;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;

/**
 * @author pdias
 *
 */
public interface SubPanelProvider {

	public ImageIcon getImageIcon();
	public String getName();
	public String getDescription();
	public ToolbarButton getPaletteToolbarButton(Dimension dim);
	public ToolbarButton getPaletteToolbarButton(int width, int height);
	public SubPanel getSubPanel();
}
