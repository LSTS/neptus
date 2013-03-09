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
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

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
    protected boolean rtpsOn = false;

    protected ImcId16 imc3Id = null;

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
     * @return the rtpsOn
     */
    public boolean isRtpsOn() {
        return rtpsOn;
    }

    /**
     * @param rtpsOn the rtpsOn to set
     */
    public void setRtpsOn(boolean rtpsOn) {
        this.rtpsOn = rtpsOn;
    }

    /**
     * @return the imc3Id
     */
    public ImcId16 getImc3Id() {
        return imc3Id;
    }

    /**
     * @param imc3Id the imc3Id to set
     */
    public void setImc3Id(ImcId16 imc3Id) {
        this.imc3Id = imc3Id;
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

            node = doc.selectSingleNode("//rtps-on");
            if (node != null)
                rtpsOn = Boolean.parseBoolean(node.getText());
            else
                rtpsOn = false;

            node = doc.selectSingleNode("//imc3-id");
            if (node != null)
                imc3Id = new ImcId16(node.getText());
            else
                imc3Id = null;

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
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML(java.lang.String)
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
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("port").setText(Integer.toString(port));
        root.addElement("port-tcp").setText(Integer.toString(portTCP));
        root.addElement("udp-on").setText(Boolean.toString(isUdpOn()));
        root.addElement("tcp-on").setText(Boolean.toString(isTcpOn()));
        root.addElement("rtps-on").setText(Boolean.toString(isRtpsOn()));

        if (getImc3Id() != null)
            root.addElement("imc3-id").setText(getImc3Id().toPrettyString());

        return document;
    }
}
