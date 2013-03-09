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
 * 05/06/2011
 */
package pt.up.fe.dceg.neptus.types.mission;

/**
 * @author pdias
 *
 */
public class ActionType {

    String action = "";
    
//    public boolean evaluate() {
//        return true;
//    }
    
    public String getStringRepresentation() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String toString() {
    	return action;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() {
        ActionType clone = new ActionType();
        clone.setAction(action);
        return clone;
    }
}
