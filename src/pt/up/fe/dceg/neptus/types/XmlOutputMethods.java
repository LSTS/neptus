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
 * 19/Fev/2005
 * $Id:: XmlOutputMethods.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.types;

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
     * {@link pt.up.fe.dceg.neptus.util.FileUtil#saveToFile(String, String)} e afins. Isto porque ao gravar como UTF-8
     * os caracteres são adulterados guardando por exemplo "é" em 2 caracteres (ou seja pega nos dois caracteres UTF-8 e
     * transforma-os em 2 UTF-8s).</i></div>
     * 
     * @return
     */
    public String asXML();

    /**
     * Este problema foi resolvido com a versão 1.6.1 do DOM4J. <div><i>Atenção ao usar este método com o método
     * {@link pt.up.fe.dceg.neptus.util.FileUtil#saveToFile(String, String)} e afins. Isto porque ao gravar como UTF-8
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
