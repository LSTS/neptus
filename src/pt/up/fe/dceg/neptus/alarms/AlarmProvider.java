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
 * Author: José Correia
 * Jan 26, 2012
 */
package pt.up.fe.dceg.neptus.alarms;

import pt.up.fe.dceg.neptus.alarms.AlarmManager.AlarmLevel;

/**
 * The Interface AlarmProvider.
 *
 * Every object meant to launch an alarm must implement this interface and 
 * be added to the console alarms list via {@link AlarmManager#addAlarmProvider(AlarmProvider)}
 * @author jqcorreia
 */
public interface AlarmProvider {
    
    /**
     * Method to return the current alarm level of the provider
     *
     * @return the alarm state
     */
    public AlarmLevel getAlarmState();
    
    /**
     * Method to return the alarm name.
     *
     * @return the alarm name
     */
    public String getAlarmName();
    
    /**
     * Implement this method to return the current alarm message ( this messages are disassociated 
     * with the messages used in {@link AlarmManager#postMessage(AlarmProvider, String, AlarmLevel)}
     * This message describes the present state of the alarm in a more verbose way
     *
     * @return the alarm message
     */
    public String getAlarmMessage();
}
