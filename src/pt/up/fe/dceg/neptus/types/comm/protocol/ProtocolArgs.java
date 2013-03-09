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
 * 19/Jun/2005
 */
package pt.up.fe.dceg.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.types.XmlInputMethods;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;

/**
 * @author Paulo Dias
 */
public abstract class ProtocolArgs implements XmlOutputMethods, XmlInputMethods {

    protected String DEFAULT_ROOT_ELEMENT = "protocol";

    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

}
