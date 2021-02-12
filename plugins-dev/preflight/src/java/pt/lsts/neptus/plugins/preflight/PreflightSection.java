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
 * 17 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class PreflightSection extends JPanel {
    private JPanel sectionNamePanel;
    private JLabel sectionNameLabel;    
    protected JPanel checksPanel; /* Panel that contains the "values" and checks */
    protected final GridBagConstraints c = new GridBagConstraints();    
    
    private ArrayList<PreflightCheck> checks;
    
    public PreflightSection(String sectionName) {
        /* init main panel */
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        checks = new ArrayList<>();
        
        initSectionNamePanel(sectionName);
        initChecksPanel();
        sectionNamePanel.add(sectionNameLabel, new GridBagConstraints());
        
        add(Box.createVerticalStrut(2));
        add(sectionNamePanel);
        add(Box.createVerticalStrut(1));
        add(checksPanel);
    }
    
    private void initSectionNamePanel(String sectionName) {
        sectionNamePanel = new JPanel();
        final Dimension d = new Dimension(Preflight.MAX_COMPONENT_WIDTH, 20);
        sectionNamePanel.setBackground(Color.DARK_GRAY);
        sectionNamePanel.setMaximumSize(d);
        sectionNamePanel.setMinimumSize(d);
        sectionNamePanel.setLayout(new GridBagLayout());
        
        sectionNameLabel = new JLabel("<html><b>" + sectionName + "</b></html>");
        sectionNameLabel.setForeground(Color.WHITE);
    }
    
    private void initChecksPanel() {
        checksPanel = new JPanel();
        checksPanel.setLayout(new BoxLayout(checksPanel, BoxLayout.Y_AXIS));
        checksPanel.setBackground(Color.WHITE);
    }
    
    public void addNewCheckItem(PreflightCheck newItem) {
        checksPanel.add(newItem);
        checks.add(newItem);
    }
    
    public ArrayList<PreflightCheck> getSectionChecks() {
        return checks;
    }
}
