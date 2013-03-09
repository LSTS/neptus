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
