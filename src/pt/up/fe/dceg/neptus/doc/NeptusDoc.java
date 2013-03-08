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
 * Aug 29, 2011
 * $Id:: NeptusDoc.java 9615 2012-12-30 23:08:28Z pdias                         $:
 */
package pt.up.fe.dceg.neptus.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to provide documentation about a specific class
 * @author zp
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NeptusDoc {

    /**
     * @return The title for the page of this documentation article
     */
    public String ArticleTitle() default "";
    
    /**
     * @return The html file that holds the documentation about this class
     */
    public String ArticleFilename();
    
    /**
     * @return The section this article / class belongs to.
     */
    public String Section() default "";
}
