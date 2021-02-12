/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 21, 2012
 */
package pt.lsts.neptus.console.notifications;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.View;

import pt.lsts.neptus.console.notifications.Notification.NotificationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * GlassPane to draw Notification popups
 * 
 * @author Hugo
 * 
 */
public class NotificationsGlassPane extends JPanel {

    private static final long serialVersionUID = -1397790967075620867L;
    private static final int MARGIN_BOTTOM = 5;
    private static final int MARGIN_RIGHT = 2;
    private static final int BOTTOM_GAP = 25;
    private final JFrame frame;
    private List<Notification> list = new ArrayList<>();
    private int currentHeight = BOTTOM_GAP;

    public NotificationsGlassPane(JFrame frame) {
        super(null);
        this.frame = frame;
        this.setupListeners();
        setOpaque(false);
        frame.setGlassPane(this);
        this.setVisible(true);
    }

    public void refresh() {
        currentHeight = BOTTOM_GAP;
        List<Component> comps = Arrays.asList(this.getComponents());
        for (Component component : comps) {
            component.setLocation(this.getWidth() - (component.getWidth() + MARGIN_RIGHT), ((this.getHeight()
                    - (component.getHeight() + MARGIN_BOTTOM) - currentHeight)));
            currentHeight += component.getHeight() + MARGIN_BOTTOM;

        }
    }

    public void clear() {
        this.removeAll();
        list.clear();
        currentHeight = BOTTOM_GAP;
        repaint();
    }

    public void add(final Notification noty) {
        if (list.size() > NotificationsCollection.MAX_SIZE) {
            this.clear();
        }
        if (noty.getType() == NotificationType.INFO)
            return;
        frame.setGlassPane(this);
        this.setVisible(true);
        list.add(noty);
        this.add(build(noty, false));
        this.repaint();
    }

    public void addAtomic(Notification noty) {
        clear();
        currentHeight = 200 + 32;

        frame.setGlassPane(this);
        this.setVisible(true);
        list.add(noty);
        this.add(build(noty, true));
        this.repaint();
    }

    private JLabel build(final Notification noty, boolean atomic) {
        final JLabel label;
        Border paddingBorder = BorderFactory.createEmptyBorder(6, 10, 6, 10);
        Border border;
        String html;
        String msgTxt = noty.getText() != null && !noty.getText().isEmpty() ? noty.getText().replaceAll("\n", "<br>") : "";
        switch (noty.getType()) {
            case SUCCESS:
                html = "<html> <b>" + noty.getSrc() + "</b> " + noty.getTitle() + "<br>" + msgTxt+ "</html>";
                label = new JLabel(html, ImageUtils.createImageIcon("images/icons/noty-success.png"),
                        SwingConstants.LEFT);
                label.setBackground(new Color(0xDFF0D8));
                label.setForeground(new Color(0x333333));
                border = new LineBorder(new Color(0x468847), 1);
                break;
            case ERROR:
                html = "<html> <b>" + noty.getSrc() + "</b> " + noty.getTitle() + "<br>" + msgTxt + "</html>";
                label = new JLabel(html, ImageUtils.createImageIcon("images/icons/noty-error.png"), SwingConstants.LEFT);
                label.setBackground(new Color(0xF2DEDE));
                label.setForeground(new Color(0x333333));
                border = new LineBorder(new Color(0xB94A48), 1);
                break;
            case WARNING:
                html = "<html> <b>" + noty.getSrc() + "</b> " + noty.getTitle() + "<br>" + msgTxt + "</html>";
                label = new JLabel(html, ImageUtils.createImageIcon("images/icons/noty-warning.png"),
                        SwingConstants.LEFT);
                label.setBackground(new Color(0xFCF8E3));
                label.setForeground(new Color(0x333333));
                border = new LineBorder(new Color(0xC09853), 1);
                break;
            case INFO:
                html = "<html> <b>" + noty.getSrc() + "</b> " + noty.getTitle() + "<br>" + msgTxt + "</html>";
                label = new JLabel(html, ImageUtils.createImageIcon("images/icons/noty-info.png"), SwingConstants.LEFT);
                label.setBackground(new Color(0xD9EDF7));
                label.setForeground(new Color(0x333333));
                border = new LineBorder(new Color(0x3A87AD), 1);
                break;
            default:
                html = "<html> <b>" + noty.getSrc() + "</b> " + noty.getTitle() + "<br>" + msgTxt + "</html>";
                label = new JLabel(html, ImageUtils.createImageIcon("images/icons/info.png"), SwingConstants.LEFT);
                label.setBackground(new Color(0xD9EDF7));
                label.setForeground(new Color(0x333333));
                border = new LineBorder(new Color(0x3A87AD), 1);
                break;
        }
        label.setBorder(BorderFactory.createCompoundBorder(border, paddingBorder));
        label.setOpaque(true);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setIconTextGap(10);

        View view = (View) label.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
        view.setSize(500, 0);
        float w = view.getPreferredSpan(View.X_AXIS);
        float h = view.getPreferredSpan(View.Y_AXIS);
        label.setSize(new Dimension((int) Math.ceil(w), (int) Math.ceil(h) + 30));

        label.setLocation(this.getWidth() - (label.getWidth() + MARGIN_RIGHT), (this.getHeight()
                - (label.getHeight() + MARGIN_BOTTOM) - currentHeight));

        if (!atomic) {
            currentHeight += label.getHeight() + MARGIN_BOTTOM;

            if (noty.needsHumanAction()) {
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        list.remove(noty);
                        remove(label);
                        currentHeight -= label.getHeight() + MARGIN_BOTTOM;
                        refresh();
                        repaint();
                    }
                });
            }
            else {
                final Timer timer = new Timer("notification timer");
                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        list.remove(noty);
                        remove(label);
                        currentHeight -= label.getHeight() + MARGIN_BOTTOM;
                        refresh();
                        repaint();
                        timer.cancel();

                    }
                };
                timer.schedule(tt, 4 * 1000);
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        timer.cancel();
                        list.remove(noty);
                        remove(label);
                        currentHeight -= label.getHeight() + MARGIN_BOTTOM;
                        refresh();
                        repaint();
                    }
                });
            }
        }
        return label;
    }

    private void setupListeners() {
        this.frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                 refresh();
            }

        });
    }
}