/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/10/28
 */
package pt.up.fe.dceg.neptus.types.comm.protocol;

import java.net.URL;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;

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
