/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: ColorMapListRenderer.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapUtils;

public class ColorMapListRenderer extends JLabel implements ListCellRenderer<Object> {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
	public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof ColorMap) {
			ColorMap cmap = (ColorMap)value;
			return new JLabel(cmap.toString(), new ImageIcon(ColorMapUtils.getBar(cmap, ColorMapUtils.HORIZONTAL_ORIENTATION, 60, 14)),
					JLabel.LEFT);			
		}
		return new JLabel("Invalid Color Map");
	}
}