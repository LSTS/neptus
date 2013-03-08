/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 19, 2013
 * $Id:: MultiSystemHistory.java 10009 2013-02-21 13:59:59Z zepinto             $:
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.awt.BorderLayout;
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

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.LogBookControl;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author zp
 *
 */
public class MultiSystemHistory extends JPanel {

    private static final long serialVersionUID = 1L;
    protected JTabbedPane tabs = new JTabbedPane();
    protected LinkedHashMap<String, LogBookHistory> histories = new LinkedHashMap<>();
    protected LinkedHashMap<String, JList<HistoryMessage>> lists = new LinkedHashMap<>();

    /**
     * 
     */
    public MultiSystemHistory() {
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }

    public Collection<HistoryMessage> add(Collection<HistoryMessage> msgs, String src) {
        if (!histories.containsKey(src)) {
            createHistory(src);
        }

        Collection<HistoryMessage> ret = histories.get(src).add(msgs);

        int size = lists.get(src).getModel().getSize()-1;
        if (size > 0)
            lists.get(src).ensureIndexIsVisible(size);
        
        return ret;
    }

    public void removeHistory(String src) {
        for (int i = 0; i < tabs.getTabCount(); i++)
            if (tabs.getTitleAt(i).equals(src)) {
                tabs.removeTabAt(i);
                return;
            }

        histories.remove(src);
        lists.remove(src);
    }

    public JList<HistoryMessage> createHistory(String src) {
        final LogBookHistory hist = new LogBookHistory(src);
        histories.put(src, hist);
        final JList<HistoryMessage> list = new JList<>(hist);
        list.setCellRenderer(hist);
        tabs.addTab(src, new JScrollPane(list));
        lists.put(src, list);

        list.addMouseListener(new MouseAdapter() {
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
                            LogBookControl ctrl = new LogBookControl();
                            ctrl.setCommand(LogBookControl.COMMAND.GET);
                            ImcMsgManager.getManager().sendMessageToSystem(ctrl, hist.sysname);
                        }
                    });

                    final int index = list.locationToIndex(arg0.getPoint());
                    if (index != -1) {
                        popup.add(I18n.text("Copy entry to clipboard")).addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String text = list.getModel().getElementAt(index).toString();

                                ClipboardOwner owner = new ClipboardOwner() {
                                    public void lostOwnership(Clipboard clipboard, Transferable contents) {};                       
                                };
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), owner);
                            }
                        });
                    }

                    if (list.getModel().getSize() > 0) {
                        popup.add(I18n.text("Copy history to clipboard")).addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String text = "";
                                
                                for (int i = 0; i < list.getModel().getSize(); i++) {
                                    text += list.getModel().getElementAt(i)+"\n";
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

        return list;
    }

    public void add(HistoryMessage m, String src) {
        if (!histories.containsKey(src)) {
            createHistory(src);
        }

        histories.get(src).add(m);
        int size = lists.get(src).getModel().getSize()-1;
        if (size > 0)
            lists.get(src).ensureIndexIsVisible(size);
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
