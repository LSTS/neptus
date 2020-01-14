/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/10/28
 */
package pt.lsts.neptus.types.comm.protocol;

import java.net.URL;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;

/**
 * @author zp
 *
 */
public class HTTPArgs extends ProtocolArgs {

	{ DEFAULT_ROOT_ELEMENT = "http"; }
	
	protected URL url;
	protected Vector<String> formats = new Vector<String>();
	
	protected boolean loadOk = true;
	
	@Override
	public boolean isLoadOk() {
		return loadOk;
	}
	
    public boolean load(Element elem)
    {
        try
        {
            url = new URL(elem.selectSingleNode("//url").getText());
            String fmts = elem.selectSingleNode("//supported-formats").getText();
            for (String f : fmts.split(",")) {
            	formats.add(f.trim().toLowerCase());
            }
            loadOk = true;
        } catch (Exception e)
        {
            NeptusLog.pub().error(this, e);
            loadOk = false;           
        }
        return loadOk;
    }
    
    public boolean load (String xml)
    {
        try
        {
            Document doc = DocumentHelper.parseText(xml);
            loadOk = load(doc.getRootElement());
        } catch (DocumentException e)
        {
            NeptusLog.pub().error(this, e);
            loadOk = false;            
        }
        return loadOk;
    }
	
    public Document asDocument(String rootElementName)
    {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        root.addElement("url").setText(url.getPath());
        String tmp = "";
        if (formats.size() > 0) {
        	tmp = formats.get(0);
        }
        for (int i = 1; i < formats.size(); i++)
        	tmp += ", "+formats.get(i);
        root.addElement("supported-formats").setText(tmp);
        return document;
    }

	/**
	 * @return the url
	 */
	public final URL getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(URL url) {
		this.url = url;
	}

	/**
	 * @return the formats
	 */
	public final Vector<String> getSupportedFormats() {
		return formats;
	}
	
	public void setSupportedFormats(Vector<String> formats) {
		for (int i = 0; i < formats.size(); i++)
			formats.setElementAt(formats.get(i).trim().toLowerCase(), i);
		this.formats = formats;
	}
	
	public boolean isFormatSupported(String format) {
		return formats.contains(format.trim().toLowerCase());
	}
	
}
