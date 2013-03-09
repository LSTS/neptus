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
 * 16/Jan/2005
 */
package pt.up.fe.dceg.neptus.types.vehicle;

import java.util.LinkedHashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.misc.FileType;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author Paulo
 *
 */
public class TemplateFileVehicle extends FileType
{
    protected String parametersToPass = "";
    protected String outputFileName = "";
    
    protected LinkedHashMap<String, String> parametersToPassList = 
    	new LinkedHashMap<String, String>();
    
    /**
     * 
     */
    public TemplateFileVehicle()
    {
        super();
        this.setType("xslt");
    }

    /**
     * @param xml
     */
    public TemplateFileVehicle(String xml)
    {
        //super(xml);
        load(xml);
        this.setType("xslt");
    }

    /**
     * @see pt.up.fe.dceg.neptus.types.misc.FileType#load(java.lang.String)
     */
    @SuppressWarnings("unchecked")
	public boolean load(String xml)
    {
        if (!super.load(xml))
            return false;

        try
        {
            //doc = DocumentHelper.parseText(xml);
            Node nd = doc.selectSingleNode("/file/parameters-to-pass");
            if (nd != null)
            {
                setParametersToPass(""); //nd.asXML());
                List<Node> paramNodesList = nd.selectNodes("./param");
                for (Node param : paramNodesList)
                {
                	String name = param.selectSingleNode("name").getText();
                	String value = param.selectSingleNode("value").getText();
                	parametersToPassList.put(name, value);
                	setParametersToPass(getParametersToPass() + " " + name + "=" + value);
                }
            }
            
            this.setOutputFileName(doc.selectSingleNode("/file/output-file-name").getText());
        }
        catch (Exception e)
        {
            NeptusLog.pub().error(this, e);
            return false;
        }

        return true;
    }

    /**
     * @return Returns the parametersToPass.
     */
    public String getParametersToPass()
    {
        return parametersToPass;
    }
    /**
     * @param parametersToPass The parametersToPass to set.
     */
    public void setParametersToPass(String parametersToPass)
    {
        this.parametersToPass = parametersToPass;
    }
    
    
    /**
     * @return
     */
    public LinkedHashMap<String, String> getParametersToPassList() 
    {
		return parametersToPassList;
	}

	/**
     * @return Returns the outputFileName.
     */
    public String getOutputFileName()
    {
        return outputFileName;
    }
    /**
     * @param outputFileName The outputFileName to set.
     */
    public void setOutputFileName(String outputFileName)
    {
        this.outputFileName = outputFileName;
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
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName)
    {
        Document document = super.asDocument(rootElementName);
        Element root = document.getRootElement();
        
        if (!parametersToPass.equalsIgnoreCase(""))
        {
            //root.addElement( "parameters-to-pass" ).addText(getParametersToPass());
        	Element paramToPass = root.addElement( "parameters-to-pass" );
        	for (String key : getParametersToPassList().keySet())
        	{
        		String value = getParametersToPassList().get(key);
        		Element paramT = paramToPass.addElement("param");
        		paramT.addElement("name").addText(key);
        		paramT.addElement("value").addText(value);
        	}
        	
        }
        if (originalFilePath == "")
            root.addElement("output-file-name").addText(getOutputFileName());
        else
            root.addElement("output-file-name").addText(
                    FileUtil.relativizeFilePathAsURI(originalFilePath, this
                            .getOutputFileName()));
        
        return document;
    }

}
