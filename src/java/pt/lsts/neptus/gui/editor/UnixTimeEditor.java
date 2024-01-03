/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;

/**
 * @author zp
 *
 */
public class UnixTimeEditor extends AbstractPropertyEditor {

    JPanel panel = new JPanel(new BorderLayout());
    JSpinner.DateEditor deditor;
    JSpinner timeSpinner;

    public UnixTimeEditor() {
        timeSpinner = new JSpinner( new SpinnerDateModel() );
        deditor = new JSpinner.DateEditor(timeSpinner, "yyyy-MMM-dd HH:mm:ss");
        timeSpinner.setEditor(deditor);
        timeSpinner.setValue(new Date());
        editor = timeSpinner;
    }

    @Override
    public Object getValue() {
        return (((Date)timeSpinner.getValue()).getTime()) / 1000;
    }

    @Override
    public void setValue(Object value) {
        timeSpinner.setValue(new Date((Long)value * 1000));
    }

    public static void main(String[] args) {
        final DefaultProperty p = PropertiesEditor.getPropertyInstance("test", Long.class, System.currentTimeMillis()/1000, true);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(p, UnixTimeEditor.class);

        PropertiesEditor.editProperties(new PropertiesProvider() {
            public DefaultProperty[] getProperties() {

                return new DefaultProperty[] {p};
            };

            public String getPropertiesDialogTitle() {
                return "testing";
            };

            public void setProperties(Property[] properties) {
                NeptusLog.pub().info("<###> "+properties[0]);
            };

            public String[] getPropertiesErrors(Property[] properties) {
                return null;
            }
        }, true);
    }
}
