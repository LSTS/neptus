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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: mauro
 * Dec 16, 2015
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Mauro Brandão
 * @since 17/12/2015
 */
public class HistoriesPanelView extends JPanel {
    private static final long serialVersionUID = 1L;
    protected JPanel TabPanel = new JPanel();
    protected Vector<HistoryMessage> myMessages = new Vector<HistoryMessage>();
    
    protected ConsoleLayout console = null;

    protected boolean showInfo, showWarn, showError, showDebug;
    
    private String sysName = "";
    
    private String imgsPath = "pt/lsts/neptus/plugins/logs/";
    
    protected LinkedHashMap<msg_type, Color> bgColors = new LinkedHashMap<HistoryMessage.msg_type, Color>();
    {
        bgColors.put(msg_type.critical, Color.black);
        bgColors.put(msg_type.error, new Color(255, 128, 128));
        bgColors.put(msg_type.warning, new Color(255, 255, 128));
        bgColors.put(msg_type.info, new Color(200, 255, 200));
        bgColors.put(msg_type.debug, new Color(217, 217, 217));
    }

    public HistoriesPanelView(ConsoleLayout console, String sysName) {
        this.console = console;
        this.sysName = sysName;
        deployLayout();

    }
    
    public HistoriesPanelView(String src, boolean showInfo, boolean showWarn, boolean showError, boolean showDebug) {
        this.sysName = src;
        this.showInfo = showInfo;
        this.showWarn = showWarn;
        this.showError = showError;
        this.showDebug = showDebug;
        deployLayout();
    }

    private void deployLayout() {
        setLayout(new BorderLayout());
        TabPanel.setLayout(new MigLayout("hidemode 3"));
        TabPanel.setBackground(Color.white);
        add(TabPanel, BorderLayout.CENTER);
    }

    public void reloadMessages() {
        IMCMessage m = IMCDefinition.getInstance().create("LogBookControl", "command", "GET");
        ImcMsgManager.getManager().sendMessageToVehicle(m, HistoriesPanelView.this.sysName, null);

        m = IMCDefinition.getInstance().create("LogBookControl", "command", "GET_ERR");
        ImcMsgManager.getManager().sendMessageToVehicle(m, HistoriesPanelView.this.sysName, null);
    }

    public void refreshHistoryMessages() {
        Vector<HistoryMessage> tmp = new Vector<HistoryMessage>();
        Collections.sort(myMessages);
        tmp.addAll(myMessages);
        myMessages.clear();
        TabPanel.removeAll();
        Collections.sort(tmp);
        setMessages(tmp);
    }

    public void clear() {
        myMessages.clear();
        TabPanel.removeAll();
        TabPanel.repaint();
    }

    public void setMessages(Vector<HistoryMessage> messages) {
        for (HistoryMessage m : messages) {
                if (m.type == msg_type.info && !showInfo)
                    continue;
                if (m.type == msg_type.warning && !showWarn)
                    continue;
                if (m.type == msg_type.error && !showError)
                    continue;
                if (m.type == msg_type.debug && !showDebug)
                    continue;
                
                JLabel l = new JLabel("", JLabel.LEFT);
                
                l.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent arg0) {
                        if (arg0.getButton() == MouseEvent.BUTTON3) {
                            JPopupMenu popup = new JPopupMenu();
                            popup.add(I18n.text("Clear")).addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    clear();
                                }
                            });

                            popup.add(I18n.text("Refresh")).addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    refreshHistoryMessages();
                                }
                            });

                            final int index = myMessages.lastIndexOf(arg0.getPoint());
                            if (index != -1) {
                                popup.add(I18n.text("Copy entry to clipboard")).addActionListener(new ActionListener() {

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        String text = myMessages.elementAt(index).toString();

                                        ClipboardOwner owner = new ClipboardOwner() {
                                            public void lostOwnership(Clipboard clipboard, Transferable contents) {};                       
                                        };
                                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), owner);
                                    }
                                });
                            }

                            if (myMessages.size() > 0) {
                                popup.add(I18n.text("Copy history to clipboard")).addActionListener(new ActionListener() {

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        String text = "";
                                        
                                        for (int i = 0; i < myMessages.size(); i++) {
                                            text += myMessages.elementAt(i)+"\n";
                                        }

                                        ClipboardOwner owner = new ClipboardOwner() {
                                            public void lostOwnership(Clipboard clipboard, Transferable contents) {};                       
                                        };
                                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), owner);
                                    }
                                });
                            }

                            popup.show((Component)arg0.getSource(), arg0.getX(), arg0.getY());
                        }
                    }
                });
                l.setText(m.toString());
                l.setIcon(getIcon(m.type));
                l.setToolTipText(I18n.textf("Received on %timeStamp (%context)", new Date(m.timestamp), m.context));
                l.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 3));
                l.setOpaque(true);
                l.setBackground(bgColors.get(m.type));
                l.setVisible(true);
                
                TabPanel.add(l,"wrap, dock center, pad -4 -2 4 2");
        }

        invalidate();
        validate();

        repaint();
    }
    
    public void add(HistoryMessage msg) {
        if (!myMessages.contains(msg)) {
            myMessages.add(msg);
            Collections.sort(myMessages);
            setMessages(myMessages);
        }
    }
    
    public Collection<HistoryMessage> add(Collection<HistoryMessage> msgs) {
        
        Vector<HistoryMessage> notExisting = new Vector<>();
        
        for (HistoryMessage msg : msgs) {
            if (!myMessages.contains(msg)) {
                myMessages.add(msg);
                notExisting.add(msg);
            }
        }
        
        if (notExisting.isEmpty())
            return notExisting;
        
        Collections.sort(myMessages);
        setMessages(myMessages);
        Collections.sort(notExisting);
        return notExisting;
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
