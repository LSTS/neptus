/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pt.lsts.neptus.i18n.I18n;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginDescription {

    public enum CATEGORY {
        PLANNING(I18n.text("Planning")),
        WEB_PUBLISHING(I18n.text("Web Publishing")),
        INTERFACE(I18n.text("Interface")),
        COMMUNICATIONS(I18n.text("Communications")),
        UNSORTED(I18n.text("Neptus Plug-ins"));
        private String name;

        private CATEGORY(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    String name() default "";
	String author() default "LSTS-FEUP";
	String description() default "";
	String version() default "1.0";
	String documentation() default "";
	String icon() default "";
	boolean active() default true;

    CATEGORY category() default CATEGORY.UNSORTED;
	boolean experimental() default false;
	
}
