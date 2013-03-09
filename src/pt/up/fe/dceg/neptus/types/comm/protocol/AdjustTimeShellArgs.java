/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * 28/Jun/2005
 */
package pt.up.fe.dceg.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.Dom4JUtil;

/**
 * @author Paulo Dias
 */
public class AdjustTimeShellArgs 
extends ProtocolArgs
{
    public static final String DEFAULT_ROOT_ELEMENT = "time-shell";

    protected boolean setSeconds  = true;
    protected boolean setYear = true;
    protected boolean use2DigitYear = false;
    protected boolean useHwClock = true;
    

    private Document doc = null;
    private boolean isLoadOk = true;

    /**
     * 
     */
    public AdjustTimeShellArgs()
    {
        super();
    }

    /**
     * 
     */
    public AdjustTimeShellArgs(String xml)
    {
        //super();
        load(xml);
    }


    

	/**
	 * @return the setSeconds
	 */
	public boolean isSetSeconds() {
		return setSeconds;
	}

	/**
	 * @param setSeconds the setSeconds to set
	 */
	public void setSetSeconds(boolean setSeconds) {
		this.setSeconds = setSeconds;
	}

	/**
	 * @return the setYear
	 */
	public boolean isSetYear() {
		return setYear;
	}

	/**
	 * @param setYear the setYear to set
	 */
	public void setSetYear(boolean setYear) {
		this.setYear = setYear;
	}

	/**
	 * @return the use2DigitYear
	 */
	public boolean isUse2DigitYear() {
		return use2DigitYear;
	}

	/**
	 * @param use2DigitYear the use2DigitYear to set
	 */
	public void setUse2DigitYear(boolean use2DigitYear) {
		this.use2DigitYear = use2DigitYear;
	}

	/**
	 * @return the useHwClock
	 */
	public boolean isUseHwClock() {
		return useHwClock;
	}

	/**
	 * @param useHwClock the useHwClock to set
	 */
	public void setUseHwClock(boolean useHwClock) {
		this.useHwClock = useHwClock;
	}

	public boolean isLoadOk()
    {
        return isLoadOk;
    }

    public boolean load(Element elem)
    {
        try
        {
            doc = Dom4JUtil.elementToDocument(elem);
            if (doc == null)
            {
                isLoadOk = false;
                return false;
            }
            
            Node node = doc.selectSingleNode("//set-seconds");
            if (node != null)
            {
            	setSeconds = Boolean.parseBoolean(node.getText());
            }

            node = doc.selectSingleNode("//set-year");
            if (node != null)
            {
            	setYear = Boolean.parseBoolean(node.getText());
            }

            node = doc.selectSingleNode("//use-2-digit-year");
            if (node != null)
            {
            	use2DigitYear = Boolean.parseBoolean(node.getText());
            }

            node = doc.selectSingleNode("//use-hwclock");
            if (node != null)
            {
            	useHwClock = Boolean.parseBoolean(node.getText());
            }

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
        
        root.addElement("set-seconds").setText(Boolean.toString(isSetSeconds()));
        root.addElement("set-year").setText(Boolean.toString(isSetYear()));
        root.addElement("use-2-digit-year").setText(Boolean.toString(isUse2DigitYear()));
        root.addElement("use-hwclock").setText(Boolean.toString(isUseHwClock()));
        
        return document;
    }

}
