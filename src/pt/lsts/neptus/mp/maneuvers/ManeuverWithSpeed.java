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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 07/07/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;

/**
 * @author pdias
 *
 */
public interface ManeuverWithSpeed {
    /**
     * @return The speed value.
     */
    public double getSpeed();
    
    /**
     * Sets the speed value
     * 
     * @param speed
     */
    public void setSpeed(double speed);
    
    /**
     * @return The speed units.
     */
    public SPEED_UNITS getSpeedUnits();
    
    /**
     * Sets the speed units
     * 
     * @param units
     */
    public void setSpeedUnits(Maneuver.SPEED_UNITS units);
}
