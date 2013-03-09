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
 * Feb 2, 2012
 */
package pt.up.fe.dceg.neptus.alarms;

import pt.up.fe.dceg.neptus.alarms.AlarmManager.AlarmLevel;

/**
 * @author jqcorreia
 *
 */
public class AlarmMessage implements Comparable<AlarmMessage>{
    String message;
    AlarmLevel level;
    long time;
    
    
    public AlarmMessage(String message, AlarmLevel level, long time) {
        this.message = message;
        this.level = level;
        this.time = time;
    }
    
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    /**
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * @return the level associated with this message
     */
    public AlarmLevel getLevel() {
        return level;
    }
    /**
     * @param level the level to set
     */
    public void setLevel(AlarmLevel level) {
        this.level = level;
    }
    /**
     * @return the time of this message (Epoch time in seconds)
     */
    public long getTime() {
        return time;
    }
    /**
     * @param time the time to set (Epoch time in seconds)
     */
    public void setTime(long time) {
        this.time = time;
    }
    
    /**
     * Comparator, in this case order the messages by time
     */
    @Override
    public int compareTo(AlarmMessage o) {
        return (int)(this.time - o.time);
    }
}
