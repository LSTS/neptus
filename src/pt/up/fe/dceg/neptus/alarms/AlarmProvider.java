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
