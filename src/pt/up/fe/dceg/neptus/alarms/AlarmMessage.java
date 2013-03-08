/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Feb 2, 2012
 * $Id:: AlarmMessage.java 9615 2012-12-30 23:08:28Z pdias                      $:
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
