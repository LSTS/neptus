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
 */
package pt.up.fe.dceg.neptus.types.comm;

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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PasswordPanel;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.comm.protocol.AdjustTimeShellArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.FTPArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.IMCArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.ProtocolArgs;

/**
 * @author Paulo Dias
 * @author ZP
 */
public class CommMean implements XmlOutputMethods {
    protected static final String DEFAULT_ROOT_ELEMENT = "comm-mean";

    public static final String IMC = "imc";
    public static final String HTTP = "http";

    protected String name = "";
    protected String type = "";
    protected String hostAddress = "127.0.0.1";
    
    //Credentials
    protected String userName = "root";
    protected String password = null;
    protected boolean isPasswordSaved = false;
    
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
            userName = doc.selectSingleNode("//credentials/@username").getText();
            Node nd = doc.selectSingleNode("//credentials/@password");
            if (nd != null) {
                password = nd.getText();
                isPasswordSaved = true;
            }
            else
                isPasswordSaved = false;
            
            nd = doc.selectSingleNode("//protocols");
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
                else if (nodeName.equalsIgnoreCase(AdjustTimeShellArgs.DEFAULT_ROOT_ELEMENT)) {
                    AdjustTimeShellArgs adjTimeParam = new AdjustTimeShellArgs(ndP.asXML());
                    protocolsArgs.put(AdjustTimeShellArgs.DEFAULT_ROOT_ELEMENT, adjTimeParam);
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
     * @return Returns the userName.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName
     *            The userName to set.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return Returns the password.
     */
    public String getPassword() {
        if ((password == null) || !isPasswordSaved()) {
            String[] ret = PasswordPanel.showPasswordDialog("Enter password for " + hostAddress,
                    userName, password, isPasswordSaved);
            setPassword(ret[0]);
            setPasswordSaved(Boolean.parseBoolean(ret[1]));
        }
        return password;
    }

    /**
     * @param password
     *            The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return Returns the isPasswordSaved.
     */
    public boolean isPasswordSaved() {
        return isPasswordSaved;
    }

    /**
     * @param isPasswordSaved
     *            The isPasswordSaved to set.
     */
    public void setPasswordSaved(boolean isPasswordSaved) {
        this.isPasswordSaved = isPasswordSaved;
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
     * @see
     * pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
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
     * @see
     * pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("name").setText(name);
        root.addElement("type").setText(type);

        root.addElement("host-address").setText(hostAddress);

        Element cred = root.addElement("credentials");
        cred.addAttribute("username", userName);
        if (isPasswordSaved)
            cred.addAttribute("password", password);

        String protocolsStr = "";
        Iterator<?> it = protocols.iterator();
        while (it.hasNext()) {
            String pt = (String) it.next();
            protocolsStr += " " + pt;
        }
        root.addElement("protocols").setText(protocolsStr);

        Element laten = root.addElement("latency");
        laten.addAttribute("value", latency);
        if (latencyUnit != "")
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
