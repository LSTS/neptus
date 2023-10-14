/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Hugo Dias
 * Nov 12, 2012
 */
package pt.lsts.neptus.console.notifications;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventNewNotification;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;

/**
 * @author Hugo
 * 
 */
public class NotificationsDialog extends JDialog implements WindowFocusListener, AWTEventListener {
    private static final long serialVersionUID = -344983838194932720L;
    private ConsoleLayout console;
    private JList<Notification> jList;
    private JPanel options;
    private JCheckBox check;
    private JButton clear;
    private JScrollPane listScroller;
    private NotificationsCollection notifications;
    private NotificationsGlassPane glassPane;
    private boolean popupsEnabled = true;
    private boolean focus = false;

    /**
     * Construtor
     * @param notifications
     * @param console
     */
    public NotificationsDialog(final NotificationsCollection notifications, ConsoleLayout console) {
        super(console);
        this.console = console;
        this.notifications = notifications;
        NeptusEvents.register(this, console);
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
        this.glassPane = new NotificationsGlassPane(console);

        this.setVisible(false);
        this.setUndecorated(true);
        this.setLayout(new BorderLayout());
        this.setSize(new Dimension(500, 200));
        this.addWindowFocusListener(this);

        // list
        jList = new JList<Notification>();
        jList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jList.setLayoutOrientation(JList.VERTICAL);
        jList.setCellRenderer(new NotificationRenderer());
        jList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false && jList.getSelectedIndex() != -1) {
                    Notification noty = jList.getSelectedValue();
                    glassPane.addAtomic(noty);
                }
            }
        });
        listScroller = new JScrollPane(jList);
        this.add(listScroller, BorderLayout.CENTER);

        // options panel
        options = new JPanel();
        options.setName("options");
        options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
        check = new JCheckBox(new AbstractAction(I18n.text("Disable popups")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                popupsEnabled = !popupsEnabled;
            }
        });
        clear = new JButton(new AbstractAction(I18n.text("Clear")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
        options.add(Box.createRigidArea(new Dimension(5, 0)));
        options.add(check);
        options.add(Box.createHorizontalGlue());
        options.add(clear);
        this.add(options, BorderLayout.SOUTH);
    }

    /**
     * Positions the dialog at bottom left side relative to the console
     */
    public void position() {
        Point p = console.getLocationOnScreen();
        p.x += console.getWidth() - (getWidth() + 10);
        p.y += console.getHeight() - (getHeight() + 32);

        setLocation(p);
    }

    /**
     * Sets the visiblity flag of the dialog
     * 
     * @param flag
     */
    public void visible(boolean flag) {
        this.setVisible(flag);
        glassPane.clear();
        if (flag) {
            this.position();
        }
    }

    /**
     * Clears the list and glasspane (popups)
     */
    public void clear() {
        notifications.clear();
        jList.setListData(new Notification[0]);
        jList.repaint();
        glassPane.clear();
    }

    /*
     * EVENTS
     */
    @Subscribe
    public void onNewNotification(ConsoleEventNewNotification e) {
        List<Notification> n = notifications.getList();
        Collections.sort(n);
        jList.setListData(n.toArray(new Notification[0]));
        if (popupsEnabled) {
            glassPane.add(e.getNoty());
        }
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        focus = true;
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        focus = false;
    }

    private class NotificationRenderer extends JLabel implements ListCellRenderer<Notification> {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<? extends Notification> list, Notification value,
                int index, boolean isSelected, boolean cellHasFocus) {
            setText("<html> " + value.getTimeText() + " [ <b width='100px; display: inline'>" + value.getSrc()
                    + "</b> ] " + value.getTitle() + "</html>");

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                switch (value.getType()) {
                    case ERROR:
                        setBackground(new Color(0xE6C0C0));
                        break;
                    case SUCCESS:
                        setBackground(new Color(0xC5E4B9));
                        break;
                    case INFO:
                        setBackground(new Color(0x95CDE8));
                        break;
                    case WARNING:
                        setBackground(new Color(0xF8F0C3));
                        break;
                    default:
                        break;
                }
                setForeground(new Color(0x333333));
            }
            setEnabled(list.isEnabled());
            setFont(new Font("Arial", Font.PLAIN, 12));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            return this;
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event instanceof MouseEvent && this.isVisible()) {
            MouseEvent me = (MouseEvent) event;
            String name = me.getComponent().getName() == null ? "" : me.getComponent().getName();
            if (!name.equals("notification") && focus == false)
                this.visible(false);
        }
    }
}
