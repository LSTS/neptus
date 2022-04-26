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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Apr 30, 2013
 */
package pt.lsts.neptus.plugins.trex.goals;

import java.util.Collection;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.TrexAttribute;

/**
 * @author zp
 *
 */
public class GoalWithoutAttributes extends TrexGoal {

    
    public GoalWithoutAttributes(String timeline, String predicate) {
        super(timeline, predicate);
    }
    
     @Override
    public Collection<TrexAttribute> getAttributes() {
        return new Vector<TrexAttribute>();
    }

    @Override
    public void parseAttributes(Collection<TrexAttribute> attributes) {

    }

    @Override
    public void setSpecificProperties(Collection<Property> properties) {
    }

    @Override
    public Collection<DefaultProperty> getSpecificProperties() {
        return new Vector<DefaultProperty>();
    }

    @Override
    public String toJson() {
        return "{" + "\"on\": \"" + super.timeline + "\",\"pred\": \"" + super.predicate + "\"," + "}";
    }

}
