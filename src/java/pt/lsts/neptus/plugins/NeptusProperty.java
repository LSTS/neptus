/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.plugins;

import java.beans.PropertyEditor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.swing.table.TableCellRenderer;

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
     * The units will provide further information on the units (if any) for this field. 
     * It will be used with {@link #description()} to show to the operator.
     * @return The units of the field. It is optional.
     */
    String units() default "";
    
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
     * The (full) class name of the renderer to be used for this property
     */
    Class<? extends TableCellRenderer> rendererClass() default TableCellRenderer.class;
    
    /**
     * Whether this property is to be not editable in user-input dialogs (changed only in code)
     */
    boolean editable() default true;

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
