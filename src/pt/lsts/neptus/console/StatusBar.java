/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.console;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.events.ConsoleEventNewNotification;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.notifications.NotificationsDialog;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.DateTimeUtil;

import com.google.common.eventbus.Subscribe;

/**
 * @author Hugo, PDias
 */
public class StatusBar extends JPanel {
    private static final long serialVersionUID = -945440076259058094L;

    private static final int FONT_SIZE = 12;

    private JLabel clock;
    private JLabel mainSystem;
    private JLabel plan;
    protected JButton notificationButton;
    private ConsoleLayout console;
    private NotificationsDialog notificationsDialog;
    private int notificationCount = 0;

    protected Timer clockTimer = null;
    protected TimerTask clockTimerTask = null;

    public StatusBar(ConsoleLayout console, NotificationsDialog notificationsDialog) {
        super();

        this.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.setPreferredSize(new Dimension(console.getWidth(), 25));
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.notificationsDialog = notificationsDialog;
        this.console = console;
        this.initialize();
        NeptusEvents.register(this, console);
        this.setVisible(true);
    }

    public void initialize() {
        JLabel labelMainSystem = new JLabel(I18n.text("System") + ": ");
        labelMainSystem.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
        this.add(labelMainSystem);

        /// N/A Not Available
        mainSystem = new JLabel(I18n.text("N/A"));
        mainSystem.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
        this.add(mainSystem);

        this.add(Box.createRigidArea(new Dimension(5, 0)));
        JLabel labelPlanVehicle = new JLabel(I18n.text("Plan") + ": ");
        labelPlanVehicle.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
        this.add(labelPlanVehicle);

        plan = new JLabel(I18n.text("N/A"));
        plan.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
        this.add(plan);

        this.add(Box.createHorizontalGlue());

        // Clock
        clock = new JLabel();
        clock.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
        clock.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(clock);
        startClock();

        this.add(Box.createRigidArea(new Dimension(5, 0)));

        notificationButton = new JButton(I18n.text("Notifications"));
        notificationButton.setName("notification");
        notificationButton.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
        notificationButton.setAction(new AbstractAction(I18n.text("Notifications")) {
            private static final long serialVersionUID = 1L;
            private boolean show = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                this.show = !show;
                if (this.show == false && notificationsDialog.isVisible() == false) {
                    this.show = true;
                }
                notificationsDialog.visible(this.show);
                notificationCount = 0;
                notificationButton.setText(I18n.text("Notifications"));
                notificationButton.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
            }
        });
        this.add(notificationButton);
    }

    public void startClock() {
        clockTimer = new Timer("status bar clock", true);
        clockTimerTask = new TimerTask() {
            @Override
            public void run() {
                /// Universal Time Coordinated
                String clockStr = DateTimeUtil.timeUTCFormaterNoSegs3.format(new Date(System.currentTimeMillis()))
                        + " " + I18n.text("UTC");
                clock.setText(clockStr);
            }
        };
        clockTimer.schedule(clockTimerTask, 100, 800);
    }

    public void stopClock() {
        clock.setText("");
        if (clockTimerTask != null)
            clockTimerTask.cancel();
        if (clockTimer != null)
            clockTimer.cancel();
    }

    public void clean() {
        this.stopClock();
        NeptusEvents.unregister(this, console);
    }

    /*
     * EVENTS
     */
    @Subscribe
    public void onMainSystemChange(ConsoleEventMainSystemChange e) {
        mainSystem.setText(e.getCurrent());
    }

    @Subscribe
    public void onPlanChange(ConsoleEventPlanChange e) {
        plan.setText(e.getCurrent() != null ? e.getCurrent().getId() : I18n.text("N/A"));
    }

    @Subscribe
    public void onNewNotification(ConsoleEventNewNotification e) {
        notificationCount++;
        notificationButton.setText(I18n.textf("%n Notifications", notificationCount));
        notificationButton.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
    }
}
