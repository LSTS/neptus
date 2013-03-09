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
 */
package pt.up.fe.dceg.neptus.plugins;

import java.beans.PropertyEditor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NeptusProperty {

    public enum DistributionEnum {
        DEVELOPER,
        CLIENT;
    }

    public enum LEVEL {
        REGULAR(1),
        ADVANCED(3);

        private int level;

        private LEVEL(int level) {
            this.level = level;
        }

        /**
         * @return the level
         */
        public int getLevel() {
            return level;
        }

    }

    public enum VALIDATOR {
        POSITIVE_INT,
        DECIMAL,
        POSITIVE_DECIMAL,
        STRING,
        CUSTOM_VALIDATOR;
    }

	/**
	 * @return The name of the field (to be presented by the GUI). If not set, the name of the
	 * variable will be used.
	 */
    String name() default "";
	
	/**
	 * The description will provide further information on this field (seen in the configuration panel)
	 * @return The description of the field. If not set, the <b>name</b> parameter will be used.
	 */
    String description() default "";
    
    /**
     * You can use categories to group various properties that are somehow related
     * @return The category for this configuration field
     */
    String category() default "General";
    
    /**
     * The (full) class name of the editor to be used for this property
     */
    Class<? extends PropertyEditor> editorClass() default PropertyEditor.class;
    
    /**
     * Whether this property is to be hidden in user-input dialogs (changed only in code)
     */
    boolean hidden() default false;

    /**
     * Visibility of setting: 
     * - Regular: even clients can edit 
     * - Advanced: only developers can edit
     * @return
     */
    LEVEL userLevel() default LEVEL.ADVANCED;

    /**
     * When make jars distinguishes properties that clients don't have access
     * 
     * @return
     */
    DistributionEnum distribution() default DistributionEnum.CLIENT;
}
