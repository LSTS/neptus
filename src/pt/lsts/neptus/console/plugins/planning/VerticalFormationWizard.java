/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * 13/05/2016
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.wizard.PlanSelectionPage;
import pt.lsts.neptus.wizard.VehicleSelectionPage;
import pt.lsts.neptus.wizard.WizardPage;

/**
 * @author zp
 */
@PluginDescription
@Popup(width=800, height=600, pos=POSITION.CENTER)
public class VerticalFormationWizard extends ConsolePanel {

    private static final long serialVersionUID = -8189579968717153114L;

    private JButton btnAdvance, btnBack, btnCancel;
    private JPanel main;
    
    ArrayList<WizardPage<?>> pages = new ArrayList<>(); 
    private JLabel lblTop = new JLabel();
    private PlanSelectionPage planSelection;
    private VehicleSelectionPage vehicleSelection;
    int page = 0;
    
    /**
     * @param console
     */
    public VerticalFormationWizard(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        btnAdvance = new JButton(I18n.text("Next"));
        btnBack = new JButton(I18n.text("Previous"));
        btnCancel = new JButton(I18n.text("Cancel"));
        
        btnAdvance.addActionListener(this::advance);
        btnBack.addActionListener(this::back);
        btnCancel.addActionListener(this::cancel);
        
        JPanel btns = new JPanel(new BorderLayout());
        JPanel flow1 = new JPanel(new FlowLayout());
        flow1.add(btnBack);
        JPanel flow2 = new JPanel(new FlowLayout());
        flow2.add(btnCancel);
        flow2.add(btnAdvance);
        btns.add(flow1, BorderLayout.WEST);
        btns.add(flow2, BorderLayout.EAST);
        add(btns, BorderLayout.SOUTH);
        
        lblTop.setPreferredSize(new Dimension(60, 60));
        lblTop.setMinimumSize(lblTop.getPreferredSize());
        lblTop.setOpaque(true);
        lblTop.setBackground(Color.white);
        lblTop.setFont(new Font("Helvetica", Font.BOLD, 18));
        add(lblTop, BorderLayout.NORTH);
        
        planSelection = new PlanSelectionPage(console.getMission(), false);
        vehicleSelection = new VehicleSelectionPage(new ArrayList<VehicleType>(), true);
        pages.add(planSelection);
        pages.add(vehicleSelection);
        main = new JPanel(new CardLayout());
        pages.forEach( p -> main.add(p, p.getTitle()));
        add(main, BorderLayout.CENTER);
        lblTop.setText(pages.get(0).getTitle());
    }
    
    public void advance(ActionEvent evt) {
        if (page == pages.size()-1) {            
            System.out.println("FINISH!");
            return;
        }
        else if (page == pages.size()-2) {
            btnAdvance.setText(I18n.text("Finish"));
        }
        
        WizardPage<?> currentPage = pages.get(++page);
        
        
        lblTop.setText(currentPage.getTitle());
        ((CardLayout)main.getLayout()).show(main, currentPage.getTitle());
    }
    
    public void back(ActionEvent evt) {
        if (page == 0)
            return;
        btnAdvance.setText(I18n.text("Next"));
        WizardPage<?> currentPage = pages.get(--page);
        lblTop.setText(currentPage.getTitle());
        ((CardLayout)main.getLayout()).show(main, currentPage.getTitle());
                
    }

    public void cancel(ActionEvent evt) {
        System.out.println("Cancel...");
    }


    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
