/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 23, 2005
 */
package pt.lsts.neptus.types.mission;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * This class holds information about a transition (in a plan graph)
 * @author ZP
 */
public class TransitionType implements XmlOutputMethods {
    
    String id;
    String sourceManeuver;
    String targetManeuver;
    ConditionType condition;
    ActionType action;
	private static final String DEFAULT_ROOT_ELEMENT = "edge";
    
    /**
     * Creates a new transition between two maneuvers
     * @param source The origin (maneuver) of this transition
     * @param target The destiny (maneuver) of this transition
     */
    public TransitionType(String source, String target) {
        setId(NameNormalizer.getRandomID());
        setSourceManeuver(source);
        setTargetManeuver(target);
        
        setCondition(new ConditionType());
    }

    /**
     * Returns this transition's condition
     * @return
     */
    public ConditionType getCondition() {
        return condition;
    }
    
    /**
     * Sets the transition condition (A.K.A. guard)
     * @param condition
     */
    public void setCondition(ConditionType condition) {
        this.condition = condition;
    }
    
    /**
     * @return the action
     */
    public ActionType getAction() {
        return action;
    }
    
    /**
     * @param action the action to set
     */
    public void setAction(ActionType action) {
        this.action = action;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSourceManeuver() {
        return sourceManeuver;
    }
    
    public void setSourceManeuver(String sourceManeuver) {
        this.sourceManeuver = sourceManeuver;
    }
    
    public String getTargetManeuver() {
        return targetManeuver;
    }
    
    public void setTargetManeuver(String targetManeuver) {
        this.targetManeuver = targetManeuver;
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
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
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
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("id").setText(getId());
        root.addElement("source").setText(getSourceManeuver());
        root.addElement("target").setText(getTargetManeuver());
        root.addElement("guard").setText(getCondition().toString());
        if (getAction() != null && !"".equalsIgnoreCase(getAction().toString()))
            root.addElement("actions").setText(getAction().toString());

        return document;
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + sourceManeuver + " -> " + targetManeuver + " (" + condition
                + (getAction() != null && !"".equalsIgnoreCase(getAction().toString())?" / " + action:"") + ")";
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        TransitionType clone = new TransitionType(this.sourceManeuver, this.targetManeuver);
        clone.id = this.id;
        clone.condition = (ConditionType) this.condition.clone();
        if (getAction() != null)
            clone.action = (ActionType) this.action.clone();
        else
            clone.action = this.action;
        return clone;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransitionType))
            return super.equals(obj);
        TransitionType other = (TransitionType) obj;
        
        if (!this.getId().equals(other.getId()))
            return false;
        if (!this.getSourceManeuver().equals(other.getSourceManeuver()))
            return false;
        if (!this.getTargetManeuver().equals(other.getTargetManeuver()))
            return false;
        if (!this.getCondition().toString().equals(other.getCondition().toString()))
            return false;
        if (!this.getAction().toString().equals(other.getAction().toString()))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getId().hashCode();
        hash = 31 * hash + getSourceManeuver().hashCode();
        hash = 31 * hash + getTargetManeuver().hashCode();
        hash = 31 * hash + getCondition().toString().hashCode();
        hash = 31 * hash + getAction().toString().hashCode();
        return hash;
    }
}
