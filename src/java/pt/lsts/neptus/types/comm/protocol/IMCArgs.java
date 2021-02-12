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
 * 28/Jun/2005
 */
package pt.lsts.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.util.Dom4JUtil;

/**
 * @author Paulo Dias
 */
public class IMCArgs extends ProtocolArgs {

    protected static final String DEFAULT_ROOT_ELEMENT = "imc";

    public static final int DEFAULT_PORT = 6002;

    protected int port = DEFAULT_PORT;
    protected int portTCP = DEFAULT_PORT;
    protected boolean udpOn = true;
    protected boolean tcpOn = false;

    protected ImcId16 imcId = null;

    private Document doc = null;
    private boolean isLoadOk = true;

    /**
     * 
     */
    public IMCArgs() {
        super();
    }

    /**
     * 
     */
    public IMCArgs(String xml) {
        // super();
        load(xml);
    }

    public int getPort() {
        return port;
    }

    /**
     * @param port The port to set.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the portTCP
     */
    public int getPortTCP() {
        return portTCP;
    }

    /**
     * @param portTCP the portTCP to set
     */
    public void setPortTCP(int portTCP) {
        this.portTCP = portTCP;
    }

    /**
     * @return the udpOn
     */
    public boolean isUdpOn() {
        return udpOn;
    }

    /**
     * @param udpOn the udpOn to set
     */
    public void setUdpOn(boolean udpOn) {
        this.udpOn = udpOn;
    }

    /**
     * @return the tcpOn
     */
    public boolean isTcpOn() {
        return tcpOn;
    }

    /**
     * @param tcpOn the tcpOn to set
     */
    public void setTcpOn(boolean tcpOn) {
        this.tcpOn = tcpOn;
    }

    /**
     * @return the imcId
     */
    public ImcId16 getImcId() {
        return imcId;
    }

    /**
     * @param imcId the imcId to set
     */
    public void setImcId(ImcId16 imcId) {
        this.imcId = imcId;
    }

    public boolean isLoadOk() {
        return isLoadOk;
    }

    public boolean load(Element elem) {
        try {
            doc = Dom4JUtil.elementToDocument(elem);
            if (doc == null) {
                isLoadOk = false;
                return false;
            }

            port = Integer.parseInt(doc.selectSingleNode("//port").getText());

            Node node = doc.selectSingleNode("//port-tcp");
            if (node != null)
                portTCP = Integer.parseInt(node.getText());
            else
                portTCP = DEFAULT_PORT;

            node = doc.selectSingleNode("//udp-on");
            if (node != null)
                udpOn = Boolean.parseBoolean(node.getText());
            else
                udpOn = true;

            node = doc.selectSingleNode("//tcp-on");
            if (node != null)
                tcpOn = Boolean.parseBoolean(node.getText());
            else
                tcpOn = false;

            node = doc.selectSingleNode("//imc-id");
            if (node != null)
                imcId = new ImcId16(node.getText());
            else
                imcId = null;

            isLoadOk = true;
        }
        catch (Exception e) {
            port = DEFAULT_PORT;
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
        return true;
    }

    /**
     * @param xml
     */
    public boolean load(String xml) {
        try {
            doc = DocumentHelper.parseText(xml);
            return load(doc.getRootElement());
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
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
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
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
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("port").setText(Integer.toString(port));
        root.addElement("port-tcp").setText(Integer.toString(portTCP));
        root.addElement("udp-on").setText(Boolean.toString(isUdpOn()));
        root.addElement("tcp-on").setText(Boolean.toString(isTcpOn()));

        if (getImcId() != null)
            root.addElement("imc-id").setText(getImcId().toPrettyString());

        return document;
    }
}
