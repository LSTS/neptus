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
 * Author: José Correia
 * Jan 28, 2013
 */
package pt.lsts.neptus.params;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.editor.custom.CustomSystemPropertyEditor;
import pt.lsts.neptus.params.renderer.SystemPropertyRenderer;

/**
 * @author jqcorreia
 * @author pdias
 */
public class SystemProperty extends DefaultProperty implements PropertyChangeListener {
    private static final long serialVersionUID = 1L;

    public static enum Scope {
        GLOBAL(I18n.textmark("global")),
        IDLE(I18n.textmark("idle")),
        PLAN(I18n.textmark("plan")),
        MANEUVER(I18n.textmark("maneuver"));

        private String text;

        Scope(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        public static Scope fromString(String text) {
            if (text != null) {
                for (Scope b : Scope.values()) {
                    if (text.equalsIgnoreCase(b.text)) {
                        return b;
                    }
                }
            }
            return null;
        }
    }

    public static enum Visibility {
        DEVELOPER("developer"),
        USER("user");

        private String text;

        Visibility(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        public static Visibility fromString(String text) {
            if (text != null) {
                for (Visibility b : Visibility.values()) {
                    if (text.equalsIgnoreCase(b.text)) {
                        return b;
                    }
                }
            }
            return null;
        }
    }

    public static enum ValueTypeEnum {
        STRING(I18n.textmark("string")),
        INTEGER(I18n.textmark("integer")),
        REAL(I18n.textmark("real")),
        BOOLEAN(I18n.textmark("boolean"));

        private String text;

        {
            I18n.textmark("list"); // just marking for translation
        }

        ValueTypeEnum(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        public static ValueTypeEnum fromString(String text) {
            if (text != null) {
                for (ValueTypeEnum b : ValueTypeEnum.values()) {
                    if (text.equalsIgnoreCase(b.text)) {
                        return b;
                    }
                }
            }
            return STRING;
        }
    }

    private Object defaultValue = null;

    private Scope scope;
    private Visibility visibility;

    private String categoryId = getCategory();

    private AbstractPropertyEditor editor = null;
    private DefaultCellRenderer renderer = null;

    private CustomSystemPropertyEditor sectionCustomEditor = null;

    private ValueTypeEnum valueType = ValueTypeEnum.STRING;

    private long timeSync = -2;
    private long timeDirty = -1;

    /* (non-Javadoc)
     * @see com.l2fprod.common.propertysheet.DefaultProperty#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Object value) {
        boolean equals = false;
        if (getValue() != null && getValue().equals(value))
            equals = true;
        // System.out.println("<###>##################################### " + getName() + " equals=" + equals + "   " +  getValue() + "   " + value);
        super.setValue(value);
        if (!equals)
            setTimeDirty(System.currentTimeMillis());
    }

    /**
     * @return the defaultValue
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void resetToDefault() {
        if (defaultValue != null)
            setValue(defaultValue);
    }

    /**
     * This is the category not I18n.
     * @return the categoryId
     */
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * This is the category not I18n.
     * @param categoryId the categoryId to set
     */
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * @return the scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * @return the visibility
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * @param visibility the visibility to set
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * @return the editor
     */
    public AbstractPropertyEditor getEditor() {
        return editor;
    }

    /**
     * @param editor the editor to set
     */
    public void setEditor(AbstractPropertyEditor editor) {
        this.editor = editor;
    }

    /**
     * @return the renderer
     */
    public DefaultCellRenderer getRenderer() {
        return renderer;
    }

    /**
     * @param renderer the renderer to set
     */
    public void setRenderer(DefaultCellRenderer renderer) {
        this.renderer = renderer;
        updatePropRenderer();
    }

    /**
     * @return the sectionCustomEditor
     */
    public CustomSystemPropertyEditor getSectionCustomEditor() {
        return sectionCustomEditor;
    }

    /**
     * @param sectionCustomEditor the sectionCustomEditor to set
     */
    public void setSectionCustomEditor(CustomSystemPropertyEditor sectionCustomEditor) {
        this.sectionCustomEditor = sectionCustomEditor;
    }

    /**
     * @return the valueType
     */
    public ValueTypeEnum getValueType() {
        return valueType;
    }

    /**
     * @param valueType the valueType to set
     */
    public void setValueType(ValueTypeEnum valueType) {
        this.valueType = valueType;
    }

    /**
     * @return the timeSync
     */
    public long getTimeSync() {
        return timeSync;
    }

    /**
     * @param timeSync the timeSync to set
     */
    public void setTimeSync(long timeSync) {
        this.timeSync = timeSync;
        this.timeDirty = this.timeSync;
        updatePropRenderer();
    }

    /**
     * @return the timeDirty
     */
    public long getTimeDirty() {
        return timeDirty;
    }

    /**
     * @param timeDirty the timeDirty to set
     */
    public void setTimeDirty(long timeDirty) {
        this.timeDirty = timeDirty;
        updatePropRenderer();
    }

    private void updatePropRenderer() {
        if (this.renderer == null)
            return;

        if (!(renderer instanceof SystemPropertyRenderer))
            return;

        if (timeDirty > timeSync || timeSync <= 0) {
            ((SystemPropertyRenderer) renderer).setPropertyInSync(false);
        }
        else {
            ((SystemPropertyRenderer) renderer).setPropertyInSync(true);
        }
    }

    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (sectionCustomEditor != null) {
            sectionCustomEditor.propertyChange(evt);
            return;
        }

        if(!(evt.getSource() instanceof SystemProperty))
            return;

        try {
            if (editor != null && editor instanceof PropertyChangeListener) {
                ((PropertyChangeListener) editor).propertyChange(evt);
                editor.setValue(getValue());
                setValue(editor.getValue());
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage() + " :: " + getName() + " :: valType='" + getValueType() + "' :: val='" + getValue()
                    + "' :: defaultVal='" + defaultValue + "'", e);
        }
    }
}
