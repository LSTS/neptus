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
 * 22 de Fev de 2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import pt.lsts.neptus.console.plugins.planning.UavPiccoloControl.PiccoloControlConfiguration;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PiccoloControlConfigurationPanel extends JPanel {

    private PiccoloControlConfiguration config = null;
    
//    private short planWpMin = -1, planWpMax = -1, handoverWpMin = -1, handoverWpMax = -1;
//    private float serviceWpRadius = -1, serviceWpAltitude = -1, serviceWpSpeed = -1;
    
    //GUI
    private JFormattedTextField planWpMinText, planWpMaxText, handoverWpMinText, handoverWpMaxText,
            serviceWpRadiusText, serviceWpAltitudeText, serviceWpSpeedText;
    private JLabel planWpLabel, handoverWpLabel, bracketsLabel1, bracketsLabel2, bracketsLabel3,
            bracketsLabel4, commaLabel1, commaLabel2, serviceWpLabel, serviceWpRadiusLabel, serviceWpAltitudeLabel,
            serviceWpSpeedLabel;
//    private JButton okButton, cancelButton;
    
    /**
     * 
     */
    public PiccoloControlConfigurationPanel() {
        initialize();
    }
    
    /**
     * 
     */
    private void initialize() {
        planWpLabel = new JLabel("Plan waypoints:");
        handoverWpLabel = new JLabel("Handover waypoints:");
        bracketsLabel1 = new JLabel("[");
        bracketsLabel2 = new JLabel("]");
        bracketsLabel3 = new JLabel("[");
        bracketsLabel4 = new JLabel("]");
        commaLabel1 = new JLabel(", ");
        commaLabel2 = new JLabel(", ");
        serviceWpLabel = new JLabel("Service waypoint");
        serviceWpRadiusLabel = new JLabel("Radius (m):");
        serviceWpAltitudeLabel = new JLabel("Altitude (m):");
        serviceWpSpeedLabel = new JLabel("Speed (m/s):");
        
        planWpMinText = new JFormattedTextField(NumberFormat.getIntegerInstance());
        planWpMaxText = new JFormattedTextField(NumberFormat.getIntegerInstance());
        handoverWpMinText = new JFormattedTextField(NumberFormat.getIntegerInstance());
        handoverWpMaxText = new JFormattedTextField(NumberFormat.getIntegerInstance());
        serviceWpRadiusText = new JFormattedTextField(NumberFormat.getNumberInstance());
        serviceWpAltitudeText = new JFormattedTextField(NumberFormat.getNumberInstance());
        serviceWpSpeedText = new JFormattedTextField(NumberFormat.getNumberInstance());
        
        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        this.setLayout(layout);
        
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(planWpLabel)
                            .addComponent(bracketsLabel1)
                            .addGap(2)
                            .addComponent(planWpMinText, 40, 70, Short.MAX_VALUE)
                            .addGap(2)
                            .addComponent(commaLabel1)
                            .addGap(2)
                            .addComponent(planWpMaxText, 40, 70, Short.MAX_VALUE)
                            .addGap(2)
                            .addComponent(bracketsLabel2))    
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(handoverWpLabel)
                            .addComponent(bracketsLabel3)
                            .addGap(2)
                            .addComponent(handoverWpMinText, 40, 70, Short.MAX_VALUE)
                            .addGap(2)
                            .addComponent(commaLabel2)
                            .addGap(2)
                            .addComponent(handoverWpMaxText, 40, 70, Short.MAX_VALUE)
                            .addGap(2)
                            .addComponent(bracketsLabel4)))    
            .addGroup(layout.createParallelGroup()
                    .addComponent(serviceWpLabel)
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(serviceWpRadiusLabel)
                            .addComponent(serviceWpRadiusText, 80, 200, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(serviceWpAltitudeLabel)
                            .addComponent(serviceWpAltitudeText, 80, 200, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(serviceWpSpeedLabel)
                            .addComponent(serviceWpSpeedText, 80, 200, Short.MAX_VALUE)))
            );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(planWpLabel)
                                .addComponent(bracketsLabel1)
                                .addComponent(planWpMinText)
                                .addComponent(commaLabel1)
                                .addComponent(planWpMaxText)
                                .addComponent(bracketsLabel2))    
                        .addGroup(layout.createParallelGroup()
                                .addComponent(handoverWpLabel)
                                .addComponent(bracketsLabel3)
                                .addComponent(handoverWpMinText)
                                .addComponent(commaLabel2)
                                .addComponent(handoverWpMaxText)
                                .addComponent(bracketsLabel4)))    
                .addGroup(layout.createSequentialGroup()
                        .addComponent(serviceWpLabel)
                        .addGroup(layout.createParallelGroup()
                                .addComponent(serviceWpRadiusLabel)
                                .addComponent(serviceWpRadiusText))
                        .addGroup(layout.createParallelGroup()
                                .addComponent(serviceWpAltitudeLabel)
                                .addComponent(serviceWpAltitudeText))
                        .addGroup(layout.createParallelGroup()
                                .addComponent(serviceWpSpeedLabel)
                                .addComponent(serviceWpSpeedText)))
                );

        layout.linkSize(SwingConstants.VERTICAL, planWpLabel, planWpMinText, planWpMaxText,
                handoverWpLabel, handoverWpMinText, handoverWpMaxText, serviceWpRadiusText,
                serviceWpAltitudeText, serviceWpSpeedText);
        
        layout.linkSize(SwingConstants.HORIZONTAL, planWpLabel, handoverWpLabel);
        layout.linkSize(SwingConstants.HORIZONTAL, serviceWpRadiusLabel, serviceWpAltitudeLabel,
                serviceWpSpeedLabel);
//        layout.linkSize(SwingConstants.HORIZONTAL, planWpMinText, planWpMaxText, handoverWpMinText,
//                handoverWpMaxText);
//        layout.linkSize(SwingConstants.HORIZONTAL, serviceWpRadiusText, serviceWpAltitudeText,
//                serviceWpSpeedText);
    }

    public PiccoloControlConfiguration getConfiguration() {
        if (config == null)
            return new PiccoloControlConfiguration();
        
        getConfigurationWorker(config);
        return config;
    }

    public PiccoloControlConfiguration getConfigurationWorker(PiccoloControlConfiguration config) {        
        config.setPlanWpMin((short) Integer.parseInt(planWpMinText.getText()));
        config.setPlanWpMax((short) Integer.parseInt(planWpMaxText.getText()));
        config.setHandoverWpMin((short) Integer.parseInt(handoverWpMinText.getText()));
        config.setHandoverWpMax((short) Integer.parseInt(handoverWpMaxText.getText()));
        config.setServiceRadius(Double.parseDouble(serviceWpRadiusText.getText()));
        config.setServiceAltitude(Double.parseDouble(serviceWpAltitudeText.getText()));
        config.setServiceSpeed(Double.parseDouble(serviceWpSpeedText.getText()));
        
        return config;
    }

    public void setConfiguration(PiccoloControlConfiguration config) {
        this.config = config;
        planWpMinText.setText(""+this.config.planWpMin);
        planWpMaxText.setText(""+this.config.planWpMax);
        handoverWpMinText.setText(""+this.config.handoverWpMin);
        handoverWpMaxText.setText(""+this.config.handoverWpMax);
        serviceWpRadiusText.setText(""+this.config.serviceRadius);
        serviceWpAltitudeText.setText(""+this.config.serviceAltitude);
        serviceWpSpeedText.setText(""+this.config.serviceSpeed);
    }
    
    public String[] validateConfiguration() {
        Vector<String> list = new Vector<String>(); 
        PiccoloControlConfiguration cfg = new PiccoloControlConfiguration();
        getConfigurationWorker(cfg);
        if (cfg.planWpMin < -1 || cfg.planWpMin > 99)
            list.add("planWpMin should be in [-1, 99] range");
        if (cfg.planWpMax < -1 || cfg.planWpMax > 99)
            list.add("planWpMax should be in [-1, 99] range");
        if (cfg.handoverWpMin < -1 || cfg.handoverWpMin > 99)
            list.add("handoverWpMin should be in [-1, 99] range");
        if (cfg.handoverWpMax < -1 || cfg.handoverWpMax > 99)
            list.add("handoverWpMax should be in [-1, 99] range");
        if (cfg.planWpMax < cfg.planWpMin)
            list.add("planWpMax is higher than planWpMin");
        if (cfg.handoverWpMax < cfg.handoverWpMin)
            list.add("handoverWpMax is higher than handoverWpMin");
        
        if (list.size() == 0) {
            if (!(cfg.planWpMin == -1 && cfg.planWpMax == -1 && cfg.handoverWpMin == -1 && cfg.handoverWpMax == -1))
                if (cfg.planWpMin <= cfg.handoverWpMin && cfg.handoverWpMin <= cfg.planWpMax
                        || cfg.planWpMin <= cfg.handoverWpMax && cfg.handoverWpMax <= cfg.planWpMax)
                    list.add("plan and handover ranges are overlapping");
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    boolean userCanceled = false;
    public PiccoloControlConfiguration showEditPanel(PiccoloControlConfiguration source,
            Window parent) {
        userCanceled = false;
        setConfiguration(source);
        final JDialog dialog = new JDialog(parent, "Set Configurations");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.add(this);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                userCanceled = true;
            }
        });
        final JButton okButton = new JButton(new AbstractAction("Ok") {
            @Override
            public void actionPerformed(ActionEvent e) {
                userCanceled = false;
                dialog.dispose();
            }
        });
        GuiUtils.reactEnterKeyPress(okButton);
        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                okButton.requestFocusInWindow();
            }
        });
        //dialog.add(okButton, BorderLayout.SOUTH);
        JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                userCanceled = true;
                dialog.dispose();
            }
        });
        GuiUtils.reactEscapeKeyPress(cancelButton);
        JPanel jp = new JPanel();
        jp.setLayout(new GridLayout(0, 2));
        jp.add(okButton);
        jp.add(cancelButton);
        dialog.add(jp, BorderLayout.SOUTH);
        dialog.pack();
        GuiUtils.centerParent(dialog, parent);
        dialog.setVisible(true);
        if (!userCanceled) {
            String[] errors = validateConfiguration();
            if (errors.length == 0)
                return getConfiguration();
            else {
                String message = "";
                for (String msg : errors)
                    message += msg + "\n";
                JOptionPane.showMessageDialog(parent, message, "Configuration not valid",
                        JOptionPane.ERROR_MESSAGE);
                return showEditPanel(source, parent);
            }
        }
        return null;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        //GuiUtils.testFrame(new PiccoloControlConfigurationPanel()); //320, 240
        new PiccoloControlConfigurationPanel().showEditPanel(new PiccoloControlConfiguration(),
                new JFrame());
    }

}
