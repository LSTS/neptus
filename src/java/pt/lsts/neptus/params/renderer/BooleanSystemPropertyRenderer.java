/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * Mar 2, 2013
 */
package pt.lsts.neptus.params.renderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;


@SuppressWarnings("serial")
public class BooleanSystemPropertyRenderer extends SystemPropertyRenderer {
    private JCheckBox checkBox = new JCheckBox();
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.params.ConfigurationManager.PropertyRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        checkBox.setSelected(Boolean.TRUE.equals(!(value instanceof Boolean) ? Boolean.parseBoolean(value.toString()) : value));
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        checkBox.setBackground(comp.getBackground());
        return checkBox;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.params.renderer.SystemPropertyRenderer#clone()
     */
    @Override
    public BooleanSystemPropertyRenderer clone() throws CloneNotSupportedException {
        BooleanSystemPropertyRenderer clone = (BooleanSystemPropertyRenderer) super.clone();
        clone.checkBox = new JCheckBox();
        return clone;
    }
}