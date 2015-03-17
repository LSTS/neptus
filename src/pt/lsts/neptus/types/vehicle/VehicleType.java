/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2005/01/14
 */
package pt.lsts.neptus.types.vehicle;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverFactory;
import pt.lsts.neptus.types.XmlInputMethods;
import pt.lsts.neptus.types.XmlInputMethodsFromFile;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.comm.protocol.AdjustTimeShellArgs;
import pt.lsts.neptus.types.comm.protocol.FTPArgs;
import pt.lsts.neptus.types.comm.protocol.IMCArgs;
import pt.lsts.neptus.types.comm.protocol.GsmArgs;
import pt.lsts.neptus.types.comm.protocol.IridiumArgs;
import pt.lsts.neptus.types.comm.protocol.ProtocolArgs;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.misc.FileType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.XMLValidator;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class VehicleType implements XmlOutputMethods, XmlInputMethods, XmlInputMethodsFromFile {

    /*
     * <def prefix="SYSTEMTYPE" name="System Type" abbrev="SystemType"> <enum id="0" name="CCU" abbrev="CCU"/> <enum
     * id="1" name="Human-portable Sensor" abbrev="HUMANSENSOR"/> <enum id="2" name="UUV" abbrev="UUV"/> <enum id="3"
     * name="USV" abbrev="USV"/> <enum id="4" name="UAV" abbrev="UAV"/> <enum id="5" name="UGV" abbrev="UGV"/> <enum
     * id="6" name="Static sensor" abbrev="STATICSENSOR"/> <enum id="7" name="Mobile sensor" abbrev="MOBILESENSOR"/>
     * <enum id="8" name="Wireless Sensor Network" abbrev="WSN"/>
     */

    public static enum SystemTypeEnum {
        UNKNOWN,
        VEHICLE,
        CCU,
        STATICSENSOR,
        MOBILESENSOR,
        ALL
    };

    public static enum VehicleTypeEnum {
        UNKNOWN,
        UUV,
        USV,
        UGV,
        UAV,
        ALL
    };

    protected static final String DEFAULT_ROOT_ELEMENT_DEPREC = "vehicle";
    protected static final String DEFAULT_ROOT_ELEMENT = "system";

    public static final Color DEFAULT_ICON_COLOR = Color.RED;

    private String id = "";
    private String name = "";
    private String type = "";
    private String model = "";
    private boolean operationalActive = true;

    private float xSize = 0;
    private float ySize = 0;
    private float zSize = 0;
    private String topImageHref = "";
    private String sideImageHref = "";
    private String backImageHref = "";
    private String presentationImageHref = "";
    private String model3dHref = "";
    private Color iconColor = DEFAULT_ICON_COLOR;

    private LinkedHashMap<String, TemplateFileVehicle> transformationXSLTTemplates = new LinkedHashMap<String, TemplateFileVehicle>();
    private FileType maneuverAdditionalFile = null;
    private LinkedHashMap<String, FileType> miscConfigurationFiles = new LinkedHashMap<String, FileType>();
    private String coordinateSystemLabel = null;
    private CoordinateSystem coordinateSystem = null;

    private Document doc = null;
    protected boolean isLoadOk = true;

    private String originalFilePath = "";

    private LinkedHashMap<String, String> feasibleManeuvers = new LinkedHashMap<String, String>();
    private Element xmlFeasibleManeuvers = null;

    /**
     * <code>communicationMeans &lt;String type, CommMean V&gt;</code>
     */
    private LinkedHashMap<String, CommMean> communicationMeans = new LinkedHashMap<String, CommMean>();

    protected LinkedList<String> protocols = new LinkedList<String>();
    protected LinkedHashMap<String, ProtocolArgs> protocolsArgs = new LinkedHashMap<String, ProtocolArgs>();

    private LinkedHashMap<String, String> consoles = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> consolesType = new LinkedHashMap<String, String>();

    private ManeuverFactory manFactory = null;

    public VehicleType() {

    }

    public VehicleType(String url) {
        loadFile(url);
    }

    public boolean isLoadOk() {
        return isLoadOk;
    }

    /**
     * @param url
     */
    public boolean loadFile(String url) {
        originalFilePath = new File(url).getAbsolutePath();

        String fileAsString = FileUtil.getFileAsString(url);
        if (fileAsString == null) {
            JOptionPane.showMessageDialog(ConfigFetch.getSuperParentFrame(), "<html>File " + url
                    + " was not loaded.<br>The file was not found.</html>");
            return false;
        }

        return load(fileAsString);
    }

    public boolean loadFile(File file) {
        return loadFile(file.getAbsolutePath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlInputMethods#load(org.dom4j.Element)
     */
    public boolean load(Element elem) {
        doc = Dom4JUtil.elementToDocument(elem);
        if (doc == null) {
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlInputMethods#load(java.lang.String)
     */
    public boolean load(String xml) {
        try {
            doc = DocumentHelper.parseText(xml);
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(e.getMessage());
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /**
     * @return
     */
    private boolean load() {
        long initTime = System.currentTimeMillis();
        try {
            // doc = DocumentHelper.parseText(xml);

            // isLoadOk = validate(doc);

            // Test base element name
            String rootElemName = DEFAULT_ROOT_ELEMENT;
            if (doc.selectSingleNode("/" + DEFAULT_ROOT_ELEMENT) != null)
                rootElemName = DEFAULT_ROOT_ELEMENT;
            else if (doc.selectSingleNode("/" + DEFAULT_ROOT_ELEMENT_DEPREC) != null)
                rootElemName = DEFAULT_ROOT_ELEMENT_DEPREC;

            try {
                // FIXME get feasible maneuvers!
                xmlFeasibleManeuvers = (Element) doc.selectSingleNode("/" + rootElemName + "/feasibleManeuvers");
                xmlFeasibleManeuvers = (Element) xmlFeasibleManeuvers.createCopy().detach();
                List<?> maneuvers = doc.selectNodes("/" + rootElemName
                        + "/feasibleManeuvers/maneuver/*/annotation/implementation-class");

                for (int i = 0; i < maneuvers.size(); i++) {
                    Node tmpMan = (Node) maneuvers.get(i);
                    String manName = tmpMan.getParent().getParent().getName();
                    feasibleManeuvers.put(manName, tmpMan.getText());
                }
            }
            catch (Exception e) {
                NeptusLog.pub().debug(e.getMessage());
                NeptusLog.pub().warn("No maneuvers found for system " + id + "!!");
            }

            this.setId(doc.selectSingleNode("/" + rootElemName + "/properties/id").getText());
            this.setName(doc.selectSingleNode("/" + rootElemName + "/properties/name").getText());
            this.setType(doc.selectSingleNode("/" + rootElemName + "/properties/type").getText());
            Node nd = doc.selectSingleNode("/" + rootElemName + "/properties/model");
            if (nd != null)
                this.setModel(nd.getText());
            else
                this.setModel("");
            nd = doc.selectSingleNode("/" + rootElemName + "/properties/operational-active");
            if (nd != null) {
                try {
                    operationalActive = Boolean.parseBoolean(nd.getText());
                }
                catch (Exception e) {
                    NeptusLog.pub().debug(e.getMessage());
                    operationalActive = true;
                }
            }
            else {
                operationalActive = true;
            }

            this.setXSize(new Float(doc.selectSingleNode("/" + rootElemName + "/properties/appearance/x-size")
                    .getText()).floatValue());
            this.setYSize(new Float(doc.selectSingleNode("/" + rootElemName + "/properties/appearance/y-size")
                    .getText()).floatValue());
            this.setZSize(new Float(doc.selectSingleNode("/" + rootElemName + "/properties/appearance/z-size")
                    .getText()).floatValue());

            this.setTopImageHref(doc.selectSingleNode("/" + rootElemName + "/properties/appearance/top-image-2D")
                    .getText());
            this.setSideImageHref(doc.selectSingleNode("/" + rootElemName + "/properties/appearance/side-image-2D")
                    .getText());
            nd = doc.selectSingleNode("/" + rootElemName + "/properties/appearance/presentation-image-2D");
            if (nd != null)
                this.setPresentationImageHref(nd.getText());
            else
                this.setPresentationImageHref("");
            nd = doc.selectSingleNode("/" + rootElemName + "/properties/appearance/back-image-2D");
            if (nd != null)
                this.setBackImageHref(nd.getText());
            else
                this.setBackImageHref("");
            this.setModel3DHref(doc.selectSingleNode("/" + rootElemName + "/properties/appearance/model-3D").getText());
            this.setTopImageHref(ConfigFetch.resolvePathWithParent(originalFilePath, getTopImageHref()));
            this.setSideImageHref(ConfigFetch.resolvePathWithParent(originalFilePath, getSideImageHref()));
            if (!getPresentationImageHref().equalsIgnoreCase(""))
                this.setPresentationImageHref(ConfigFetch.resolvePathWithParent(originalFilePath,
                        getPresentationImageHref()));
            if (!getBackImageHref().equalsIgnoreCase(""))
                this.setBackImageHref(ConfigFetch.resolvePathWithParent(originalFilePath, getBackImageHref()));
            this.setModel3DHref(ConfigFetch.resolvePathWithParent(originalFilePath, getModel3DHref()));

            nd = doc.selectSingleNode("/" + rootElemName + "/properties/appearance/icon-color");
            if (nd != null) {
                String rS = nd.selectSingleNode("r").getText();
                String gS = nd.selectSingleNode("g").getText();
                String bS = nd.selectSingleNode("b").getText();
                int rr = Integer.parseInt(rS);
                int gg = Integer.parseInt(gS);
                int bb = Integer.parseInt(bS);
                this.setIconColor(new Color(rr, gg, bb));
            }

            // this.setCoordinateSystemUsedLabel(doc.selectSingleNode("/"+rootElemName+"/properties/coordinate-system").getText());
            nd = doc.selectSingleNode("/" + rootElemName + "/properties/coordinate-system-label");
            if (nd != null)
                this.setCoordinateSystemLabel(nd.getText());
            else {
                nd = doc.selectSingleNode("/" + rootElemName + "/properties/coordinate-system-def");
                if (nd != null) {
                    String xml1 = nd.asXML();
                    CoordinateSystem cs = new CoordinateSystem(xml1);
                    this.setCoordinateSystem(cs);
                }
                else {
                    // TODO Ver melhor esta parte
                    CoordinateSystem cs = new CoordinateSystem();
                    this.setCoordinateSystem(cs);
                }
            }

            nd = doc.selectSingleNode("/" + rootElemName + "/protocols-supported/protocols");
            protocols.clear();
            if (nd != null) {
                String protocolsStr = nd.getText();
                StringTokenizer strt = new StringTokenizer(protocolsStr, " ");
                while (strt.hasMoreTokens()) {
                    protocols.add(strt.nextToken());
                }
            }

            List<?> lt = doc.selectNodes("/" + rootElemName + "/protocols-supported/protocols-args/*");
            Iterator<?> it = lt.iterator();
            protocolsArgs.clear();
            while (it.hasNext()) {
                Node ndP = (Node) it.next();
                String nodeName = ndP.getName();
                if (nodeName.equalsIgnoreCase("ftp")) {
                    FTPArgs ftpA = new FTPArgs(ndP.asXML());
                    protocolsArgs.put("ftp", ftpA);
                }
                else if (nodeName.equalsIgnoreCase(CommMean.IMC)) {
                    IMCArgs imcA = new IMCArgs(ndP.asXML());
                    protocolsArgs.put(CommMean.IMC, imcA);
                }
                else if (nodeName.equalsIgnoreCase(AdjustTimeShellArgs.DEFAULT_ROOT_ELEMENT)) {
                    AdjustTimeShellArgs adjTimeParam = new AdjustTimeShellArgs(ndP.asXML());
                    protocolsArgs.put(AdjustTimeShellArgs.DEFAULT_ROOT_ELEMENT, adjTimeParam);
                }
                else if (nodeName.equalsIgnoreCase("gsm")) {
                    GsmArgs gsmArgs = new GsmArgs();
                    gsmArgs.load(ndP.asXML());
                    protocolsArgs.put(CommMean.GSM, gsmArgs);
                }
                else if (nodeName.equalsIgnoreCase("iridium")) {
                    IridiumArgs iridiumArgs = new IridiumArgs();
                    iridiumArgs.load(ndP.asXML());
                    protocolsArgs.put(CommMean.IRIDIUM, iridiumArgs);
                }
            }

            List<?> lst = doc.selectNodes("/" + rootElemName + "/transformation-xslt-templates/file");
            ListIterator<?> lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                TemplateFileVehicle tfile = new TemplateFileVehicle(elem.asXML());
                transformationXSLTTemplates.put(tfile.getId(), tfile);
                tfile.setOriginalFilePath(originalFilePath);
                tfile.setHref(ConfigFetch.resolvePathWithParent(originalFilePath, tfile.getHref()));
                tfile.setOutputFileName(ConfigFetch.resolvePathWithParent(originalFilePath, tfile.getOutputFileName()));
            }

            nd = doc.selectSingleNode("/" + rootElemName + "/configuration-files/maneuver-additional/file");
            if (nd != null) {
                FileType tfile = new FileType(nd.asXML());
                this.setManeuverAdditionalFile(tfile);
                tfile.setOriginalFilePath(originalFilePath);
                tfile.setHref(ConfigFetch.resolvePathWithParent(originalFilePath, tfile.getHref()));
            }

            lst = doc.selectNodes("/" + rootElemName + "/configuration-files/misc/file");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                FileType tfile = new FileType(elem.asXML());
                miscConfigurationFiles.put(tfile.getId(), tfile);
                tfile.setOriginalFilePath(originalFilePath);
                tfile.setHref(ConfigFetch.resolvePathWithParent(originalFilePath, tfile.getHref()));
            }

            nd = doc.selectSingleNode("/" + rootElemName + "/communication-means");
            if (nd != null) {
                lst = nd.selectNodes("comm-mean");
                lstIt = lst.listIterator();
                while (lstIt.hasNext()) {
                    Element elem = (Element) lstIt.next();
                    CommMean cm = new CommMean(elem.asXML());
                    communicationMeans.put(cm.getType(), cm);
                }
            }

            List<?> list = doc.selectNodes("/" + rootElemName + "/consoles/console");
            lstIt = list.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                Node cls = elem.selectSingleNode("classname");
                if (cls == null)
                    cls = elem.selectSingleNode("xml-file");
                if (cls == null)
                    continue;
                String typeName = cls.getName();
                String typeNameValue = cls.getText();

                if (typeName.equals("xml-file"))
                    if (!"".equalsIgnoreCase(originalFilePath))
                        typeNameValue = ConfigFetch.resolvePathWithParent(originalFilePath, typeNameValue);

                consoles.put(elem.attributeValue("name"), typeNameValue);
                consolesType.put(elem.attributeValue("name"), typeName);
                // NeptusLog.pub().info("<###>put("+elem.attributeValue("name")+", "+elem.selectSingleNode("classname").getText()+")");
            }

        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            long totalTime = System.currentTimeMillis() - initTime;
            NeptusLog.pub().info(this + ": Total vehicle load time: " + totalTime + " ms.");
            isLoadOk = false;
            return false;
        }
        long totalTime = System.currentTimeMillis() - initTime;
        NeptusLog.pub().debug(this + ": Total vehicle load time: " + totalTime + " ms.");
        isLoadOk = true;
        return true;
    }

    /**
     * @param doc
     * @return
     */
    public static boolean validate(Document doc) {
        try {
            String sLoc = new File(ConfigFetch.getVehicleSchemaLocation()).getAbsoluteFile().toURI().toASCIIString();
            XMLValidator xmlVal = new XMLValidator(doc, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e1) {
            NeptusLog.pub().error("Vehicle:validate", e1);
            // e1.printStackTrace();
            return false;
        }
    }

    /**
     * @param xml
     * @return
     */
    public static boolean validate(String xml) {
        try {
            String sLoc = new File(ConfigFetch.getVehicleSchemaLocation()).getAbsoluteFile().toURI().toASCIIString();
            XMLValidator xmlVal = new XMLValidator(xml, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e1) {
            NeptusLog.pub().error("Vehicle:validate", e1);
            // e1.printStackTrace();
            return false;
        }
    }

    /**
     * @param file
     * @return
     */
    public static boolean validate(File file) {
        try {
            // System.err.println(file.getAbsoluteFile().toURI());
            String xml = FileUtil.getFileAsString(file);
            return validate(xml);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Vehicle:validate", e);
            return false;
        }
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getFeasibleManeuvers() {

        return feasibleManeuvers;
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    
    public String getNickname() {
        return getId().replaceFirst("lauv-", "").replaceAll("-", "");
    }

    /**
     * @param name The name to set.
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
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the operationalActive
     */
    public boolean isOperationalActive() {
        return operationalActive;
    }

    /**
     * @param operationalActive the operationalActive to set
     */
    public void setOperationalActive(boolean operationalActive) {
        this.operationalActive = operationalActive;
    }

    /**
     * @return Returns the xSize.
     */
    public float getXSize() {
        return xSize;
    }

    /**
     * @param size The xSize to set.
     */
    public void setXSize(float size) {
        xSize = size;
    }

    /**
     * @return Returns the ySize.
     */
    public float getYSize() {
        return ySize;
    }

    /**
     * @param size The ySize to set.
     */
    public void setYSize(float size) {
        ySize = size;
    }

    /**
     * @return Returns the zSize.
     */
    public float getZSize() {
        return zSize;
    }

    /**
     * @param size The zSize to set.
     */
    public void setZSize(float size) {
        zSize = size;
    }

    /**
     * @return Returns the transformationXSLTTemplates.
     */
    public LinkedHashMap<String, TemplateFileVehicle> getTransformationXSLTTemplates() {
        return transformationXSLTTemplates;
    }

    /**
     * @return Returns the maneuverAdditionalFile.
     */
    public FileType getManeuverAdditionalFile() {
        return maneuverAdditionalFile;
    }

    /**
     * @param maneuverAdditionalFile The maneuverAdditionalFile to set.
     */
    public void setManeuverAdditionalFile(FileType maneuverAdditionalFile) {
        this.maneuverAdditionalFile = maneuverAdditionalFile;
    }

    /**
     * @return Returns the configurationFilesMisc.
     */
    public LinkedHashMap<String, FileType> getMiscConfigurationFiles() {
        return miscConfigurationFiles;
    }

    /**
     * @return Returns the coordinateSystemUsed.
     */
    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * @param coordinateSystem The coordinateSystemUsed to set.
     */
    public void setCoordinateSystem(CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }

    /**
     * @return Returns the coordinateSystemUsedLabel.
     */
    public String getCoordinateSystemLabel() {
        return coordinateSystemLabel;
    }

    /**
     * @param coordinateSystemLabel The coordinateSystemUsedLabel to set.
     */
    public void setCoordinateSystemLabel(String coordinateSystemLabel) {
        this.coordinateSystemLabel = coordinateSystemLabel;
    }

    /**
     * @return Returns the originalFilePath.
     */
    public String getOriginalFilePath() {
        return originalFilePath;
    }

    /**
     * @param originalFilePath The originalFilePath to set.
     */
    public void setOriginalFilePath(String originalFilePath) {
        // FIXME Actualizar tb os filhos
        this.originalFilePath = originalFilePath;
    }

    /**
     * @return
     */
    public String getSideImageHref() {
        return sideImageHref;
    }

    /**
     * @param sideImageHref
     */
    public void setSideImageHref(String sideImageHref) {
        this.sideImageHref = sideImageHref;
    }

    /**
     * @return
     */
    public String getTopImageHref() {
        return topImageHref;
    }

    /**
     * @param topImageHref
     */
    public void setTopImageHref(String topImageHref) {
        this.topImageHref = topImageHref;
    }

    /**
     * @return the backImageHref
     */
    public String getBackImageHref() {
        return backImageHref;
    }

    /**
     * @param backImageHref the backImageHref to set
     */
    public void setBackImageHref(String backImageHref) {
        this.backImageHref = backImageHref;
    }

    /**
     * @return the presentationImageHref
     */
    public String getPresentationImageHref() {
        return presentationImageHref;
    }

    protected ImageIcon icon = null;

    public ImageIcon getIcon() {
        if (icon == null)
            icon = new ImageIcon(ImageUtils.getImage(presentationImageHref).getScaledInstance(40, -1,
                    Image.SCALE_SMOOTH));
        return icon;
    }

    /**
     * @param presentationImageHref the presentationImageHref to set
     */
    public void setPresentationImageHref(String presentationImageHref) {
        this.presentationImageHref = presentationImageHref;
    }

    /**
     * @return Returns the model3dHref.
     */
    public String getModel3DHref() {
        return model3dHref;
    }

    /**
     * @param model3dHref The model3dHref to set.
     */
    public void setModel3DHref(String model3dHref) {
        this.model3dHref = model3dHref;
    }

    /**
     * @return the iconColor
     */
    public Color getIconColor() {
        return iconColor;
    }

    /**
     * @param iconColor the iconColor to set
     */
    public void setIconColor(Color iconColor) {
        this.iconColor = iconColor;
    }

    /**
     * @return Returns the communicationMeans.
     */
    public LinkedHashMap<String, CommMean> getCommunicationMeans() {
        return communicationMeans;
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

        root.addComment(ConfigFetch.getSaveAsCommentForXML());

        Element properties = root.addElement("properties");
        properties.addElement("id").addText(getId());
        properties.addElement("name").addText(getName());
        properties.addElement("type").addText(getType());
        if (!"".equalsIgnoreCase(getModel()))
            properties.addElement("model").addText(getModel());
        if (!operationalActive)
            properties.addElement("operational-active").addText("false");

        Element appearence = properties.addElement("appearance");
        appearence.addElement("x-size").addText(Float.toString(getXSize()));
        appearence.addElement("y-size").addText(Float.toString(getYSize()));
        appearence.addElement("z-size").addText(Float.toString(getZSize()));
        if ("".equals(originalFilePath))
            appearence.addElement("top-image-2D").addText(getTopImageHref());
        else
            appearence.addElement("top-image-2D").addText(
                    FileUtil.relativizeFilePathAsURI(originalFilePath, getTopImageHref()));
        if ("".equals(originalFilePath))
            appearence.addElement("side-image-2D").addText(getSideImageHref());
        else
            appearence.addElement("side-image-2D").addText(
                    FileUtil.relativizeFilePathAsURI(originalFilePath, getSideImageHref()));
        if (!getBackImageHref().equalsIgnoreCase("")) {
            if ("".equals(originalFilePath))
                appearence.addElement("back-image-2D").addText(getBackImageHref());
            else
                appearence.addElement("back-image-2D").addText(
                        FileUtil.relativizeFilePathAsURI(originalFilePath, getBackImageHref()));
        }
        if (!getPresentationImageHref().equalsIgnoreCase("")) {
            if ("".equals(originalFilePath))
                appearence.addElement("presentation-image-2D").addText(getPresentationImageHref());
            else
                appearence.addElement("presentation-image-2D").addText(
                        FileUtil.relativizeFilePathAsURI(originalFilePath, getPresentationImageHref()));
        }
        if ("".equals(originalFilePath))
            appearence.addElement("model-3D").addText(getModel3DHref());
        else
            appearence.addElement("model-3D").addText(
                    FileUtil.relativizeFilePathAsURI(originalFilePath, getModel3DHref()));

        if (getIconColor() != DEFAULT_ICON_COLOR) {
            Element colorE = appearence.addElement("icon-color");
            colorE.addElement("r").setText(Integer.toString(getIconColor().getRed()));
            colorE.addElement("g").setText(Integer.toString(getIconColor().getGreen()));
            colorE.addElement("b").setText(Integer.toString(getIconColor().getBlue()));
        }

        if ("".equals(coordinateSystemLabel))
            properties.add(coordinateSystem.asElement());
        else
            properties.addElement("coordinate-system-label").addText(coordinateSystemLabel);

        try {
            // feasibleManeuvers
            // Element feasibleManeuvers = root.addElement( "feasibleManeuvers" );
            Document docX = Dom4JUtil.elementToDocumentCleanFormating((Element) xmlFeasibleManeuvers.detach());
            // root.add(xmlFeasibleManeuvers);
            root.add(docX.getRootElement().detach());
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e.getMessage());
            NeptusLog.pub().warn("No maneuvers found for system " + id + "!!");
        }

        // Element sensors = root.addElement( "sensors" );

        String protocolsStr = "";
        Iterator<?> it = protocols.iterator();
        while (it.hasNext()) {
            String pt = (String) it.next();
            protocolsStr += " " + pt;
        }
        if (!"".equalsIgnoreCase(protocolsStr)) {
            Element protoSupport = root.addElement("protocols-supported");
            protoSupport.addElement("protocols").setText(protocolsStr);

            if (protocolsArgs.size() > 0) {
                Element protoArgs = protoSupport.addElement("protocols-args");
                it = protocolsArgs.values().iterator();
                if (protocolsArgs.size() > 0)

                    while (it.hasNext()) {
                        Element elem = ((ProtocolArgs) it.next()).asElement();
                        protoArgs.add(elem);
                    }
            }
        }

        // Element communicationMeans = root.addElement( "communication-means" );
        if (communicationMeans.size() > 0) {
            Element configurationFiles = root.addElement("communication-means");
            it = communicationMeans.values().iterator();
            while (it.hasNext()) {
                CommMean cm = (CommMean) it.next();
                Element cmean = cm.asElement();
                configurationFiles.add(cmean);
            }
        }

        if (transformationXSLTTemplates.size() != 0) {
            Element transformationXsltTemplates = root.addElement("transformation-xslt-templates");
            for (Iterator<?> iter = transformationXSLTTemplates.values().iterator(); iter.hasNext();) {
                TemplateFileVehicle tmplt = (TemplateFileVehicle) iter.next();
                transformationXsltTemplates.add(tmplt.asElement());
            }
        }

        if ((maneuverAdditionalFile != null) | (miscConfigurationFiles.size() != 0)) {
            Element configurationFiles = root.addElement("configuration-files");
            if (maneuverAdditionalFile != null)
                configurationFiles.add(maneuverAdditionalFile.asElement());
            if (miscConfigurationFiles.size() != 0) {
                Element miscConf = configurationFiles.addElement("misc");
                for (Iterator<?> iter = miscConfigurationFiles.values().iterator(); iter.hasNext();) {
                    FileType ft = (FileType) iter.next();
                    miscConf.add(ft.asElement());
                }
            }
        }

        /*
         * List list = doc.selectNodes("/"+rootElemName+"/consoles/console"); lstIt = list.listIterator(); while
         * (lstIt.hasNext()) { Element elem = (Element) lstIt.next(); Node cls = elem.selectSingleNode("classname"); if
         * (cls == null) cls = elem.selectSingleNode("xml-file"); if (cls == null) continue; String typeName =
         * cls.getName(); String typeNameValue = cls.getText(); consoles.put(elem.attributeValue("name"),
         * typeNameValue); consolesType.put(elem.attributeValue("name"), typeName);
         * //NeptusLog.pub().info("<###>put("+elem.attributeValue
         * ("name")+", "+elem.selectSingleNode("classname").getText()+")"); }
         */
        if (!consoles.isEmpty()) {
            Element consolesFiles = root.addElement("consoles");
            for (String conName : consoles.keySet()) {
                String file, type;
                file = consoles.get(conName);
                type = consolesType.get(conName);
                if ((file != null) && (type != null)) {
                    if (type.equals("xml-file"))
                        if (!"".equals(originalFilePath))
                            file = FileUtil.relativizeFilePathAsURI(originalFilePath, file);

                    Element consElem = consolesFiles.addElement("console");
                    consElem.addAttribute("name", conName);
                    consElem.addElement(type).addText(file);
                }
            }
        }

        return document;
    }

    public static VehicleType valueOf(String value) {
        return VehiclesHolder.getVehicleById(value);

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getId();
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getConsoles() {
        return consoles;
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getConsolesTypes() {
        return consolesType;
    }

    public ImcId16 getImcId() {
        for (CommMean cm : getCommunicationMeans().values()) {
            ProtocolArgs args = cm.getProtocolsArgs().get(CommMean.IMC);
            if (args != null) {
                IMCArgs imcArgs = (IMCArgs) args;
                return (imcArgs.getImc3Id() == null) ? ImcId16.NULL_ID : imcArgs.getImc3Id();
            }
        }
        return ImcId16.NULL_ID;
    }

    /**
     * Get a list of this vehicle's feasible plans
     * 
     * @return all plans in the current mission that belong to the active vehicle's ID
     */
    public PlanType[] getFeasiblePlans(MissionType mission) {
        Vector<PlanType> plans = new Vector<PlanType>();

        if (mission == null || mission == null)
            return new PlanType[] {};

        for (PlanType p : mission.getIndividualPlansList().values()) {
            if (p.isSupportedBy(this))
                plans.add(p);
        }

        return plans.toArray(new PlanType[] {});
    }

    /**
     * @return
     */
    public ManeuverFactory getManeuverFactory() {
        if (manFactory == null) {
            manFactory = new ManeuverFactory(this);

            List<?> maneuvers = xmlFeasibleManeuvers
                    .selectNodes("maneuver/*[name()!='minTime' and name()!='maxTime' and name()!='custom-settings']");

            for (Object o : maneuvers) {

                Element el = (Element) o;
                Maneuver man = manFactory.getManeuver(el.getName());

                if (man != null) {
                    NeptusLog.pub().debug(
                            "loading maneuver '" + el.getName() + "' default values for vehicle '" + getId() + "'");
                    man.loadFromXML(el.asXML());
                    man.loadFromXMLExtraParameters(el.getParent());
                    manFactory.putManeuver(man);
                }
            }
        }

        return manFactory;
    }
}