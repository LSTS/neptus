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
 * Author: Paulo Dias
 * 2008/04/13
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.LocationCopyPastePanel;
import pt.lsts.neptus.gui.MessagePreviewer;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 * @author keila - Changes on 2/12/2019
 *
 */
public class ImcMessageSenderPanel extends JPanel {

    private static final long serialVersionUID = 3776289592692060016L;

    private static ImageIcon ICON = new ImageIcon(
            ImageUtils.getImage("images/imc.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
    private static ImageIcon ICON1 = new ImageIcon(
            ImageUtils.getImage("images/imc.png").getScaledInstance(48, 48, Image.SCALE_SMOOTH));

    private JComboBox<String> messagesComboBox = null;
    private JButton editMessageButton = null;
    private JButton publishButton = null;
    private JButton burstPublishButton = null;
    private JButton createButton = null;
    private JButton previewButton = null;

    private LocationCopyPastePanel locCopyPastePanel = null;

    private JTextField address = new JTextField("127.0.0.1");
    private NumberFormat nf = new DecimalFormat("#####");
    private JTextField port = null;
    private JTextField bindPort = new JTextField("");

    private MessagePreviewer msgPreview;

    private JTabbedPane tabs = null;

    private IMCFieldsPanel fields = null;

    private HashMap<String, IMCMessage> messagesPool = new HashMap<String, IMCMessage>();

    /**
     * 
     */
    public ImcMessageSenderPanel() {
//        this.setPreferredSize(new Dimension(500,600));
        initialize();
    }

    private void initialize() {

        port = new JTextField(nf.format(GeneralPreferences.commsLocalPortUDP));

        // Main Tab Panel
        JPanel holder_config = new JPanel();
        GroupLayout layout_config = new GroupLayout(holder_config);
        holder_config.setLayout(layout_config);
        layout_config.setAutoCreateGaps(true);
        layout_config.setAutoCreateContainerGaps(true);

        // Footer panel
        JPanel holder_footer = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Main Tab components
        JLabel addressLabel = new JLabel("Address and Port to UDP send");
        JLabel localBindLabel = new JLabel("Local Port to bind (can be blanc)");
        JLabel msgNameLabel = new JLabel("Choose IMC Message");

        layout_config.setHorizontalGroup(layout_config.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(layout_config.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(localBindLabel)
                        .addComponent(bindPort))
                .addGroup(layout_config.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(addressLabel)
                        .addGroup(layout_config.createSequentialGroup().addComponent(address).addComponent(port)))
                .addGroup(layout_config.createParallelGroup(Alignment.CENTER).addComponent(msgNameLabel)
                        .addComponent(getMessagesComboBox())));

        layout_config.setVerticalGroup(layout_config.createSequentialGroup()
                .addGroup(layout_config.createSequentialGroup().addComponent(localBindLabel).addComponent(bindPort, 25,
                        25, 25))
                .addGroup(layout_config.createSequentialGroup().addComponent(addressLabel)
                        .addGroup(layout_config.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(address, 25, 25, 25).addComponent(port, 25, 25, 25)))
                .addGroup(layout_config.createSequentialGroup().addComponent(msgNameLabel)
                        .addComponent(getMessagesComboBox(), 25, 25, 25)));

        // Buttons container in the bottom
        holder_footer.add(getLocCopyPastPanel());
        holder_footer.add(getEditMessageButton());
        holder_footer.add(getCreateButton());
        holder_footer.add(getBurstPublishButton());
        holder_footer.add(getPublishButton());
        holder_footer.add(getPreviewButton());
        JScrollPane scrollFooter = new JScrollPane(holder_footer) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(450, 40);
            }  
        };

        tabs = getTabedPane();
        String mgsName = (String) getMessagesComboBox().getSelectedItem();
        fields = new IMCFieldsPanel(null, mgsName, null);
        tabs.add("General Settings", holder_config);
        tabs.add("Message Fields", fields.getContents());
        this.setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(scrollFooter, BorderLayout.SOUTH);

    }

    /**
     * @return
     */
    private JTabbedPane getTabedPane() {
        if (tabs == null) {
            tabs = new JTabbedPane();
            ChangeListener changePane = new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    if (tabs.getSelectedIndex() == 1) { // fields

                        if (messagesComboBox == null) {
                            messagesComboBox = getMessagesComboBox();
                        }
                        // all components have been created
                        String mName = (String) getMessagesComboBox().getSelectedItem();
                        IMCMessage m = ImcMessageSenderPanel.this.messagesPool.get(mName);
                        if (ImcMessageSenderPanel.this.fields == null) {
                            fields = new IMCFieldsPanel(null, mName, m);
                            tabs.setComponentAt(1, fields.getContents());
                            tabs.repaint();
                        }
                        else if (!mName.equals(ImcMessageSenderPanel.this.fields.getMessageName())) {
                            fields = new IMCFieldsPanel(null, mName, m);
                            tabs.setComponentAt(1, fields.getContents());
                            tabs.repaint();
                        }
                    }

                }

            };
            tabs.addChangeListener(changePane);

        }
        return tabs;
    }

    private JComboBox<String> getMessagesComboBox() {
        if (messagesComboBox == null) {
            List<String> mList = new ArrayList<String>(IMCDefinition.getInstance().getMessageCount());
            for (String mt : IMCDefinition.getInstance().getMessageNames()) {
                mList.add(mt);
            }
            Collections.sort(mList);
            messagesComboBox = new JComboBox<String>(mList.toArray(new String[mList.size()]));
            messagesComboBox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedItem = (String) messagesComboBox.getSelectedItem();
                    IMCMessage m = ImcMessageSenderPanel.this.messagesPool.get(selectedItem);
                    if (fields == null)
                        fields = new IMCFieldsPanel(null, selectedItem, m);
                    if (!(fields.getMessageName().equals(selectedItem))) {
                        IMCMessage toCache = fields.getImcMessage();
                        ImcMessageSenderPanel.this.messagesPool.put(fields.getMessageName(), toCache);
                        fields = new IMCFieldsPanel(null, selectedItem, m);
                        tabs.setComponentAt(1, fields.getContents());
                        tabs.repaint();

                    }
                }
            });
        }
        return messagesComboBox;
    }

    /**
     * This method initializes editMessageButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getEditMessageButton() {
        if (editMessageButton == null) {
            editMessageButton = new JButton();
            editMessageButton.setText("Edit");
            editMessageButton.setPreferredSize(new Dimension(90, 26));
            editMessageButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tabs.setSelectedIndex(1);
                }
            });
        }
        return editMessageButton;
    }

    /**
     * @return the createButton
     */
    private JButton getCreateButton() {
        if (createButton == null) {
            createButton = new JButton();
            createButton.setText("Generate");
            createButton.setPreferredSize(new Dimension(102, 26));
            createButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String mName = (String) getMessagesComboBox().getSelectedItem();
                    getOrCreateMessage(mName);
                }
            });
        }
        return createButton;
    }

    /**
     * This method initializes publishButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getPublishButton() {
        if (publishButton == null) {
            publishButton = new JButton();
            publishButton.setText("Publish");
            publishButton.setPreferredSize(new Dimension(90, 26));
            publishButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String msgName = (String) messagesComboBox.getSelectedItem();
                    String msg = null;
                    IMCMessage sMsg = messagesPool.get(msgName);
                    if (sMsg != null) {
                        try {
                            sMsg.setTimestampMillis(System.currentTimeMillis());
                            sMsg.dump(System.out);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            IMCOutputStream ios = new IMCOutputStream(baos);
                            sMsg.serialize(ios);

                            ByteUtil.dumpAsHex(msgName + " [size=" + baos.size() + "]", baos.toByteArray(), System.out);
                            msg = sendUdpMsg(baos.toByteArray(), baos.size());
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        if (msg != null)
                            JOptionPane.showMessageDialog(publishButton,
                                    "Error sending " + msgName + " [" + msg + "]!");
                    }
                    else {
                        JOptionPane.showMessageDialog(publishButton,
                                "Edit first message " + msgName + " to create it!");
                    }
                }
            });
        }
        return publishButton;
    }

    /**
     * This method associates a @pt.lsts.imc.sender.MessageEditor to the currently selected message on the panel
     * 
     * @return
     */
    private JButton getPreviewButton() {
        if (previewButton == null) {
            previewButton = new JButton();
            previewButton.setText("Preview");
            previewButton.setPreferredSize(new Dimension(90, 26));
        }
        ActionListener previewAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                String mName = (String) messagesComboBox.getSelectedItem();
                IMCMessage m = ImcMessageSenderPanel.this.messagesPool.get(mName);
                if (fields == null)
                    fields = new IMCFieldsPanel(null, mName, m);
                else if (!mName.equals(fields.getMessageName())) {
                    IMCMessage toCache = fields.getImcMessage();
                    ImcMessageSenderPanel.this.messagesPool.put(fields.getMessageName(), toCache);
                    fields = new IMCFieldsPanel(null, mName, m);
                }

                IMCMessage sMsg = fields.getImcMessage();
                messagesPool.put(mName, sMsg);
                msgPreview = new MessagePreviewer(sMsg, true, true, true);
                JDialog dg = new JDialog(SwingUtilities.getWindowAncestor(ImcMessageSenderPanel.this),
                        ModalityType.DOCUMENT_MODAL);
                dg.setContentPane(msgPreview);
                dg.setSize(500, 500);
                dg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                GuiUtils.centerParent(dg, (Window) dg.getParent());
                dg.setVisible(true);
            }
        };
        previewButton.addActionListener(previewAction);
        return previewButton;
    }

    /**
     * @return the burstPublishButton
     */
    private JButton getBurstPublishButton() {
        if (burstPublishButton == null) {
            burstPublishButton = new JButton();
            burstPublishButton.setText("Burst");
            burstPublishButton.setPreferredSize(new Dimension(85, 26));
            burstPublishButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    burstPublishButton.setEnabled(false);
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            Collection<String> mtypes = IMCDefinition.getInstance().getMessageNames();
                            for (String mt : mtypes) {
                                String msgName = mt;
                                IMCMessage sMsg = getOrCreateMessage(msgName);
                                sMsg.setTimestampMillis(System.currentTimeMillis());
                                sMsg.dump(System.out);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                IMCOutputStream ios = new IMCOutputStream(baos);
                                try {
                                    sMsg.serialize(ios);
                                    ByteUtil.dumpAsHex(msgName + " [size=" + baos.size() + "]", baos.toByteArray(),
                                            System.out);
                                    String msg = sendUdpMsg(baos.toByteArray(), baos.size());
                                }
                                catch (Exception e1) {
                                    NeptusLog.pub().error(e1);
                                    e1.printStackTrace();
                                }
                            }
                            return false;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            if (burstPublishButton != null)
                                burstPublishButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return burstPublishButton;
    }

    /**
     * @return the locCopyPastPanel
     */
    private LocationCopyPastePanel getLocCopyPastPanel() {
        if (locCopyPastePanel == null) {
            locCopyPastePanel = new LocationCopyPastePanel() {
                private static final long serialVersionUID = 1809942752421373734L;

                @Override
                public void setLocationType(LocationType locationType) {
                    super.setLocationType(locationType);
                    ImcMessageSenderPanel.this.fields = ImcMessageSenderPanel.this.fields
                            .applyLocation(getLocCopyPastPanel().getLocationType(), ImcMessageSenderPanel.this.fields.getImcMessage());
                    tabs.removeTabAt(1);
                    ImcMessageSenderPanel.this.tabs.add("Message Fields",
                            ImcMessageSenderPanel.this.fields.getContents());
                    ImcMessageSenderPanel.this.tabs.repaint();
                }
            };
            // TODO add paste button for imc messages
            locCopyPastePanel.setPreferredSize(new Dimension(85, 26));
            locCopyPastePanel.setMaximumSize(new Dimension(85, 26));
            // locCopyPastPanel.setBorder(null);
            locCopyPastePanel
                    .setToolTipText("Pastes to EstimatedState Message (but doesn't copy from there nor touches ref)");
        }
        return locCopyPastePanel;
    }

    public String sendUdpMsg(byte[] msg, int size) {

        try {
            DatagramSocket sock = null;
            if ("".equalsIgnoreCase(bindPort.getText())) {
                sock = new DatagramSocket();
            }
            else {
                int bport = Integer.parseInt(bindPort.getText());
                sock = new DatagramSocket(bport);
            }
            sock.connect(new InetSocketAddress(address.getText(), Integer.parseInt(port.getText())));
            sock.send(new DatagramPacket(msg, size));
            sock.close();
            return null;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return "Error sending the UDP message: " + e.getMessage();
        }
    }

    private IMCMessage getOrCreateMessage(String mName) {
        IMCMessage msg = messagesPool.get(mName);
        if (msg == null) {
            msg = IMCDefinition.getInstance().create(mName);
            messagesPool.put(mName, msg);
        }
        // applyLocation(msg);
        return msg;
    }

    public static JFrame getFrame() {
        JFrame frame = GuiUtils.testFrame(new ImcMessageSenderPanel(), "Teste 1,2", 500, 500);
        frame.setSize(600, 500);
        frame.setTitle("IMC Message Sender (by UDP)");
        ArrayList<Image> imageList = new ArrayList<Image>();
        imageList.add(ICON.getImage());
        imageList.add(ICON1.getImage());
        frame.setIconImages(imageList);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        return frame;
    }

    /**
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        ConfigFetch.initialize();
        new ImcMessageSenderPanel().getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
