/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * Dec 3, 2019
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author keila
 * 3/12/19
 */
public class IMCFieldsPane {

    private final int mid;
    private final List<String> m_fields;
    private final String[] h = { "timestamp", "src", "src_ent", "dst", "dst_ent" };
    private final List<String> headers = Arrays.asList(h);

    private JTextField srcId = new JTextField("");
    private JTextField dstId = new JTextField("");
    private JTextField srcEntId = new JTextField("");
    private JTextField dstEntId = new JTextField("");
    
    private JPanel holder_Hfields;
    private JPanel content, holder_fields;
    private JScrollPane scrollable;

    /**
     * @return the holder_fields
     */
    public JPanel getContents() {
        return this.content;
    }

    public IMCFieldsPane(String name) {
        System.err.println("IMCFieldsPane "+name);
        mid = IMCDefinition.getInstance().getMessageId(name);
        m_fields = new ArrayList<>();
        IMCMessage msg = IMCDefinition.getInstance().create(name);
        
        for (String f : msg.getFieldNames()) {
            if (!headers.contains(f)) {
                m_fields.add(f);
//                System.err.println("Field added: "+f);
            }
        }
        initializePanel();

    }

    /**
     * 
     */
    @SuppressWarnings("serial")
    private void initializePanel() {

        // Fixed components from IMC Headers
        holder_Hfields = new JPanel() {
          @Override
          public Dimension getPreferredSize() {
              return new Dimension(450, 130);
          }
        };
        GroupLayout layout_fields = new GroupLayout(holder_Hfields);
        holder_Hfields.setLayout(layout_fields);
        layout_fields.setAutoCreateGaps(true);
        layout_fields.setAutoCreateContainerGaps(true);

        JLabel srcDstIdLabel = new JLabel("Source and Destination IMC IDs (can be blanc)");
        JLabel srcDestEntLabel = new JLabel("Source and Destination Entities IDs (can be blanc)");
        
        layout_fields.setHorizontalGroup(
                layout_fields.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(srcDstIdLabel)
                        .addGroup(layout_fields.createSequentialGroup().addComponent(srcId).addComponent(dstId))
                        .addComponent(srcDestEntLabel)
                        .addGroup(layout_fields.createSequentialGroup().addComponent(srcEntId).addComponent(dstEntId)));

        layout_fields.setVerticalGroup(layout_fields.createSequentialGroup().addComponent(srcDstIdLabel)
                .addGroup(layout_fields.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(srcId, 25, 25, 25).addComponent(dstId, 25, 25, 25))
                .addComponent(srcDestEntLabel).addGroup(layout_fields.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(srcEntId, 25, 25, 25).addComponent(dstEntId, 25, 25, 25)));

        holder_fields = new JPanel();
        GroupLayout layout = new GroupLayout(holder_fields);
        holder_fields.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        
        GroupLayout.ParallelGroup horizontal = layout.createParallelGroup(Alignment.CENTER);
        GroupLayout.SequentialGroup vertical = layout.createSequentialGroup();
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(horizontal));
        layout.setVerticalGroup(vertical);
        
        IMCMessage msg = IMCDefinition.getInstance().create(getMessageName());
        JLabel previousLabel = null;
        
        for (int i = 0; i < m_fields.size(); i++) {
            String field = m_fields.get(i);
            
//            System.err.println("Field: "+field);
//            System.err.println("Remain: "+(i%3));
            
            JLabel label = new JLabel(field);
            String dType = msg.getTypeOf(field);
            System.err.println("Type of field "+field+" "+dType);
            if(dType.equalsIgnoreCase("message") ) { // Inline Message
                List<String> mList = new ArrayList<String>(IMCDefinition.getInstance().getMessageCount());
                for (String mt : IMCDefinition.getInstance().getMessageNames()) {
                    mList.add(mt);
                }
                Collections.sort(mList);
                JComboBox<String> messagesComboBox = new JComboBox<String>(mList.toArray(new String[mList.size()]));
                JButton edit = new JButton("Edit");

                edit.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        IMCFieldsPane inceptionFields = new IMCFieldsPane((String) messagesComboBox.getSelectedItem()); 
                        JDialog dg = new JDialog(SwingUtilities.getWindowAncestor(IMCFieldsPane.this.getContents()),
                                ModalityType.DOCUMENT_MODAL);
                        dg.setTitle("Insert Inline IMC Message");
                        dg.setContentPane(inceptionFields.getContents());
                        dg.setSize(500, 500);
                        dg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        GuiUtils.centerParent(dg, (Window) dg.getParent());
                        dg.setVisible(true);
                        
                    }
                });
                label.setLabelFor(messagesComboBox);
                horizontal.addGroup(layout.createSequentialGroup().
                    addComponent(label).addComponent(messagesComboBox).addComponent(edit));
                vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
                    addComponent(label).addComponent(messagesComboBox).addComponent(edit));
            }
            else if(dType.equalsIgnoreCase("message-list")) {
                //TODO list of Message?
                
            }
            else {
                JTextField tField = new JTextField(String.valueOf(msg.getValue(field)));
                label.setLabelFor(tField);
                horizontal.addGroup(layout.createSequentialGroup().
                    addComponent(label).addComponent(tField));
                vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
                    addComponent(label).addComponent(tField));
            }
            if ( i%3 == 0) {
                //TODO change line
            }
            if(i > 0) {
                layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
            }
            previousLabel = label;

        }
        // layout.linkSize(SwingConstants.HORIZONTAL, labels[i], labels[0]);

        this.content = new JPanel(new BorderLayout()){

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(500, 300);
            }
        };
        this.scrollable = new JScrollPane(holder_fields) {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(450, 250);
            }
        };
        this.content.add(holder_Hfields, BorderLayout.CENTER);
        this.content.add(scrollable, BorderLayout.SOUTH);

    }

    public IMCMessage getImcMessage() {
        // TODO collect all values from fields
        IMCMessage msg = IMCDefinition.getInstance().create(getMessageName()); //Try parser for each data type
        //fillSrcDstId
        // TODO validate message -> return it
        return msg;
    }

    /**
     * @return
     */
    public String getMessageName() {
        return IMCDefinition.getInstance().getMessageName(mid);
    }
    
    /**
     * @param sMsg
     */
    protected void fillSrcDstId(IMCMessage sMsg) {
        if (!"".equalsIgnoreCase(srcId.getText())) {
            int id = -1;
            try {
                id = (int) ImcId16.parseImcId16(srcId.getText());
            } catch (NumberFormatException e) {
                try {
                    id = Integer.parseInt(srcId.getText());
                } catch (NumberFormatException e1) {
                    try {
                        id = Integer.parseInt(srcId.getText(), 16);
                    } catch (NumberFormatException e2) {
                        e2.printStackTrace();
                    }
                }
            }
            if (id < 0) {
                srcId.setText("");
            }
            else {
                    sMsg.getHeader().setValue("src", id);
            }
        }

        if (!"".equalsIgnoreCase(dstId.getText())) {
            int id = -1;
            try {
                id = (int) ImcId16.parseImcId16(dstId.getText());
            } catch (NumberFormatException e) {
                try {
                    id = Integer.parseInt(dstId.getText());
                } catch (NumberFormatException e1) {
                    try {
                        id = Integer.parseInt(dstId.getText(), 16);
                    } catch (NumberFormatException e2) {
                    }
                }
            }
            if (id < 0) {
                dstId.setText("");
            }
            else {
                    sMsg.getHeader().setValue("dst", id);
            }
        }
    }
public static void main (String[] args) {
    JFrame frame = new JFrame("IMC Fields Tab");
    JPanel content = new IMCFieldsPane("TransmissionRequest").getContents();
    frame.setSize(500, 500);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setContentPane(content);
    frame.setVisible(true);
}
}
