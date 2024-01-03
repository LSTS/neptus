/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 26/Jun/2005
 */
package pt.lsts.neptus.types.checklist;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.XmlInputMethods;
import pt.lsts.neptus.types.XmlInputMethodsFromFile;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.mission.ChecklistMission;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.XMLValidator;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Refactored in 06/11/2006.
 * @author Paulo Dias
 */
public class ChecklistType implements XmlOutputMethods, XmlInputMethods, XmlInputMethodsFromFile {
    
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
     * @see pt.lsts.neptus.types.XmlInputMethods#load(org.dom4j.Element)
     */
    @Override
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
    @Override
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

    @Override
    public boolean loadFile(File file) {
        return loadFile(file.getAbsolutePath());
    }

    /**
     * @param url
     */
    @Override
    public boolean loadFile(String url) {
        originalFilePath = new File(url).getAbsolutePath();
        String fileAsString = FileUtil.getFileAsString(url);
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
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    @Override
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    @Override
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
    @Override
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
    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    @Override
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
    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        
        root.addComment(ConfigFetch.getSaveAsCommentForXML());
        
        root.addAttribute("name", getName());
        
        if (getVersion() != null && !"".equalsIgnoreCase(getVersion())) {
            root.addAttribute("version", getVersion());
        }
        
        if (getDescription() != null) {
            root.addElement("description").setText(getDescription());
        }
        
        if (isFlat()) {
            if (!groupList.isEmpty()) {
                LinkedList<CheckItem> lkl = groupList.values().iterator().next();
                Iterator<CheckItem> it = lkl.iterator();
                while (it.hasNext()) {
                    CheckItem ci = it.next();
                    root.add(ci.asElement());
                }
            }
        }
        else {
            Iterator<String> it = groupList.keySet().iterator();
            while (it.hasNext()) {
                String name = it.next();
                Element groupElem = root.addElement("group");
                groupElem.addAttribute("name", name);
                LinkedList<CheckItem> lkl = groupList.get(name);
                Iterator<CheckItem> it1 = lkl.iterator();
                while (it1.hasNext()) {
                    CheckItem ci = it1.next();
                    groupElem.add(ci.asElement());
                }
            }
        }
        
        return document;
    }


    @Override
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
            String xml = FileUtil.getFileAsString(file);
            return validate(xml);
        }
        catch (Exception e) {
            NeptusLog.pub().error("ChecklistType:validate", e);
            return false;
        }
    }

    @Override
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