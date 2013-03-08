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
 * $Id:: DocumentationProvider.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.doc;

/**
 * This Interface is provided by classes that provide documentation to the end-user
 * @author zp
 */
public interface DocumentationProvider {

    /**
     * Retrieve the name of the section this documentation belongs to
     * @return The name of the section this documentation article belongs to
     */
    public String getSectionName();
        
    /**
     * Retrieve the title to be used in the article
     * @return The name of this article
     */
    public String getArticleTitle();
    
    
    /**
     * Retrieve the name of the html file that provides documentation for this class
     * @return The filename of the article (inside doc/manual) providing documentation for this class
     */
    public String getDocumentationFile();
}
