/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 2006/11/18
 */
package pt.lsts.neptus.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LsfReport;

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
