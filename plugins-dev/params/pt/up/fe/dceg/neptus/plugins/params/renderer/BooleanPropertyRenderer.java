/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * Mar 2, 2013
 * $Id:: BooleanPropertyRenderer.java 10068 2013-03-05 03:31:47Z robot          $:
 */
package pt.up.fe.dceg.neptus.plugins.params.renderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;


@SuppressWarnings("serial")
public class BooleanPropertyRenderer extends PropertyRenderer {
    private JCheckBox checkBox = new JCheckBox();
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.params.ConfigurationManager.PropertyRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        checkBox.setSelected(Boolean.TRUE.equals(!(value instanceof Boolean) ? Boolean.parseBoolean(value.toString()) : value));
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        checkBox.setBackground(comp.getBackground());
        return checkBox;
    }
}