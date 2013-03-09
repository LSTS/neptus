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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pt.up.fe.dceg.neptus.i18n.I18n;

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

    CATEGORY category() default CATEGORY.UNSORTED;
	boolean experimental() default false;
	
}
