/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: StatusBar.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.console;

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

import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventNewNotification;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventPlanChange;
import pt.up.fe.dceg.neptus.console.notifications.NotificationsDialog;
import pt.up.fe.dceg.neptus.events.NeptusEvents;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;

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
