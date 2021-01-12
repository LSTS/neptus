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
 * 19/Jun/2005
 */
package pt.lsts.neptus.types.comm;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.comm.protocol.FTPArgs;
import pt.lsts.neptus.types.comm.protocol.IMCArgs;
import pt.lsts.neptus.types.comm.protocol.ProtocolArgs;

/**
 * @author Paulo Dias
 * @author ZP
 */
public class CommMean implements XmlOutputMethods {
    protected static final String DEFAULT_ROOT_ELEMENT = "comm-mean";

    public static final String IMC = "imc";
    public static final String HTTP = "http";
    public static final String IRIDIUM = "iridium";
    public static final String GSM = "gsm";

    protected String name = "";
    protected String type = "";
    protected String hostAddress = "127.0.0.1";
    
    protected LinkedList<String> protocols = new LinkedList<String>();
    protected LinkedHashMap<String, ProtocolArgs> protocolsArgs = new LinkedHashMap<String, ProtocolArgs>();
    
    //Latency
    protected String latency = "";
    protected String latencyUnit = "";
    
    protected boolean isActive = true;
    
    private Document doc = null;


    /**
     * 
     */
    public CommMean() {
        super();
    }

    /**
     * 
     */
    public CommMean(String xml) {
        // super();
        load(xml);
    }

    /**
     * @param xml
     */
    public boolean load(String xml) {
        try {
            doc = DocumentHelper.parseText(xml);
            
            name = doc.selectSingleNode("//name").getText();
            type = doc.selectSingleNode("//type").getText();
            
            hostAddress = doc.selectSingleNode("//host-address").getText();
            
            Node nd = doc.selectSingleNode("//protocols");
            String protocolsStr = nd.getText();
            StringTokenizer strt = new StringTokenizer(protocolsStr, " ");
            while (strt.hasMoreTokens()) {
                protocols.add(strt.nextToken());
            }
        	
        	latency = doc.selectSingleNode("//latency/@value").getText();
        	nd = doc.selectSingleNode("//latency/@unit");
        	if (nd != null)
        	    latencyUnit = nd.getText();
        	
        	List<?> lt = doc.selectNodes("//protocols-args/*");
        	Iterator<?> it = lt.iterator();
            while (it.hasNext()) {
                Node ndP = (Node) it.next();
                String nodeName = ndP.getName();
                if (nodeName.equalsIgnoreCase("ftp")) {
                    FTPArgs ftpA = new FTPArgs(ndP.asXML());
                    protocolsArgs.put("ftp", ftpA);
                }
                else if (nodeName.equalsIgnoreCase(IMC)) {
                    IMCArgs imcA = new IMCArgs(ndP.asXML());
                    protocolsArgs.put(CommMean.IMC, imcA);
                }
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(this, e);
            return false;
        }
        return true;
    }


    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return Returns the hostAddress.
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param hostAddress
     *            The hostAddress to set.
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * @return Returns the latency.
     */
    public String getLatency() {
        return latency;
    }

    /**
     * @param latency
     *            The latency to set.
     */
    public void setLatency(String latency) {
        this.latency = latency;
    }

    /**
     * @return Returns the latencyUnit.
     */
    public String getLatencyUnit() {
        return latencyUnit;
    }

    /**
     * @param latencyUnit
     *            The latencyUnit to set.
     */
    public void setLatencyUnit(String latencyUnit) {
        this.latencyUnit = latencyUnit;
    }

    /**
     * @return Returns the protocols.
     */
    public LinkedList<String> getProtocols() {
        return protocols;
    }

    /**
     * @return Returns the protocolsArgs.
     */
    public LinkedHashMap<String, ProtocolArgs> getProtocolsArgs() {
        return protocolsArgs;
    }

    /**
     * @return Returns the isActive.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * @param isActive
     *            The isActive to set.
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("name").setText(name);
        root.addElement("type").setText(type);

        root.addElement("host-address").setText(hostAddress);

        String protocolsStr = "";
        Iterator<?> it = protocols.iterator();
        while (it.hasNext()) {
            String pt = (String) it.next();
            protocolsStr += " " + pt;
        }
        root.addElement("protocols").setText(protocolsStr);

        Element laten = root.addElement("latency");
        laten.addAttribute("value", latency);
        if (!"".equals(latencyUnit))
            laten.addAttribute("unit", latencyUnit);

        if (protocolsArgs.size() > 0) {
            Element protoArgs = root.addElement("protocols-args");
            it = protocolsArgs.values().iterator();
            while (it.hasNext()) {
                Element elem = ((ProtocolArgs) it.next()).asElement();
                protoArgs.add(elem);
            }
        }

        return document;
    }
}
