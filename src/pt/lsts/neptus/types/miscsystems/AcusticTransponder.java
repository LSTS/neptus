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
 * 9/Out/2005
 */
package pt.lsts.neptus.types.miscsystems;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.XmlInputMethods;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class AcusticTransponder extends MiscSystems implements XmlOutputMethods, XmlInputMethods {
    protected static final String DEFAULT_ROOT_ELEMENT = "transponder";

    private float tranponderDelay = 0; // mseg.
    private float responderLockout = 0; // seg.
    private int interrogationChannel = 0;
    private int replyChannel = 0;

    private Document doc = null;
    private String originalFilePath = "";
    protected boolean isLoadOk = true;

    /**
     * 
     */
    public AcusticTransponder() {
        super();
    }

    /**
     * 
     */
    public AcusticTransponder(String xml) {
        super();
        isLoadOk = load(xml);
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
            e.printStackTrace();
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /**
     * @return
     */
    private boolean load() {
        // originalFilePath = new File(url).getAbsolutePath();

        // String fileAsString = xml; //FileUtil.getFileAsString(url);

        // Não tem validação já que não é um doc. em si.

        try {
            // doc = DocumentHelper.parseText(fileAsString);

            this.setId(doc.selectSingleNode("/transponder/properties/id").getText());
            this.setName(doc.selectSingleNode("/transponder/properties/name").getText());
            this.setModel(doc.selectSingleNode("/transponder/properties/model").getText());
            this.setXSize(new Float(doc.selectSingleNode("/transponder/properties/appearence/x-size").getText())
                    .floatValue());
            this.setYSize(new Float(doc.selectSingleNode("/transponder/properties/appearence/y-size").getText())
                    .floatValue());
            this.setZSize(new Float(doc.selectSingleNode("/transponder/properties/appearence/z-size").getText())
                    .floatValue());

            this.setTopImageHref(doc.selectSingleNode("/transponder/properties/appearence/top-image-2D").getText());
            this.setSideImageHref(doc.selectSingleNode("/transponder/properties/appearence/side-image-2D").getText());
            this.setModel3DHref(doc.selectSingleNode("/transponder/properties/appearence/model-3D").getText());
            this.setTopImageHref(ConfigFetch.resolvePathWithParent(originalFilePath, getTopImageHref()));
            this.setSideImageHref(ConfigFetch.resolvePathWithParent(originalFilePath, getSideImageHref()));
            this.setModel3DHref(ConfigFetch.resolvePathWithParent(originalFilePath, getModel3DHref()));

            this.setTranponderDelay(new Float(doc.selectSingleNode("/transponder/configuration/transponder-delay")
                    .getText()).floatValue());
            this.setResponderLockout(new Float(doc.selectSingleNode("/transponder/configuration/responder-lockout")
                    .getText()).floatValue());
            this.setInterrogationChannel(new Integer(doc.selectSingleNode(
                    "/transponder/configuration/interrogation-channel").getText()).intValue());
            this.setReplyChannel(new Integer(doc.selectSingleNode("/transponder/configuration/reply-channel").getText())
                    .intValue());

        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlInputMethods#isLoadOk()
     */
    public boolean isLoadOk() {
        return isLoadOk;
    }

    /**
     * @return Returns the tranponderDelay.
     */
    public float getTranponderDelay() {
        return tranponderDelay;
    }

    /**
     * @param tranponderDelay The tranponderDelay to set.
     */
    public void setTranponderDelay(float tranponderDelay) {
        this.tranponderDelay = tranponderDelay;
    }

    /**
     * @return Returns the responderLockout.
     */
    public float getResponderLockout() {
        return responderLockout;
    }

    /**
     * @param responderLockout The responderLockout to set.
     */
    public void setResponderLockout(float responderLockout) {
        this.responderLockout = responderLockout;
    }

    /**
     * @return Returns the interrogationChannel.
     */
    public int getInterrogationChannel() {
        return interrogationChannel;
    }

    /**
     * @param interrogationChannel The interrogationChannel to set.
     */
    public void setInterrogationChannel(int interrogationChannel) {
        this.interrogationChannel = interrogationChannel;
    }

    /**
     * @return Returns the replyChannel.
     */
    public int getReplyChannel() {
        return replyChannel;
    }

    /**
     * @param replyChannel The replyChannel to set.
     */
    public void setReplyChannel(int replyChannel) {
        this.replyChannel = replyChannel;
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

        // root.addComment(ConfigFetch.getSaveAsCommentForXML());

        Element properties = root.addElement("properties");
        properties.addElement("id").addText(getId());
        properties.addElement("name").addText(getName());
        properties.addElement("model").addText(getModel());

        Element appearence = properties.addElement("appearence");
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
        if ("".equals(originalFilePath))
            appearence.addElement("model-3D").addText(getModel3DHref());
        else
            appearence.addElement("model-3D").addText(
                    FileUtil.relativizeFilePathAsURI(originalFilePath, getModel3DHref()));

        Element configuration = root.addElement("configuration");
        configuration.addElement("transponder-delay").addText(Float.toString(getTranponderDelay()))
                .addAttribute("unit", "ms");
        configuration.addElement("responder-lockout").addText(Float.toString(getResponderLockout()))
                .addAttribute("unit", "s");
        configuration.addElement("interrogation-channel").addText(Integer.toString(getInterrogationChannel()));
        configuration.addElement("reply-channel").addText(Integer.toString(getReplyChannel()));

        return document;
    }

}
