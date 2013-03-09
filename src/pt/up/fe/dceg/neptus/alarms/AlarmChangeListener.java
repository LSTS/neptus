/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: jqcorreia
 * Jan 27, 2012
 */
package pt.up.fe.dceg.neptus.alarms;

import pt.up.fe.dceg.neptus.alarms.AlarmManager.AlarmLevel;

/**
 * This listener interface is meant to be registered with AlarmManager and provides information when singular alarm state changes
 * Also triggers when the general (maximum) alarm level of the system changes.
 * @author jqcorreia
 */
public interface AlarmChangeListener {
    
    /**
     * Called when an alarm provider state changes.
     *
     * @param provider the provider which state changed
     */
    public void alarmStateChanged(AlarmProvider provider);
    
    /**
     * Called when an max alarm level changes.
     *
     * @param maxlevel the new maximum alarm level
     */
    public void maxAlarmStateChanged(AlarmLevel maxlevel);
    
    /**
     * Called when an alarm provider is added to AlarmManager
     *
     * @param provider the provider which was added
     */
    public void alarmAdded(AlarmProvider provider);
    
    /**
     * Called when an alarm provider is removed from AlarmManager
     *
     * @param provider the provider which was removed
     */
    public void alarmRemoved(AlarmProvider provider);
}
