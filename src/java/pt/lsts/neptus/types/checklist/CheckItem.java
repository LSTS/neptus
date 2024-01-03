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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.XmlOutputMethods;

/**
 * @author Paulo Dias
 */
public class CheckItem implements XmlOutputMethods {
    protected static final String DEFAULT_ROOT_ELEMENT = "item";

    protected String name = "";
    protected boolean isChecked = false;
    protected boolean skiped = false;
    
    protected double runDurationSeconds=Double.NaN;
  
	protected String dateChecked = "";
    protected String note = "";
    
    private Vector<CheckAutoSubItem> autoSubItems=new Vector<CheckAutoSubItem>();  
    
    private Document doc;

    public CheckItem(String xml) {
        load (xml);
    }
    
    public CheckItem() {
        super();
    }
    
    /**
     * @param url
     */
    public boolean load (String xml) {
        String fileAsString = xml;

        try {
            doc = DocumentHelper.parseText(fileAsString);
            this.setName(doc.selectSingleNode("/item/@name").getText());
            Node nd = doc.selectSingleNode("/item/note");
            if (nd != null)
                this.setNote(nd.getText());
            
            nd = doc.selectSingleNode("/item/@checked");
            if (nd != null)
                setChecked(Boolean.parseBoolean(nd.getText()));
            else
                setChecked(false);
            
            nd = doc.selectSingleNode("/item/@skiped");
            if (nd != null)
                setSkiped(Boolean.parseBoolean(nd.getText()));
            else
                setChecked(false);

            nd = doc.selectSingleNode("/item/@date-checked");
            if (nd != null)
                setDateChecked(nd.getText());
            else
                setDateChecked("");
            
            nd = doc.selectSingleNode("/item/@run-duration-seconds");
            if (nd != null) {
                try {
                    setRunDurationSeconds(Double.parseDouble(nd.getText()));
                } catch (Exception e) {
                    e.printStackTrace();
                    setRunDurationSeconds(Double.NaN);
                }
            }
            else
                setRunDurationSeconds(Double.NaN);
            
            List<?> lt = doc.selectNodes("/item/subItems/*");
            
            if (!lt.isEmpty()) {
                Iterator<?> it = lt.iterator();
                while (it.hasNext()) {
                	Node aux=((Node)it.next());
                	CheckAutoSubItem casi = null;
                	if(aux.getName().equals("variableTestRange")) {
                		casi = new CheckAutoVarIntervalItem(aux.asXML());
                	}
                	else if(aux.getName().equals("userLog")) {
                		casi = new CheckAutoUserLogItem(aux.asXML());
                	}
                	else if(aux.getName().equals("userAction")) {
                		casi = new CheckAutoUserActionItem(aux.asXML());
                	}
                	
                	if(casi != null)
                		autoSubItems.add(casi);
                }
            }

        }
        catch (DocumentException e) {
            NeptusLog.pub().error(this, e);
            return false;
        }
        return true;
    }

    public void addAutoSubItem(CheckAutoSubItem asi) {
        autoSubItems.add(asi);
    }

    public void removeAutoSubItem(CheckAutoSubItem asi) {
        autoSubItems.remove(asi);
    }

    public Vector<CheckAutoSubItem> getAutoSubItems() {
        return autoSubItems;
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
     * @return Returns the dateChecked.
     */
    public String getDateChecked() {
        return dateChecked;
    }

    /**
     * @param dateChecked The dateChecked to set.
     */
    public void setDateChecked(String dateChecked) {
        this.dateChecked = dateChecked;
    }

    public double getRunDurationSeconds() {
        return runDurationSeconds;
    }

    public void setRunDurationSeconds(double runDurationSeconds) {
        this.runDurationSeconds = runDurationSeconds;
    }

    /**
     * @return Returns the isChecked.
     */
    public boolean isChecked() {
        return isChecked;
    }

    /**
     * @param isChecked The isChecked to set.
     */
    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    /**
     * @return Returns the note.
     */
    public String getNote() {
        return note;
    }

    /**
     * @param note The note to set.
     */
    public void setNote(String note) {
        if (note == null)
            this.note = "";
        this.note = note;
    }

    public boolean isSkiped() {
        return skiped;
    }

    public void setSkiped(boolean skiped) {
        this.skiped = skiped;
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

        root.addAttribute("name", getName());
        root.addAttribute("checked", Boolean.toString(isChecked()));
        if (!isSkiped())
            root.addAttribute("skiped", Boolean.toString(isSkiped()));
        if (isChecked()) {
            if (!getDateChecked().equalsIgnoreCase(""))
                root.addAttribute("date-checked", getDateChecked());

            if (!Double.isNaN(getRunDurationSeconds()))
                root.addAttribute("run-duration-seconds", getRunDurationSeconds() + "");
        }
        
        if (!getNote().equalsIgnoreCase(""))
            root.addElement("note").setText(getNote());
        
        if (!autoSubItems.isEmpty()) {
            Element subItem = DocumentHelper.createElement("subItems");

            for (CheckAutoSubItem casi : autoSubItems) {
                subItem.add(casi.asElement());
            }
            if (!subItem.content().isEmpty())
                root.add(subItem);
        }

        return document;
    }
}
