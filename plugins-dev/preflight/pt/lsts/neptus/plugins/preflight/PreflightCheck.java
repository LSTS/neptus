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
 * Author: tsmarques
 * 23 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public abstract class PreflightCheck extends JPanel {
    protected static final String VALIDATED = "[OK]";
    protected static final String NOT_VALIDATED = "[]";
    
    private JLabel state;
    private JLabel description;
    private String category;
    private JCheckBox checkBox;
    private JLabel valuesLabel; /* E.g, whithinRange values */
    private boolean maintainState;
      
    public PreflightCheck(String description, String category, boolean maintainState, String type) {
        super();
        init(description, category, maintainState);
        buildPanel(type);
        
        ImcMsgManager.registerBusListener(this); /* Subscribe to messages */
    }
        
    private void init(String description, String category, boolean maintainState) {
        setLayout(new GridLayout(0, 4));
        setBackground(Color.WHITE);
        setMinimumSize(new Dimension(Preflight.MAX_COMPONENT_WIDTH, 20));
        setMaximumSize(new Dimension(Preflight.MAX_COMPONENT_WIDTH, 20));
        
        this.state = new JLabel("", SwingConstants.CENTER);
        this.description = new JLabel(description, SwingConstants.CENTER);
        this.category = category;
        this.maintainState = maintainState;
        
        checkBox = new JCheckBox();
        checkBox.setBackground(Color.WHITE);
        valuesLabel = new JLabel("?", SwingConstants.CENTER);
        
        add(this.description, 0);
        add(state, 1);
        add(valuesLabel, 2);
        add(checkBox, 3);
        
        state.setVisible(false);
        valuesLabel.setVisible(false);
        checkBox.setVisible(false);
    }
    
    /* Adds the panel elements, according to the check type */
    private void buildPanel(String type) {
        if(type.equals("")) {
            addStateLabel();
            addValuesLabel();
            addCheckBox();
        }
        else if(type.equals("Automated")) {
            addStateLabel();
            addValuesLabel();
        }
        else if(type.equals("Manual")) {
            addStateLabel();            
            addCheckBox();
        }
        else if(type.equals("Info")) {
            addValuesLabel();
        }
    }
        
    public void setState(String newState) {
        if(newState.equals(VALIDATED)) {
            state.setForeground(Color.GREEN);
            state.setText(VALIDATED);
        }
        else {
            state.setForeground(Color.RED);
            state.setText(NOT_VALIDATED);
        }
        revalidate();
    }
    
    public void setValuesLabelText(String txt) {
        valuesLabel.setText(txt);
        revalidate();
    }
    
    public void addStateLabel() {
        state.setForeground(Color.RED);
        state.setText(NOT_VALIDATED);
        state.setVisible(true);
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
                    if(!checkBox.isSelected()) {
                        state.setForeground(Color.RED);
                        state.setText(NOT_VALIDATED);
                    }
                    else {
                        state.setForeground(Color.GREEN);
                        state.setText(VALIDATED);
                    }
                    revalidate();
                }
            }
        });
    }
    
    protected boolean messageFromMainVehicle(String msgSrc) {
        return(msgSrc.equals(Preflight.CONSOLE.getMainSystem()));
    }
        
    public String getState() {
        return state.getText();
    }
    
    public String getDescription() {
        return description.getText();
    }
    
    public String getCategory() {
        return category;
    }
        
    public boolean maintainStateOnReboot() {
        return maintainState;
    }
}
