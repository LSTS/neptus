/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2009/06/07
 */
package pt.lsts.neptus.plugins.alarms;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.AlarmProviderOld;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author ZP
 * 
 */
public abstract class SimpleAlarm extends ConsolePanel implements AlarmProviderOld, IPeriodicUpdates {

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
            if (getMainpanel().getAlarmlistener() != null)
                getMainpanel().getAlarmlistener().updateAlarmsListeners(this);

            display.setState(state);
            display.setToolTipText(message);

            if (state == AlarmProviderOld.LEVEL_4 && beepOnError)
                Toolkit.getDefaultToolkit().beep();
        }
        return true;
    }
}
