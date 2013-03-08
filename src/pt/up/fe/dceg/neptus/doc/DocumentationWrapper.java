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
 * $Id:: DocumentationWrapper.java 9615 2012-12-30 23:08:28Z pdias              $:
 */
package pt.up.fe.dceg.neptus.doc;

import pt.up.fe.dceg.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
public class DocumentationWrapper implements DocumentationProvider {

    String title, filename, section;
    
    public DocumentationWrapper(Class<?> c) {
        
        NeptusDoc doc = c.getAnnotation(NeptusDoc.class);
        if (doc != null) {
            if (doc.ArticleTitle().length() > 0)
                title = doc.ArticleTitle();
            else 
                title = c.getSimpleName();
            filename = doc.ArticleFilename();
            section = doc.Section();
            return;
        }
        
        PluginDescription pdesc = c.getAnnotation(PluginDescription.class);
        if (pdesc != null) {
            title = pdesc.name();
            filename = pdesc.documentation();       
            section = pdesc.category().toString();
        }
    }
    
    public DocumentationWrapper(Object o) {
        this(o.getClass());
    }
    
    @Override
    public String getArticleTitle() {
        return title;
    }
    
    @Override
    public String getDocumentationFile() {
        return filename;
    }
    
    @Override
    public String getSectionName() {
        return section;
    }
}
