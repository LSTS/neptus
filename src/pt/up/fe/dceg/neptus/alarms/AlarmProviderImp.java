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
 * Mar 20, 2012
 */
package pt.up.fe.dceg.neptus.alarms;

import pt.up.fe.dceg.neptus.alarms.AlarmManager.AlarmLevel;
import pt.up.fe.dceg.neptus.console.plugins.EntityStatePanel;

/**
 * A simple implementation of a AlarmProvider meant to be used by components who are able to issue various alarms (see {@link EntityStatePanel} for an example).
 * It is a basic implementation with a full constructor and a getter/setter for every field.
 * @author jqcorreia
 */
public class AlarmProviderImp  implements AlarmProvider {

    AlarmLevel state;
    String name;
    String message;
    
    public AlarmProviderImp(AlarmLevel state, String name, String message) {
        this.state = state;
        this.name = name;
        this.message = message;
    }
    
    @Override
    public AlarmLevel getAlarmState() {
        return state;
    }

    @Override
    public String getAlarmName() {
        return name;
    }

    @Override
    public String getAlarmMessage() {
        return message;
    }

    public void setState(AlarmLevel state) {
        this.state = state;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
