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
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

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
                System.out.println(properties[0]);
            };

            public String[] getPropertiesErrors(Property[] properties) {
                return null;
            }
        }, true);
    }
}
