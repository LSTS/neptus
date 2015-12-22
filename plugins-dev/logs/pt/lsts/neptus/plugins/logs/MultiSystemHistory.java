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
 * Author: José Pinto
 * Feb 19, 2013
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import pt.lsts.imc.LogBookControl;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class MultiSystemHistory extends JPanel {

    private static final long serialVersionUID = 1L;
    protected JTabbedPane tabs = new JTabbedPane();
    protected LinkedHashMap<String, HistoriesPanelView> histories = new LinkedHashMap<>();

    protected boolean showInfo = true;
    protected boolean showWarn = true;
    protected boolean showError = true;
    protected boolean showDebug = false;
    
    public MultiSystemHistory() {
            try {
                setLayout(new BorderLayout());
                add(tabs, BorderLayout.CENTER);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
       
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));

            JCheckBox check_info = new JCheckBox(new AbstractAction(I18n.text("Information")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                    showInfo = ((JCheckBox) e.getSource()).isSelected();
                    updateMessages();
                }
            });
            check_info.setSelected(showInfo);
            JCheckBox check_warn = new JCheckBox(new AbstractAction(I18n.text("Warning")) {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent e) {
                    showWarn = ((JCheckBox) e.getSource()).isSelected();
                    updateMessages();
                }
            });
            check_warn.setSelected(showWarn);
            JCheckBox check_error = new JCheckBox(new AbstractAction(I18n.text("Error")) {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent e) {
                    showError = ((JCheckBox) e.getSource()).isSelected();
                    updateMessages();
                }
            });
            check_error.setSelected(showError);

            JCheckBox check_debug = new JCheckBox(new AbstractAction(I18n.text("Debug")) {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent e) {
                    showDebug = ((JCheckBox) e.getSource()).isSelected();
                    updateMessages();
                }
            });
            check_debug.setSelected(showDebug);

            bottom.add(check_info);
            bottom.add(check_warn);
            bottom.add(check_error);
            bottom.add(check_debug);
            
            add(bottom, BorderLayout.SOUTH);
        
    }

    private void updateMessages() {
        LogBookControl ctrl = new LogBookControl();
        ctrl.setCommand(LogBookControl.COMMAND.GET);
        for (String sys : histories.keySet()) {
            ImcMsgManager.getManager().sendMessageToSystem(ctrl, sys);
            HistoriesPanelView lb = histories.get(sys);
            lb.clear();
            lb.showInfo = this.showInfo;
            lb.showWarn = this.showWarn;
            lb.showError = this.showError;
            lb.showDebug = this.showDebug;
            lb.refreshHistoryMessages();
            lb.repaint();
            tabs.repaint();
        }
    }

    public void removeHistory(String src) {
        for (int i = 0; i < tabs.getTabCount(); i++)
            if (tabs.getTitleAt(i).equals(src)) {
                tabs.removeTabAt(i);
                return;
            }

        histories.remove(src);
    }

    //Add messages to the panel from each system
    public HistoriesPanelView createHistory(String src) {
        final HistoriesPanelView hist = new HistoriesPanelView(src, showInfo, showWarn, showError, showDebug);
        histories.put(src, hist);
        tabs.addTab(src, new JScrollPane(hist));
        
        hist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                if (arg0.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(I18n.text("Clear")).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            hist.clear();
                        }
                    });

                    popup.add(I18n.text("Refresh")).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            updateMessages();
                        }
                    });

                    final int index = hist.myMessages.lastIndexOf(arg0.getPoint());
                    if (index != -1) {
                        popup.add(I18n.text("Copy entry to clipboard")).addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String text = hist.myMessages.elementAt(index).toString();

                                ClipboardOwner owner = new ClipboardOwner() {
                                    public void lostOwnership(Clipboard clipboard, Transferable contents) {};                       
                                };
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), owner);
                            }
                        });
                    }

                    if (hist.myMessages.size() > 0) {
                        popup.add(I18n.text("Copy history to clipboard")).addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String text = "";
                                
                                for (int i = 0; i < hist.myMessages.size(); i++) {
                                    text += hist.myMessages.elementAt(i)+"\n";
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

        invalidate();
        revalidate();

        return hist;
    }

    public void add(HistoryMessage m, String src) {
        if (!histories.containsKey(src)) {
            createHistory(src);
        }
        int size = 0;
        for (String sys : histories.keySet()) {
            histories.get(sys).myMessages.addElement(m);
            size= histories.get(sys).myMessages.size()-1;
            if (size > 0)
                histories.get(sys).myMessages.get(size);
        }
    }
    
    public Collection<HistoryMessage> add(Collection<HistoryMessage> msgs, String src) {
        if (!histories.containsKey(src)) {
            createHistory(src);
        }

        Collection<HistoryMessage> ret = histories.get(src).add(msgs);

        int size = histories.get(src).myMessages.size()-1;
        if (!ret.isEmpty() && isDisplayable())
            histories.get(src).myMessages.indexOf(size);
        
        return ret;
    }

    public static void main(String[] args) throws Exception {
        MultiSystemHistory hist = new MultiSystemHistory();
        
        GuiUtils.testFrame(hist);
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(300);

            hist.add(new HistoryMessage(System.currentTimeMillis(), "teste1", "ctx", true, HistoryMessage.msg_type.error), "lauv-seacon-1");
            Thread.sleep(300);

            hist.add(new HistoryMessage(System.currentTimeMillis(), "teste2", "ctx", true, HistoryMessage.msg_type.info), "lauv-seacon-1");
            Thread.sleep(300);
            hist.add(new HistoryMessage(System.currentTimeMillis()-5000, "teste3", "ctx", true, HistoryMessage.msg_type.critical), "lauv-seacon-2");
            Thread.sleep(300);
            hist.add(new HistoryMessage(System.currentTimeMillis()-5000, "teste3", "ctx", true, HistoryMessage.msg_type.warning), "lauv-seacon-1");
        }
    }    
}
