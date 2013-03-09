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
 * Jan 28, 2013
 */
package pt.up.fe.dceg.neptus.plugins.params;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import pt.up.fe.dceg.neptus.plugins.params.renderer.PropertyRenderer;


import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

/**
 * @author jqcorreia
 * @author pdias
 */
public class SystemProperty extends DefaultProperty implements PropertyChangeListener {
    private static final long serialVersionUID = 1L;
    
    public static enum Scope {
        GLOBAL("global"), PLAN("plan"), MANEUVER("maneuver");

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
        USER("user"), DEVELOPER("developer");
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
        STRING("string"), INTEGER("integer"), REAL("real"), BOOLEAN("boolean");
        private String text;

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
//        System.out.println("##################################### " + getName() + " equals=" + equals);
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
    
    private void resetToDefault() {

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
        
        if (!(renderer instanceof PropertyRenderer))
            return;
            
        if (timeDirty > timeSync || timeSync <= 0) {
            ((PropertyRenderer) renderer).setPropertyInSync(false);
        }
        else {
            ((PropertyRenderer) renderer).setPropertyInSync(true);
        }
    }
    
    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(!(evt.getSource() instanceof SystemProperty))
            return;
        
        // SystemProperty sp = (SystemProperty) evt.getSource();
        if (editor != null && editor instanceof PropertyChangeListener) {
//            System.out.println("-------------- 1");
            ((PropertyChangeListener) editor).propertyChange(evt);
            editor.setValue(getValue());
            setValue(editor.getValue());
        }
    }
}
