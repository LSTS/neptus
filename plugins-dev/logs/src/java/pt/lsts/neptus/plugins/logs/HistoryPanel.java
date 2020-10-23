/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 5 de Jul de 2010
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author ZP
 *
 */
@SuppressWarnings("serial")
public class HistoryPanel extends JPanel {

    protected JPanel mainPanel = new JPanel();
    protected Vector<HistoryMessage> myMessages = new Vector<HistoryMessage>();
    protected JScrollPane scroll = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    protected ConsoleLayout console = null;

    protected boolean showInfo = true;
    protected boolean showWarn = true;
    protected boolean showError = true;
    protected boolean showDebug = false;

    protected boolean showReload = true;

    protected boolean autoActive = true;
    private String imgsPath = "pt/lsts/neptus/plugins/logs/";

    protected ToolbarSwitch sw = new ToolbarSwitch(ImageUtils.getIcon(imgsPath + "logging.png"),
            I18n.text("Control auto-scroll"), null);

    protected LinkedHashMap<msg_type, Color> bgColors = new LinkedHashMap<HistoryMessage.msg_type, Color>();
    {
        bgColors.put(msg_type.critical, Color.black);
        bgColors.put(msg_type.error, new Color(255, 128, 128));
        bgColors.put(msg_type.warning, new Color(255, 255, 128));
        bgColors.put(msg_type.info, new Color(200, 255, 200));
        bgColors.put(msg_type.debug, new Color(217, 217, 217));
    }

    public HistoryPanel(ConsoleLayout console, boolean showReload) {
        this.console = console;
        this.showReload = showReload;

        setLayout(new BorderLayout());
        mainPanel.setLayout(new GridLayout(0, 1));
        mainPanel.setBackground(Color.white);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setAutoscrolls(true);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JCheckBox check_info = new JCheckBox(new AbstractAction(I18n.text("Information")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (HistoryPanel.this.console == null)
                    return;

                showInfo = ((JCheckBox) e.getSource()).isSelected();
                refreshHistoryMessages();

            }
        });
        check_info.setSelected(showInfo);
        JCheckBox check_warn = new JCheckBox(new AbstractAction(I18n.text("Warning")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (HistoryPanel.this.console == null)
                    return;

                showWarn = ((JCheckBox) e.getSource()).isSelected();
                refreshHistoryMessages();
            }
        });
        check_warn.setSelected(showWarn);
        JCheckBox check_error = new JCheckBox(new AbstractAction(I18n.text("Error")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (HistoryPanel.this.console == null)
                    return;

                showError = ((JCheckBox) e.getSource()).isSelected();
                refreshHistoryMessages();
            }
        });
        check_error.setSelected(showError);

        JCheckBox check_debug = new JCheckBox(new AbstractAction(I18n.text("Debug")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (HistoryPanel.this.console == null)
                    return;

                showDebug = ((JCheckBox) e.getSource()).isSelected();
                refreshHistoryMessages();
            }
        });
        check_debug.setSelected(showDebug);

        bottom.add(check_info);
        bottom.add(check_warn);
        bottom.add(check_error);
        bottom.add(check_debug);

        if (showReload) {
            ToolbarButton btn = new ToolbarButton(new AbstractAction(I18n.text("Reload"), ImageUtils.getIcon(imgsPath
                    + "reload.png")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (HistoryPanel.this.console == null)
                        return;
                    reloadMessages();
                }
            });
            btn.setText(I18n.text("Reload"));

            bottom.add(btn);
        }

        ToolbarButton clear = new ToolbarButton(
                new AbstractAction(I18n.text("Clear"), ImageUtils.getIcon(imgsPath + "eraser.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
        clear.setText(I18n.text("Clear"));
        bottom.add(clear);

        sw.setText(I18n.text("Auto-scroll"));
        sw.setState(autoActive);
        bottom.add(sw);

        add(bottom, BorderLayout.SOUTH);
    }

    public HistoryPanel() {
        this(null, true);
    }
    
    public void reloadMessages() {
        IMCMessage m = IMCDefinition.getInstance().create("LogBookControl", "command", "GET");
        ImcMsgManager.getManager().sendMessageToVehicle(m, HistoryPanel.this.console.getMainSystem(), null);

        m = IMCDefinition.getInstance().create("LogBookControl", "command", "GET_ERR");
        ImcMsgManager.getManager().sendMessageToVehicle(m, HistoryPanel.this.console.getMainSystem(), null);
    }

    public void refreshHistoryMessages() {
        Vector<HistoryMessage> tmp = new Vector<HistoryMessage>();
        tmp.addAll(myMessages);
        myMessages.clear();
        mainPanel.removeAll();
        setMessages(tmp);
    }

    public void clear() {
        myMessages.clear();
        mainPanel.removeAll();
        mainPanel.repaint();
        scroll.revalidate();
    }

    public void setMessages(Vector<HistoryMessage> messages) {
        for (HistoryMessage m : messages) {
            if (!myMessages.contains(m)) {
                myMessages.add(m);

                if (m.type == msg_type.info && !showInfo)
                    continue;
                if (m.type == msg_type.warning && !showWarn)
                    continue;
                if (m.type == msg_type.error && !showError)
                    continue;
                if (m.type == msg_type.debug && !showDebug)
                    continue;

                JLabel l = new JLabel(m.toString(), getIcon(m.type), JLabel.LEFT);
                l.setToolTipText(I18n.textf("Received on %timeStamp (%context)", new Date(m.timestamp), m.context));
                l.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 3));
                l.setOpaque(true);
                l.setBackground(bgColors.get(m.type));
                if (m.type == msg_type.critical)
                    l.setForeground(Color.yellow);
                mainPanel.add(l);
            }
        }

        invalidate();
        validate();
        
        if (sw.getState() == true) {
            autoActive=true;
            scroll.setAutoscrolls(true);
            scroll.setVerticalScrollBar(scroll.getVerticalScrollBar());
            mainPanel.scrollRectToVisible(new Rectangle(0, mainPanel.getHeight() + 22, 1, 1));
            scroll.getViewport().setViewPosition(new Point(0, scroll.getVerticalScrollBar().getMaximum()));
        }
        else {
            autoActive=false;
            scroll.setAutoscrolls(false);
        }

        repaint();
    }

    public ImageIcon getIcon(msg_type type) {
        switch (type) {
            case info:
                return ImageUtils.getIcon(imgsPath + "info.png");
            case warning:
                return ImageUtils.getIcon(imgsPath + "warning.png");
            case error:
                return ImageUtils.getIcon(imgsPath + "error.png");
            case critical:
                return ImageUtils.getIcon(imgsPath + "queue2.png");
            case debug:
                return ImageUtils.getIcon(imgsPath + "unknown.png");
            default:
                return ImageUtils.getIcon(imgsPath + "queue.png");
        }
    }
}
