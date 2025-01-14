/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Miguel Carvalho
 * 12/12/2024
 */
package pt.lsts.neptus.gui.swing;

import pt.lsts.neptus.util.ImageUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HoldFillButton extends JButton {
    private Timer timer;
    private int progress = 0;
    private int holdDurationMs = 2000; // 2 seconds
    private final List<ActionListener> actionListeners = new ArrayList<>();
    private final ImageIcon LOCK_CLOCK = new ImageIcon(ImageUtils.getScaledImage("images/buttons/lock_clock.png", 15, 15));

    public HoldFillButton(String text) {
        super(text);
        setIcon(LOCK_CLOCK);
        setupButton();
    }

    public HoldFillButton(String text, int holdDurationMs) {
        super(text);
        setIcon(LOCK_CLOCK);
        this.holdDurationMs = holdDurationMs;
        setupButton();
    }

    private void setupButton(){
        addMouseListener(new MouseAdapter() {
            private long pressStartTime;

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    pressStartTime = System.currentTimeMillis();
                    progress = 0;

                    timer = new Timer(10, event -> {
                        long elapsed = System.currentTimeMillis() - pressStartTime;
                        progress = (int) (elapsed * 100 / holdDurationMs);
                        repaint();

                        if (elapsed >= holdDurationMs) {
                            timer.stop();
                            triggerAction();
                            progress = 0;
                        }
                    });
                    timer.start();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (timer != null) {
                    timer.stop();
                }
                progress = 0;
                repaint();
            }

            private void triggerAction() {
                fireCustomActionPerformed();
            }
        });
    }

    private void fireCustomActionPerformed() {
        ActionEvent customEvent = new ActionEvent(
                this, ActionEvent.ACTION_PERFORMED, getActionCommand()
        );
        Iterator<ActionListener> iterator = actionListeners.iterator();
        while (iterator.hasNext()) {
            ActionListener listener = iterator.next();
            listener.actionPerformed(customEvent);
        }
    }

    @Override
    public void addActionListener(ActionListener l) {
        if (l == null) {
            throw new IllegalArgumentException("ActionListener cannot be null");
        }
        actionListeners.add(l);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (progress > 0) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int fillWidth = (int) (getWidth() * (progress / 100.0));

            Color fillColor = new Color(0, 128, 255, 100);

            g2d.setColor(fillColor);
            g2d.fillRect(0, 0, fillWidth, getHeight());
        }
    }
}
