/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 25, 2014
 */
package pt.lsts.neptus.plugins.pddl;

/**
 * @author zp
 *
 */
public enum PayloadRequirement {
    edgetech(50, -5, -5, 27),    
    klein(50, -5, -5, 27),
    sidescan(30, -3, -3, 4),    
    multibeam(15, 3, 3, 5),
    camera(5, -2, -2, 50),
    ctd(100, 2, 20, 0),
    rhodamine(100, 2, 20, 0);
    
    
    private int swathWidth;
    private int minDepth, maxDepth, consumptionPerHour;
    private PayloadRequirement(int swathWidth, int minDepth, int maxDepth, int consumptionPerHour) {
        this.swathWidth = swathWidth;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.consumptionPerHour = consumptionPerHour;
    }
    /**
     * @return the swathWidth
     */
    public int getSwathWidth() {
        return swathWidth;
    }
    /**
     * @param swathWidth the swathWidth to set
     */
    public void setSwathWidth(int swathWidth) {
        this.swathWidth = swathWidth;
    }
    /**
     * @return the minDepth
     */
    public int getMinDepth() {
        return minDepth;
    }
    /**
     * @param minDepth the minDepth to set
     */
    public void setMinDepth(int minDepth) {
        this.minDepth = minDepth;
    }
    /**
     * @return the maxDepth
     */
    public int getMaxDepth() {
        return maxDepth;
    }
    /**
     * @param maxDepth the maxDepth to set
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }
    /**
     * @return the consumptionPerHour
     */
    public final int getConsumptionPerHour() {
        return consumptionPerHour;
    }    
}
