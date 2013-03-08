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
 * $Id:: FTPArgs.java 9616 2012-12-30 23:23:22Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;

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
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    public String asXML(String rootElementName)
    {
        String result = "";        
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName)
    {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
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
