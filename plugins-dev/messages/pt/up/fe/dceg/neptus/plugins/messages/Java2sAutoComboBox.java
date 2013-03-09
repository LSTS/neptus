/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Jan 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.messages;

/**
 * @author jqcorreia
 *
 */
import java.awt.event.ItemEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxEditor;

public class Java2sAutoComboBox extends JComboBox<String> {
    private static final long serialVersionUID = 1L;

    private class AutoTextFieldEditor extends BasicComboBoxEditor {

        private Java2sAutoTextField getAutoTextFieldEditor() {
            return (Java2sAutoTextField) editor;
        }

        AutoTextFieldEditor(java.util.List<String> list) {
            editor = new Java2sAutoTextField(list, Java2sAutoComboBox.this);
        }
    }

    public Java2sAutoComboBox(java.util.List<String> list) {
        isFired = false;
        autoTextFieldEditor = new AutoTextFieldEditor(list);
        setEditable(true);
        setModel(new DefaultComboBoxModel<String>(list.toArray(new String[] {})) {

            private static final long serialVersionUID = 1L;

            protected void fireContentsChanged(Object obj, int i, int j) {
                if (!isFired)
                    super.fireContentsChanged(obj, i, j);
            }

        });
        setEditor(autoTextFieldEditor);
    }

    public boolean isCaseSensitive() {
        return autoTextFieldEditor.getAutoTextFieldEditor().isCaseSensitive();
    }

    public void setCaseSensitive(boolean flag) {
        autoTextFieldEditor.getAutoTextFieldEditor().setCaseSensitive(flag);
    }

    public boolean isStrict() {
        return autoTextFieldEditor.getAutoTextFieldEditor().isStrict();
    }

    public void setStrict(boolean flag) {
        autoTextFieldEditor.getAutoTextFieldEditor().setStrict(flag);
    }

    public java.util.List<String> getDataList() {
        return autoTextFieldEditor.getAutoTextFieldEditor().getDataList();
    }

    public void setDataList(java.util.List<String> list) {
        autoTextFieldEditor.getAutoTextFieldEditor().setDataList(list);
        setModel(new DefaultComboBoxModel<String>(list.toArray(new String[] {})));
    }

    void setSelectedValue(Object obj) {
        if (isFired) {
            return;
        }
        else {
            isFired = true;
            setSelectedItem(obj);
            fireItemStateChanged(new ItemEvent(this, 701, selectedItemReminder, 1));
            isFired = false;
            return;
        }
    }

    protected void fireActionEvent() {
        if (!isFired)
            super.fireActionEvent();
    }

    private AutoTextFieldEditor autoTextFieldEditor;

    private boolean isFired;

}
