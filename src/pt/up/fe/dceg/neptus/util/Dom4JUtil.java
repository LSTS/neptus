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
 * 2006/11/18
 */
package pt.up.fe.dceg.neptus.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.llf.LsfReport;

/**
 * @author Paulo Dias
 *
 */
public class Dom4JUtil
{
    public static Document elementToDocument (Element elem)
    {
        if (elem == null)
            return null;
        Document doc = DocumentHelper.createDocument();
        doc.setRootElement(elem.createCopy());
        return doc;
    }

    public static Document elementToDocumentCleanFormating (Element elem)
    {
        if (elem == null)
            return null;
        Document doc;
        try
        {
            doc = DocumentHelper.parseText(FileUtil.getAsCompactFormatedXMLString(elem.asXML()));
        }
        catch (DocumentException e)
        {
            NeptusLog.pub().error("Dom4JUtil.elementToDocumentCleanFormating error!");
            doc = null;
        }
        return doc;
    }

    public static Document documentToDocumentCleanFormating (Document doc)
    {
        if (doc == null)
            return null;
        try
        {
            doc = DocumentHelper.parseText(FileUtil.getAsCompactFormatedXMLString(doc.asXML()));
        }
        catch (DocumentException e)
        {
            NeptusLog.pub().error("Dom4JUtil.documentToDocumentCleanFormating error!");
            doc = null;
        }
        return doc;
    }

    
    
    public static org.w3c.dom.Document createEmptyDOMDocument() {
    	try {
			return (new DOMWriter()).write(DocumentHelper.createDocument());
		} catch (DocumentException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    public static Document convertDOMtoDOM4J(org.w3c.dom.Document docDOM) {
    	return (new DOMReader()).read(docDOM);
    }
    

    public static org.w3c.dom.Document convertDOM4JtoDOM(Document docDOM4J) {
    	try {
			return (new DOMWriter()).write(docDOM4J);
		} catch (DocumentException e) {
			e.printStackTrace();
			return null;
		}
    }

    public static void main(String[] args) {
		ConfigFetch.initialize();
    	LsfReport.getLogoDoc();
	}
}
