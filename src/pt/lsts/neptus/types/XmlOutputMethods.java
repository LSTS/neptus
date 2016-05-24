/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * 19/Fev/2005
 */
package pt.lsts.neptus.types;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Interface to be used if a class wants to have output as as XML string or as DOM4J Document or Element.
 * 
 * Here is an example on how to use this interface:
 * 
 * <pre>
 *    protected static final String DEFAULT_ROOT_ELEMENT = "root-elem";
 * 
 *    public String asXML()
 *    {
 *        String rootElementName = DEFAULT_ROOT_ELEMENT;
 *        return asXML(rootElementName);
 *    }
 * 
 *    
 *    public String asXML(String rootElementName)
 *    {
 *        String result = "";        
 *        Document document = asDocument(rootElementName);
 *        result = document.asXML();
 *        return result;
 *    }
 * 
 *    public Element asElement()
 *    {
 *        String rootElementName = DEFAULT_ROOT_ELEMENT;
 *        return asElement(rootElementName);
 *    }
 * 
 *    public Element asElement(String rootElementName)
 *    {
 *        return (Element) asDocument(rootElementName).getRootElement().detach();
 *   }
 * 
 *    public Document asDocument()
 *    {
 *        String rootElementName = DEFAULT_ROOT_ELEMENT;
 *        return asDocument(rootElementName);
 *    }
 * 
 *    public Document asDocument(String rootElementName)
 *    {
 *        Document document = DocumentHelper.createDocument();
 *        Element root = document.addElement( rootElementName );
 *        
 *        root.addComment(ConfigFetch.getSaveAsCommentForXML());
 *        
 *        ...
 *        
 *        return document;
 *    }
 * </pre>
 * 
 * As you notice only one has to be implemented, the others just call each other.
 * 
 * @version 0.1 2005-02-19
 * @author Paulo Dias
 */
public interface XmlOutputMethods {
    /**
     * Este problema foi resolvido com a versão 1.6.1 do DOM4J. <div><i>Atenção ao usar este método com o método
     * {@link pt.lsts.neptus.util.FileUtil#saveToFile(String, String)} e afins. Isto porque ao gravar como UTF-8
     * os caracteres são adulterados guardando por exemplo "é" em 2 caracteres (ou seja pega nos dois caracteres UTF-8 e
     * transforma-os em 2 UTF-8s).</i></div>
     * 
     * @return
     */
    public String asXML();

    /**
     * Este problema foi resolvido com a versão 1.6.1 do DOM4J. <div><i>Atenção ao usar este método com o método
     * {@link pt.lsts.neptus.util.FileUtil#saveToFile(String, String)} e afins. Isto porque ao gravar como UTF-8
     * os caracteres são adulterados guardando por exemplo "é" em 2 caracteres (ou seja pega nos dois caracteres UTF-8 e
     * transforma-os em 2 UTF-8s).</i></div>
     * 
     * @param rootElementName
     * @return
     */
    public String asXML(String rootElementName);

    /**
     * @return
     */
    public Element asElement();

    /**
     * @param rootElementName
     * @return
     */
    public Element asElement(String rootElementName);

    /**
     * @return
     */
    public Document asDocument();

    /**
     * @param rootElementName
     * @return
     */
    public Document asDocument(String rootElementName);
}
