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
 * 26/Jun/2005
 * $Id:: ChecklistType.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.types.checklist;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.XmlInputMethods;
import pt.up.fe.dceg.neptus.types.XmlInputMethodsFromFile;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.mission.ChecklistMission;
import pt.up.fe.dceg.neptus.util.Dom4JUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.XMLValidator;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * Refactored in 06/11/2006.
 * @author Paulo Dias
 */
public class ChecklistType 
implements XmlOutputMethods, XmlInputMethods, XmlInputMethodsFromFile {
    protected static final String DEFAULT_ROOT_ELEMENT = "checklist";

    public static final String FLAT_ID = "flat";

    protected String name = "";
    protected String version = "";
    protected String description = null;
    protected boolean isFlat = true;
    protected boolean isLoadOk = true;
    
    /**
     * <String, LinkedList<CheckItem> >
     */
    protected LinkedHashMap<String, LinkedList<CheckItem>> groupList = new LinkedHashMap<String, LinkedList<CheckItem>>();
    
    private Document doc;
    
    private String originalFilePath = "";

    /**
     * 
     */
    public ChecklistType(String url) {
        super();
        loadFile(url);
    }

    public ChecklistType() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#load(org.dom4j.Element)
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
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#load(java.lang.String)
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
        long initTime = System.currentTimeMillis();
        try {
            //doc = DocumentHelper.parseText(xml);
            this.setName(doc.selectSingleNode("/checklist/@name").getText());
            Node nd = doc.selectSingleNode("/checklist/description");
            if (nd != null)
                this.setDescription(nd.getText());
            else {
                nd = doc.selectSingleNode("/checklist/@description");
                if (nd != null)
                    this.setDescription(nd.getText());
                else
                    this.setDescription(null);
            }

            nd = doc.selectSingleNode("/checklist/@version");
            if (nd != null)
                this.setVersion(nd.getText());
            else
                this.setVersion("");

            nd = doc.selectSingleNode("/checklist/item");
            if (nd != null)
                setFlat(true);
            else
                setFlat(false);

            if (isFlat()) {
                List<?> lt = doc.selectNodes("/checklist/item");
                if (!lt.isEmpty()) {
                    LinkedList<CheckItem> lkl = new LinkedList<CheckItem>();
                    Iterator<?> it = lt.iterator();
                    while (it.hasNext()) {
                        CheckItem ci = new CheckItem(((Node)it.next()).asXML());
                        lkl.add(ci);
                    }
                    groupList.put(FLAT_ID, lkl);
                }
            }
            else {
                List<?> lt = doc.selectNodes("/checklist/group");
                if (!lt.isEmpty()) {
                    Iterator<?> it = lt.iterator();
                    while (it.hasNext()) {
                        Node nd1 = (Node) it.next();
                        String name = nd1.selectSingleNode("./@name").getText();
                        List<?> lt1 = nd1.selectNodes("./item");
                        LinkedList<CheckItem> lkl = new LinkedList<CheckItem>();
                        Iterator<?> it1 = lt1.iterator();
                        while (it1.hasNext()) {
                            CheckItem ci = new CheckItem(((Node)it1.next()).asXML());
                            lkl.add(ci);
                        }
                        groupList.put(name, lkl);
                    }
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            long totalTime = System.currentTimeMillis() - initTime;
            NeptusLog.pub().info(this + ": Total checklist load time: " + totalTime + " ms.");
            isLoadOk = false;
            return false;
        }
        long totalTime = System.currentTimeMillis() - initTime;
        NeptusLog.pub().info(this + ": Total checklist load time: " + totalTime + " ms.");
        isLoadOk = true;
        return true;
    }


    public boolean loadFile(File file) {
        return loadFile(file.getAbsolutePath());
    }

    /**
     * @param url
     */
    public boolean loadFile(String url) {
        originalFilePath = new File(url).getAbsolutePath();

        String fileAsString = FileUtil.getFileAsString(url);

        /*
         * try { String sLoc = new File
         * (ConfigFetch.getChecklistSchemaLocation())
         * .getAbsoluteFile().toURI().toASCIIString(); XMLValidator xmlVal = new
         * XMLValidator(fileAsString, sLoc); xmlVal.validate(); } catch
         * (Exception e1) { NeptusLog.pub().error(this, e1); }
         */
        isLoadOk = validate(fileAsString);

        return load(fileAsString);
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
     * @return the version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Returns the isFlat.
     */
    public boolean isFlat() {
        return isFlat;
    }

    /**
     * @param isFlat
     *            The isFlat to set.
     */
    public void setFlat(boolean isFlat) {
        this.isFlat = isFlat;
    }

    /**
     * @return Returns the groupList.
     */
    public LinkedHashMap<String, LinkedList<CheckItem>> getGroupList() {
        return groupList;
    }

    /**
     * @return Returns the originalFilePath.
     */
    public String getOriginalFilePath() {
        return originalFilePath;
    }

    /**
     * @param originalFilePath
     *            The originalFilePath to set.
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
        Element root = document.addElement( rootElementName );
        
        root.addComment(ConfigFetch.getSaveAsCommentForXML());
        
        root.addAttribute("name", getName());
        
        if (getVersion() != null && !"".equalsIgnoreCase(getVersion())) {
            root.addAttribute("version", getVersion());
        }
        
        if (getDescription() != null) {
            //root.addAttribute("description", getDescription());
            root.addElement("description").setText(getDescription());
        }
        
        if (isFlat()) {
            if (!groupList.isEmpty()) {
                LinkedList<CheckItem> lkl = groupList.values().iterator().next();
                Iterator<CheckItem> it = lkl.iterator();
                while (it.hasNext()) {
                    CheckItem ci = (CheckItem) it.next();
                    root.add(ci.asElement());
                }
            }
        }
        else {
            Iterator<String> it = groupList.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                Element groupElem = root.addElement("group");
                groupElem.addAttribute("name", name);
                LinkedList<CheckItem> lkl = groupList.get(name);
                Iterator<CheckItem> it1 = lkl.iterator();
                while (it1.hasNext()) {
                    CheckItem ci = (CheckItem) it1.next();
                    groupElem.add(ci.asElement());
                }
            }
        }
        
        return document;
    }


    public boolean isLoadOk() {
        return isLoadOk;
    }

    public static boolean validate(String xml) {
        try {

            String sLoc = new File(ConfigFetch.getChecklistSchemaLocation()).getAbsoluteFile()
                    .toURI().toString();
            XMLValidator xmlVal = new XMLValidator(xml, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error("ChecklistType:validate", e);
            return false;
        }
    }
    
    public boolean isTotallyChecked() {
    	//TODO
    	return true;
    }

    public static boolean validate(File file) {
        try {
            // System.err.println(file.getAbsoluteFile().toURI());
            String xml = FileUtil.getFileAsString(file);
            return validate(xml);
        }
        catch (Exception e) {
            NeptusLog.pub().error("ChecklistType:validate", e);
            return false;
        }
    }

    public String toString() {
        return getName();
    }    
 
    public ChecklistMission getCheckListMission() {
    	ChecklistMission clm = new ChecklistMission();
    	clm.setChecklist(this);
    	clm.setId(getName());
    	clm.setName(getName());
    	
    	return clm;
    }
    
    public ChecklistType createCopy() {
        ChecklistType ret = null;
        try {
            ret = (ChecklistType) this.clone();
        }
        catch (Exception e) {
            NeptusLog.pub().error(this + ": Error creating copy of Checklist", e);
        }
        return ret;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
    	ChecklistType tmpChc = new ChecklistType();
    	tmpChc.load(this.asElement());
    	
    	tmpChc.setOriginalFilePath("");
    	return tmpChc;
    }
}