/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 25/Jun/2005
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorFactory;
import com.l2fprod.common.propertysheet.PropertyRendererFactory;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.neptus.gui.objparams.ParametersPanel;

/**
 * @author ZP
 */
@SuppressWarnings("serial")
public class ParametersSheetPanel extends ParametersPanel {

    private PropertySheetPanel psp = new PropertySheetPanel();

    public ParametersSheetPanel(PropertiesProvider provider) {
        this(provider.getProperties(), PropertiesEditor.getPropertyEditorRegistry(), PropertiesEditor
                .getPropertyRendererRegistry());
    }

    public ParametersSheetPanel(Property[] properties) {
        this(properties, PropertiesEditor.getPropertyEditorRegistry(), PropertiesEditor.getPropertyRendererRegistry());
    }

    public ParametersSheetPanel(Property[] properties, PropertyEditorFactory pef) {
        this(properties, pef, PropertiesEditor.getPropertyRendererRegistry());
    }

    public ParametersSheetPanel(Property[] properties, PropertyEditorFactory pef, PropertyRendererFactory prf) {
        psp.setEditorFactory(pef);
        if (prf != null)
            psp.setRendererFactory(prf);
        if (properties != null)
            psp.setProperties(properties);

        this.setLayout(new BorderLayout(0, 0));
        this.add(psp, BorderLayout.CENTER);
        this.setMinimumSize(new Dimension(400, 400));
        this.setPreferredSize(getMinimumSize());
        psp.setToolBarVisible(false);
    }

    public String getErrors() {
        return null;
    }

    public Property[] getProperties() {
        return psp.getProperties();
    }

    public static void main(String[] args) {
    }
}
