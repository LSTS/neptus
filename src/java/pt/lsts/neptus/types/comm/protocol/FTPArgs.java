/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 19/Jun/2005
 */
package pt.lsts.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;

/**
 * @author Paulo Dias
 */
public class FTPArgs
extends ProtocolArgs
{
    protected static final String DEFAULT_ROOT_ELEMENT = "ftp";
    
    public static final String BINARY_TRANSFER_MODE = "binary";
    public static final String ASCII_TRANSFER_MODE = "ascii";

    public static final String ACTIVE_CONNECTION_MODE = "active";
    public static final String PASV_CONNECTION_MODE = "pasv";


    protected String transferMode = BINARY_TRANSFER_MODE;
    protected String connectionMode = PASV_CONNECTION_MODE;
    
    private Document doc = null;
    private boolean isLoadOk = true;


    /**
     * 
     */
    public FTPArgs()
    {
        super();
    }

    /**
     * 
     */
    public FTPArgs(String xml)
    {
        //super();
        load(xml);
    }

    public boolean isLoadOk()
    {
        return isLoadOk;
    }

    public boolean load(Element elem)
    {
        try
        {
            transferMode = elem.selectSingleNode("//transfer/@mode").getText();
            connectionMode = elem.selectSingleNode("//connection/@mode").getText();
            
            isLoadOk = true;

        } catch (Exception e)
        {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
        return true;
    }
    /**
     * @param xml
     */
    public boolean load (String xml)
    {
        try
        {
            doc = DocumentHelper.parseText(xml);
            return load(doc.getRootElement());
        } catch (DocumentException e)
        {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
    }

    /**
     * @return Returns the transferMode.
     */
    public String getTransferMode()
    {
        return transferMode;
    }
    /**
     * @param transferMode The transferMode to set.
     */
    public void setTransferMode(String transferMode)
    {
        this.transferMode = transferMode;
    }

    /**
     * @return Returns the connectionMode.
     */
    public String getConnectionMode()
    {
        return connectionMode;
    }
    /**
     * @param connectionMode The connectionMode to set.
     */
    public void setConnectionMode(String connectionMode)
    {
        this.connectionMode = connectionMode;
    }

    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    public String asXML(String rootElementName)
    {
        String result = "";        
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName)
    {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName)
    {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        
        root.addElement("transfer").addAttribute("mode", transferMode);
        root.addElement("connection").addAttribute("mode", connectionMode);
        
        return document;
    }
}
