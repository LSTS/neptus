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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;

/**
 * This class is responsible for gathering all the information about the alarms present in a given console
 * Every console as an unique AlarmManager and all the components of the console can add, remove and post messages related to the alarm that they implement
 * Get a reference to AlarmManager normally using {@link ConsoleLayout#getAlarmManager()} 
 * @author jqcorreia
 */
public class AlarmManager {
    
    /**
     * Every AlarmProvider has an inherent alarm level and this enumeration deals with the fixed values of this alarm levels
     */
    public enum AlarmLevel {
        UNKNOWN(-1), NORMAL(0), INFO(1), FAULT(2), ERROR(3), FAILURE(4);
        int value;

        private static final Map<Integer, AlarmLevel> lookup = new HashMap<Integer, AlarmLevel>();

        static {
            for (AlarmLevel s : EnumSet.allOf(AlarmLevel.class))
                lookup.put(s.getValue(), s);
        }

        private AlarmLevel(int val) {
            value = val;
        }

        /**
         * Gets the value.
         *
         * @return the value
         */
        public int getValue() {
            return value;
        }

        /**
         * Gets the.
         *
         * @param i the i
         * @return the alarm level
         */
        public static AlarmLevel get(int i) {
            return lookup.get(i);
        }
    }
    
    ArrayList<AlarmProvider> alarmProviders = new ArrayList<AlarmProvider>();
    ArrayList<AlarmChangeListener> alarmListeners = new ArrayList<AlarmChangeListener>();
    LinkedHashMap<AlarmProvider, ArrayList<AlarmMessage>> messages = new LinkedHashMap<AlarmProvider, ArrayList<AlarmMessage>>();
    
    AlarmLevel maxAlarmLevel;

    public AlarmManager() {
        
        maxAlarmLevel = AlarmLevel.NORMAL;
    }

    /**
     * Removes an alarm provider.
     *
     * @param provider the provider
     */
    public void removeAlarmProvider(AlarmProvider provider) {
        if (messages.containsKey(provider)) {
            messages.remove(provider);
        }
        if (alarmProviders.contains(provider)) {
            alarmProviders.remove(provider);
            notifyAlarmRemoved(provider);
        }
    }

    /**
     * Adds an alarm provider.
     *
     * @param provider the provider
     */
    public void addAlarmProvider(AlarmProvider provider) {
        if (!messages.containsKey(provider)) {
            messages.put(provider, new ArrayList<AlarmMessage>());
        }
        if (!alarmProviders.contains(provider)) {
            alarmProviders.add(provider);
            notifyAlarmAdded(provider);
        }

    }

    /**
     * Removes the alarm listener.
     *
     * @param provider the provider that you the disassociate the listener from 
     */
    public void removeAlarmListener(AlarmChangeListener provider) {
        if (alarmListeners.contains(provider)) {
            alarmListeners.remove(provider);
        }
    }

    /**
     * Adds an alarm listener.
     *
     * @param provider the provider to be listened
     */
    public void addAlarmListener(AlarmChangeListener provider) {
        if (!alarmListeners.contains(provider)) {
            alarmListeners.add(provider);
        }
    }
    
    
    /**
     * Calculate the max alarm level and notify the listeners on change.
     */
    private void updateMaxAlarmLevel() {
        AlarmLevel currentMaxAlarmLevel = maxAlarmLevel;
        AlarmLevel max = AlarmLevel.NORMAL;
        AlarmLevel current;
        for (AlarmProvider ap : alarmProviders) {
            current = ap.getAlarmState();
            if (current.value > max.value) {
                max = current;
            }
        }
        maxAlarmLevel = max;
        if(currentMaxAlarmLevel != maxAlarmLevel) {
            notifyMaxLevelChange();
        }
    }

    /**
     * Gets the max alarm level.
     *
     * @return the max alarm level
     */
    public AlarmLevel getMaxAlarmLevel() {
        return maxAlarmLevel;
    }

    /**
     * Gets the alarm providers.
     *
     * @return the list of alarm providers
     */
    public ArrayList<AlarmProvider> getAlarmProviders() {
        return alarmProviders;
    }
    
    /**
     * This is the entry-point for alarm notifications, used to update the global status vs. periodic updates.
     * 
     * @param provider AlarmProvider which state has changed
     */
    public void notifyAlarmChange(AlarmProvider provider) {
        for (AlarmChangeListener acl : alarmListeners) {
            acl.alarmStateChanged(provider);
        }
        updateMaxAlarmLevel();
    }
    
    /**
     * Notify all the listeners that max alarm level changed.
     */
    public void notifyMaxLevelChange() {
        for(AlarmChangeListener acl : alarmListeners) {
            acl.maxAlarmStateChanged(maxAlarmLevel);
        }
    }
    
    /**
     *  Notify all the listeners that an alarm was added.
     *
     * @param provider the provider
     */
    public void notifyAlarmAdded(AlarmProvider provider)
    {
        for(AlarmChangeListener acl: alarmListeners) {
            acl.alarmAdded(provider);
        }
    }
    
    /**
     * Notify all the listeners that an alarm was added removed.
     *
     * @param provider the provider
     */
    public void notifyAlarmRemoved(AlarmProvider provider)
    {
        for(AlarmChangeListener acl: alarmListeners) {
            acl.alarmRemoved(provider);
        }
    }
    
    /**
     * Post message asssociated with an AlarmProvider.
     *
     * @param provider the provider
     * @param msg alarm message to be issued
     * @param level the level of the message (usually the current alarm provider level)
     */
    public void postMessage(AlarmProvider provider, String msg, AlarmLevel level) {
        messages.get(provider).add(new AlarmMessage(msg, level, System.currentTimeMillis()));
    }
    
    /**
     * Removes the message.
     *
     * @param provider the provider
     * @param msg the msg
     */
    public void removeMessage(AlarmProvider provider, String msg) {
        //TODO implement message removal mechanism (if needed)
    }
    
    /**
     * Gets the messages from provider.
     *
     * @param provider the provider
     * @return the messages from provider
     */
    public ArrayList<AlarmMessage> getMessagesFromProvider(AlarmProvider provider) {
        return messages.get(provider);
    }
}
