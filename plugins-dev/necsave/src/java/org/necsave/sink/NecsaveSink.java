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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 09/08/2016
 */
package org.necsave.sink;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.PatternPredicate;
import org.necsave.NMPUtilities;

import com.google.common.eventbus.Subscribe;

import info.necsave.proto.Message;
import info.necsave.proto.ProtoDefinition;
import info.necsave.proto.ProtoInputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author zp
 *
 */
@PluginDescription(name = "NECSAVE Perception Sink", icon="org/necsave/necsave.png")
@Popup(name = "NECSAVE Messages", pos = POSITION.CENTER, height = 500, width = 800, accelerator = 'N')
public class NecsaveSink extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = -8156866308151145063L;
    
    @NeptusProperty(name="Port to listen for incoming messages")
    public int listeningPort = 32123;

    @NeptusProperty(name="Show incoming messages")
    public boolean listenIncomingMessages = true;
    
    @NeptusProperty(name="Post messages to Neptus")
    public boolean postToNeptus = true;
    
    private DatagramSocket socket = null;
    private boolean stopped = true;
    private NMPTableModel model = new NMPTableModel();
    private JXTable table = new JXTable(model);

    private JPanel bottomPanel = new JPanel();
    private JToggleButton autoScroll = new JToggleButton(I18n.text("Auto scroll"));
    private JButton clear = new JButton(I18n.text("Clear")), 
            settings = new JButton(I18n.text("Settings"));
            
    private JTextField highlight = new JTextField(40);
    
    
    /**
     * @param console
     */
    public NecsaveSink(ConsoleLayout console) {
        super(console);
        table.setSortable(false);
        
        // Show message HTML when a row is double-clicked
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    Message m = model.getMessage(table.getSelectedRow());
                    
                    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(NecsaveSink.this), "NMP - "+m.getAbbrev()+" - "+model.getId(table.getSelectedRow()));
                    JEditorPane editor = new JEditorPane("text/html", NMPUtilities.getAsHtml(m));
                    editor.setEditable(false);
                    dialog.setContentPane(new JScrollPane(editor));
                    dialog.setSize(500, 500);
                    dialog.setVisible(true);
                }
            }
        });
        
        // add table to layout
        setLayout(new BorderLayout());
        JScrollPane pane = new JScrollPane(table);
        add(pane, BorderLayout.CENTER);        

        // scroll to last message if autoscroll is selected
        table.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (autoScroll.isSelected())
                    table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
            }
        });
        
        // add buttons to layout
        bottomPanel.add(settings);
        bottomPanel.add(clear);
        bottomPanel.add(autoScroll);
        bottomPanel.add(highlight);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // set default highlighter text to be displayed
        highlight.setText("PlatformInfo");
        highlight.setBackground(Color.yellow);
        highlight.setForeground(Color.red.darker());
        updateHighlighters();
        
        settings.addActionListener(this::showSettings);
        clear.addActionListener(this::clearMessages);
        autoScroll.setSelected(true);
        highlight.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateHighlighters();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateHighlighters();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateHighlighters();
            }
        });        
        
        // tooltips
        highlight.setToolTipText(I18n.text("Text to highlight on the table"));
        autoScroll.setToolTipText(I18n.text("Advance to last received message automatically"));
        clear.setToolTipText(I18n.text("Clear table"));
        settings.setToolTipText(I18n.text("Change plug-in settings"));
    }

    public void clearMessages(ActionEvent evt) {
        model.clear();
    }
    
    public void showSettings(ActionEvent evt) {
        PluginUtils.editPluginProperties(this, true);
    }

    public void updateHighlighters() {
        PatternPredicate predicate = new PatternPredicate(".*"+highlight.getText()+".*");
        ColorHighlighter pattern = new ColorHighlighter(predicate, null, Color.BLUE, null,Color.BLACK);
        pattern.setBackground(Color.YELLOW);
        pattern.setForeground(Color.RED.darker());
        table.setHighlighters(pattern);
    }


    @Override
    public void cleanSubPanel() {
        stopped = true;
        receive.interrupt();
    }

    @Override
    public synchronized void propertiesChanged() {
        if (socket != null)
            socket.close();
        try {
            socket = new DatagramSocket(listeningPort);
            socket.setSoTimeout(3000);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            socket = null;
        }
    }

    private Thread receive = new Thread("Sink Receiver") {
        private byte[] receiveData = new byte[64 * 1024];

        public void run() {
            stopped = false;
            while (!stopped) {
                try {
                    if (socket == null) {
                        Thread.sleep(1000);
                        continue;
                    }
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    if (listenIncomingMessages) {
                        ProtoInputStream pis = new ProtoInputStream(new ByteArrayInputStream(receiveData),
                                ProtoDefinition.getInstance());
                        Message msg = ProtoDefinition.getInstance().nextMessage(pis);
                        process(receivePacket.getAddress().getHostName(), receivePacket.getPort(), msg);
                    }

                    
                }
                catch (SocketTimeoutException e) {
                    // no messages received for 3 secs
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        };
    };
    
    @Subscribe
    public void on(Message msg) {
        model.addMessage(msg);
    }    

    void process(String source, int port, Message msg) {        
        post(msg);        
    }

    @Override
    public void initSubPanel() {
        propertiesChanged();
        receive.start();
    }
}
