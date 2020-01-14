/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.planeditor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.graph.DefaultEdge;
import pt.lsts.neptus.gui.PropertiesEditor;
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
