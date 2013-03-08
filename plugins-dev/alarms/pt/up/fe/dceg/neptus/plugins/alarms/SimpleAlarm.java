/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/06/07
 * $Id:: SimpleAlarm.java 9616 2012-12-30 23:23:22Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.plugins.alarms;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.AlarmProviderOld;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author ZP
 * 
 */
public abstract class SimpleAlarm extends SimpleSubPanel implements AlarmProviderOld, IPeriodicUpdates {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Period between updates", description = "Ammount of time (in milliseconds) to wait between updates")
    public long updatePeriod = 1000;

    @NeptusProperty(name = "Beep on error", description = "Produce an audible effect in the case of an error")
    public boolean beepOnError = false;

    protected AlarmDisplay display = new AlarmDisplay();

    public AlarmDisplay getDisplay() {
        return display;
    }

    private int state = AlarmProviderOld.LEVEL_NONE;
    private String message = "";

    public SimpleAlarm(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        display.setText(getTextToDisplay());
        add(display, BorderLayout.CENTER);
        setPreferredSize(new Dimension(118, 29));
        setMinimumSize(new Dimension(10, 10));
        setSize(118, 29);

    }

    protected String getTextToDisplay() {
        return getName();
    }

    @Override
    public int sourceState() {
        return getAlarmState();
    }

    @Override
    public long millisBetweenUpdates() {
        return updatePeriod;
    }

    @Override
    public boolean update() {
        String prevMessage = message;
        int prevState = state;

        state = getAlarmState();
        message = getAlarmMessage();

        if (state != prevState || !message.equals(prevMessage)) {
            getMainpanel().getAlarmlistener().updateAlarmsListeners(this);

            display.setState(state);
            display.setToolTipText(message);

            if (state == AlarmProviderOld.LEVEL_4 && beepOnError)
                Toolkit.getDefaultToolkit().beep();
        }
        return true;
    }
}
