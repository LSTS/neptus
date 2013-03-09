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
 * Mar 23, 2005
 */
package pt.up.fe.dceg.neptus.types.mission;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.util.NameNormalizer;

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

        root.addElement("id").setText(getId());
        root.addElement("source").setText(getSourceManeuver());
        root.addElement("target").setText(getTargetManeuver());
        root.addElement("guard").setText(getCondition().toString());
        if (!"".equalsIgnoreCase(getAction().toString()))
            root.addElement("actions").setText(getAction().toString());

        return document;
    }

    @Override
    public String toString() {
        return "[" + getId() + "] " + sourceManeuver + " -> " + targetManeuver + " (" + condition
                + (!"".equalsIgnoreCase(getAction().toString())?" / " + action:"") + ")";
    }
}
