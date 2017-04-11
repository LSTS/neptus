/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * 09/03/2017
 */
package pt.lsts.neptus.plugins.nvl_runtime;

import pt.lsts.neptus.nvl.runtime.PayloadComponent;

/**
 * @author keila
 *
 */
public class NeptusPayloadAdapter implements PayloadComponent {
 
    private final String name;
    private final int range;//in Meters
    private final int frequency;
    public NeptusPayloadAdapter(String n, int r,int f){ //range in meters
        name = n;
        range = r;
        frequency = f;
    }
    public NeptusPayloadAdapter(String n){ //sensor range 0 if null
        name = n;
        range = 0;
        frequency = 0;
    }
    
    @Override
    public String getName() {
        return name;
    }
    @Override
    public int getRange() {
        return range;
    }
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (! (o instanceof PayloadComponent)) {
            return false;
        }
        PayloadComponent pc = (PayloadComponent) o;
        
        return pc.getName().equals(name) && pc.getRange() == range && pc.getFrequency() == frequency;
        
    }
    
    /**
     * Compares if the other payload can perform in the same capacities as this one
     * @param other
     * @return
     */
    public boolean hasMinCapacity(PayloadComponent other) {
        return other.getName().equals(name) && compatibleRange(other) && compatibleFrequency(other);
    }
    
    /**
     * If the payload your comparing to has at least the same capacity
     * @param other
     * @return 
     */
    public boolean compatibleRange(PayloadComponent other){
        return other.getRange()>= this.getRange();
    }
    /**
     * If the payload your comparing to has at least the same frequency? //TODO does this makes sense?
     * @param other
     * @return 
     */
    public boolean compatibleFrequency(PayloadComponent other){
        return other.getFrequency()>= this.getFrequency();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.PayloadComponent#getFrequency()
     */
    @Override
    public int getFrequency() {
        return frequency;
    }

}
