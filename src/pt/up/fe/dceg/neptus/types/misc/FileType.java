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
 * 15/Jan/2005
 * $Id:: FileType.java 9821 2013-01-31 16:05:49Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.types.misc;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.NameNormalizer;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
            if (originalFilePath == "")
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

        root.addElement("id").addText(getId());
        root.addElement("name").addText(getName());
        root.addElement("type").addText(getType());
        if (originalFilePath == "")
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
