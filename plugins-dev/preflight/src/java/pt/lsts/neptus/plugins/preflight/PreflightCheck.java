/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 23 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public abstract class PreflightCheck extends JPanel {
    protected static final int VALIDATED = 0;
    protected static final int NOT_VALIDATED = -1;
    protected static final int VALIDATED_WITH_WARNINGS = 1;
    
    protected static final Color COLOR_VALIDATED = new Color(200, 255, 200);
    protected static final Color COLOR_NOT_VALIDATED = new Color(255, 128, 128);
    protected static final Color COLOR_VALIDATED_W_WARNINGS = new Color(255, 255, 128);
    
    private JLabel description;
    private String category;
    private JCheckBox checkBox;
    private JLabel valuesLabel; /* E.g, whithinRange values */
    private boolean isRegistered;
    private boolean isPeriodic;
      
    public PreflightCheck(String description, String category, String type) {
        super();
        init(description, category);
        buildPanel(type);
        setBorder(BorderFactory.createLineBorder(Color.white, 1));
        
        setState(NOT_VALIDATED);
    }
        
    private void init(String description, String category) {
        setLayout(new GridLayout(0, 3));
        setBackground(Color.WHITE);
        setMinimumSize(new Dimension(Preflight.MAX_COMPONENT_WIDTH, 20));
        setMaximumSize(new Dimension(Preflight.MAX_COMPONENT_WIDTH, 20));
        
        this.description = new JLabel(description, SwingConstants.CENTER);
        this.category = category;
        
        checkBox = new JCheckBox();
        checkBox.setBackground(Color.WHITE);
        valuesLabel = new JLabel("?", SwingConstants.CENTER);
        
        add(this.description, 0);
        this.description.setOpaque(true);
        add(valuesLabel, 1);
        valuesLabel.setOpaque(true);
        add(checkBox, 2);
        checkBox.setOpaque(true);
        
        valuesLabel.setVisible(false);
        checkBox.setVisible(false);
    }
    
    /* Adds the panel elements, according to the check type */
    private void buildPanel(String type) {
        if(type.equals("")) {
            addValuesLabel();
            addCheckBox();
        }
        else if(type.equals("Automated")) {
            addValuesLabel();
        }
        else if(type.equals("Manual")) {
            addCheckBox();
        }
    }
    
    public boolean isRegistered() {
        return isRegistered;
    }
    
    public void registerToEventbus() {
        ImcMsgManager.getManager().registerBusListener(this);
        isRegistered = true;
    }
    
    protected boolean isPeriodic() {
        return isPeriodic;
    }
    
    protected void setAsPeriodic() {
        isPeriodic = true;
        PeriodicUpdatesService.registerPojo(this);        
    }
    
    protected void stopPeriodicUpdates() {
        if(isPeriodic)
            PeriodicUpdatesService.unregisterPojo(this);
    }
    
    protected void unregister() {
        if(isRegistered)
            ImcMsgManager.getManager().unregisterBusListener(this);
    }
        
    public void setState(int newState) {
        Color color = COLOR_NOT_VALIDATED;
        
        if(newState == VALIDATED)
            color = COLOR_VALIDATED;
        else if(newState == VALIDATED_WITH_WARNINGS)
            color = COLOR_VALIDATED_W_WARNINGS;
        
        
        setCheckBackgroundAs(color);
    }
    
    private void setCheckBackgroundAs(Color color) {
        setBackground(color);
        description.setBackground(color);
        valuesLabel.setBackground(color);
        checkBox.setBackground(color);
    }
    
    public void setValuesLabelText(String txt) {
        valuesLabel.setText(txt);
        revalidate();
    }
       
    public void addValuesLabel() {
        valuesLabel.setVisible(true);
    }
    
    public void addCheckBox() {
        checkBox.setVisible(true);
        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(checkBox.isVisible()) {
                    if(!checkBox.isSelected())
                        setState(NOT_VALIDATED);
                    else
                        setState(VALIDATED);
                    //revalidate();
                }
            }
        });
    }
    
    protected boolean messageFromMainVehicle(String msgSrc) {
        return(msgSrc.equals(Preflight.CONSOLE.getMainSystem()));
    }
           
    public String getDescription() {
        return description.getText();
    }
    
    public String getCategory() {
        return category;
    }
}
