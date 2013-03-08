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
 * 20??/??/??
 * $Id:: ManeuverTransition.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.planeditor;

import pt.up.fe.dceg.neptus.graph.DefaultEdge;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
/**
 * 
 * @author ZP
 * @author pdias
 */
public class ManeuverTransition extends DefaultEdge<Object> {
	
    private TransitionConditionAction transitionCondAction = new TransitionConditionAction();
    
	public ManeuverTransition() {
		super();
		setUserObject(transitionCondAction);
	}

	/**
     * @return the transitionCondAction
     */
    public TransitionConditionAction getTransitionCondAction() {
        return transitionCondAction;
    }
    
    /**
     * @param transitionCondAction the transitionCondAction to set
     */
    public void setTransitionCondAction(TransitionConditionAction transitionCondAction) {
        this.transitionCondAction = transitionCondAction;
        setUserObject(transitionCondAction);
    }
	
	@Override
	public DefaultProperty[] getProperties() {
		
		DefaultProperty label = PropertiesEditor.getPropertyInstance("ID", String.class, getID(), false);		
        DefaultProperty condition = PropertiesEditor.getPropertyInstance("Condition", String.class,
                transitionCondAction.getCondition().getStringRepresentation(), false);
        DefaultProperty action = PropertiesEditor.getPropertyInstance("Action", String.class,
                transitionCondAction.getAction().getStringRepresentation(), false);
		
		return new DefaultProperty[] {label, condition, action};
	}
	
	@Override
	public void setProperties(Property[] properties) {
		// Properties are not edditable...
	}
}
