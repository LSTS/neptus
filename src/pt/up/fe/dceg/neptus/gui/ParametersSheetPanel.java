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
 * 25/Jun/2005
 * $Id:: ParametersSheetPanel.java 9921 2013-02-13 19:15:24Z pdias              $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorFactory;
import com.l2fprod.common.propertysheet.PropertyRendererFactory;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

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
