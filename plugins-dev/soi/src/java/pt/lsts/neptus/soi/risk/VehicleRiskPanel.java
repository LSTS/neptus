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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.util.DateTimeUtil;

public class VehicleRiskPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JStatusLabel lblName, lblNextComm, lblLastComm, lblFuel, lblDistance, lblCollisions, lblErrors;
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
        setTextSize(18);
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
    
    private JStatusLabel createLabel(String text) {
        Font font = new Font("Arial", Font.BOLD, 18);
        JStatusLabel lbl = new JStatusLabel(text, font);       
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.gray.brighter()));
        return lbl;
    }
    
    public void setTextSize(int size) {
        List<JStatusLabel> labels = Arrays.asList(lblCollisions, lblDistance, lblErrors, lblFuel, lblLastComm, lblName, lblNextComm);
        labels.forEach(l -> l.setFontSize(size));
    }
    
    public void setRiskAnalysis(VehicleRiskAnalysis analysis) {
        this.analysis = analysis;
        
        if (analysis.problems().isEmpty()) {
            setBackground(new Color(224,255,224));
        }
        else {
            setBackground(new Color(255, 180, 180));
            NeptusLog.pub().warn("PROBLEMS for "+analysis.problems());
        }
        if (analysis.lastCommunication == null)
            lblLastComm.setState("N/D", false, "No message has been received");
        else {                
            long ellapsed = System.currentTimeMillis() - analysis.lastCommunication.getTime();
            String strEllapsed = DateTimeUtil.milliSecondsToFormatedString(ellapsed);
            lblLastComm.setState(strEllapsed, ellapsed / 60_000 > 30, "");
        }
        
        if (analysis.nextCommunication == null || analysis.nextCommunication.before(new Date())) {
            lblNextComm.setState("N/D", true, "No future communications scheduled");
        }
        else {
            long timeDiff = analysis.nextCommunication.getTime() - System.currentTimeMillis();
            lblNextComm.setState(DateTimeUtil.milliSecondsToFormatedString(timeDiff), false, "ETA: "+analysis.nextCommunication);
        }
        
        if (analysis.collisions == null || analysis.collisions.isEmpty()) {
            lblCollisions.setState("No", false, "No collisions have been detected");
        }
        else {
            String text = analysis.collisions.values().stream().collect(Collectors.joining("<br>"));
            lblCollisions.setState(analysis.collisions.size()+"!", true, "<html>"+text);
        }
        
        if (analysis.errors.isEmpty()) {
            lblErrors.setState("N/D", false, "No reported errors");
        }
        else {
            lblErrors.setState(""+analysis.errors.size(), true, ""+analysis.errors);
        }
                    
        if (analysis.location != null) {
            double dist = MyState.getLocation().getDistanceInMeters(analysis.location);
            String distance = String.format(Locale.US, "%.0f m",
                    dist);
            lblDistance.setState(distance, false, "distance to home location");
        }
        else {
            lblDistance.setState("N/D", true, "No home location has been set");            
        }
        repaint();
    }
}