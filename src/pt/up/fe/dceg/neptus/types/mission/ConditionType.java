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
 * $Id:: ConditionType.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.types.mission;

/**
 * @author zecarlos
 *
 */
public class ConditionType {

    String condition = "true";
    
    public boolean evaluate() {
        return true;
    }
    
    public String getStringRepresentation() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public String toString() {
    	return condition;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() {
        ConditionType clone = new ConditionType();
        clone.setCondition(condition);
        return clone;
    }
}
