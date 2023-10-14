/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 15/Jan/2005
 */
package pt.lsts.neptus.types.misc;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class FileType implements XmlOutputMethods {
    protected static final String DEFAULT_ROOT_ELEMENT = "file";

    protected String id = NameNormalizer.getRandomID();
    protected String name = id;
    protected String type = "unknown";
    protected String href = "benthos2.conf";
    protected String description = "";

    protected Document doc;

    protected String originalFilePath = "";

    /**
     * 
     */
    public FileType() {
        super();
    }

    /**
     * @param url
     * 
     */
    public FileType(String xml) {
        super();
        load(xml);
    }

    /**
     * @param url
     */
    public boolean load(String xml) {
        try {
            NeptusLog.pub().debug("FileType:\n" + xml);
            doc = DocumentHelper.parseText(xml);
            this.setId(doc.selectSingleNode("/file/id").getText());
            this.setName(doc.selectSingleNode("/file/name").getText());
            Node nd = doc.selectSingleNode("/file/type");
            if (nd != null)
                this.setType(nd.getText());
            else
                this.setType("unknown");
            if ("".equals(originalFilePath))
                this.setHref(doc.selectSingleNode("/file/href").getText());
            else
                this.setHref(ConfigFetch.resolvePathWithParent(originalFilePath, doc.selectSingleNode("/file/href")
                        .getText()));

            nd = doc.selectSingleNode("/file/description");
            if (nd != null)
                this.setDescription(nd.getText());

        }
        catch (DocumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @return Returns the href.
     */
    public String getHref() {
        return href;
    }

    /**
     * @param href The href to set.
     */
    public void setHref(String href) {
        this.href = href;
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
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
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
        this.originalFilePath = originalFilePath;
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

        root.addElement("id").addText(getId());
        root.addElement("name").addText(getName());
        root.addElement("type").addText(getType());
        if ("".equals(originalFilePath))
            root.addElement("href").addText(getHref());
        else
            root.addElement("href").addText(FileUtil.relativizeFilePathAsURI(originalFilePath, getHref()));
        if (!description.equalsIgnoreCase(""))
            root.addElement("description").addText(getDescription());

        return document;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getName() + " (" + getId() + ")";
    }
}
