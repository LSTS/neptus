/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * May 8, 2018
 */
package pt.lsts.neptus.soi.risk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.util.DateTimeUtil;

public class VehicleRiskPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel lblName, lblNextComm, lblLastComm, lblFuel, lblDistance, lblCollisions, lblErrors;
    private VehicleRiskAnalysis analysis = new VehicleRiskAnalysis();
    
    public VehicleRiskPanel(String vehicle) {
        setLayout(new GridLayout(1, 7));
        lblName = createLabel(vehicle);
        lblLastComm = createLabel("N/D");
        lblNextComm = createLabel("N/D");
        lblFuel = createLabel("N/D");
        lblDistance = createLabel("N/D");
        lblCollisions = createLabel("N/D");
        lblErrors = createLabel("N/D");
        setBackground(Color.WHITE);
        
        setMaximumSize(new Dimension(4000, 100));
        add(lblName);
        add(lblLastComm);
        add(lblNextComm);
        add(lblFuel);
        add(lblDistance);
        add(lblCollisions);
        add(lblErrors);          
        setTextSize(24);
    }
    
    @Override
    public void paint(Graphics g) {
        setOpaque(false);

        if (analysis.problems().size() > 0)
            setBackground(new Color(255,180, 180));
        else
            setBackground(new Color(128, 180, 128));

        Graphics2D g2 = (Graphics2D)g;
        g2.setPaint(new GradientPaint(0, 0, Color.white, 0, getHeight(), getBackground()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }
    
    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        return lbl;
    }
    
    private void setTextSize(int size) {
        List<JLabel> labels = Arrays.asList(lblCollisions, lblDistance, lblErrors, lblFuel, lblLastComm, lblName, lblNextComm);
        Font font = new Font("Arial", Font.BOLD, size);
        
        for (JLabel l : labels) {
            l.setFont(font);
            l.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.gray.brighter()));
            l.setHorizontalAlignment(JLabel.CENTER);
        }
    }
    
    public void setRiskAnalysis(VehicleRiskAnalysis analysis) {
        this.analysis = analysis;
        
        if (analysis.problems().isEmpty())
            setBackground(new Color(224,255,224));
        else
            setBackground(new Color(255, 180, 180));
        
        if (analysis.lastCommunication == null) {
            lblLastComm.setText("N/D");
            lblLastComm.setToolTipText("No message has been received");
            lblLastComm.setForeground(Color.gray.darker());
        }
        else {                
            long ellapsed = System.currentTimeMillis() - analysis.lastCommunication.getTime();
            lblLastComm.setText(DateTimeUtil.milliSecondsToFormatedString(ellapsed));
            if (ellapsed / 60_000 < 30)
                lblLastComm.setForeground(Color.green.darker());
            else
                lblLastComm.setForeground(Color.red.darker());
        }
        
        if (analysis.nextCommunication == null) {
            lblNextComm.setText("N/D");
            lblNextComm.setToolTipText("No future communications scheduled");
        }
        else {
            long timeDiff = analysis.nextCommunication.getTime() - System.currentTimeMillis();
            lblNextComm.setText(DateTimeUtil.milliSecondsToFormatedString(timeDiff));
            lblNextComm.setToolTipText("ETA: "+analysis.nextCommunication);
            lblNextComm.setForeground(Color.green.darker());                
        }
        
        if (analysis.collisions == null || analysis.collisions.isEmpty()) {
            lblCollisions.setText("0");
            lblCollisions.setForeground(Color.green.darker());
            lblCollisions.setToolTipText("No collisions have been detected");
        }
        else {
            lblCollisions.setText(""+analysis.collisions.size());
            lblCollisions.setForeground(Color.red.darker());
            String text = analysis.collisions.values().stream().collect(Collectors.joining("\n"));
            lblCollisions.setToolTipText(text);
        }
                    
        if (analysis.location != null) {
            double dist = MyState.getLocation().getDistanceInMeters(analysis.location);
            String distance = String.format(Locale.US, "%.0f m",
                    dist);
            lblDistance.setText(distance);                
        }
        else {
            lblDistance.setText("N/D"); 
        }
        repaint();
    }
}