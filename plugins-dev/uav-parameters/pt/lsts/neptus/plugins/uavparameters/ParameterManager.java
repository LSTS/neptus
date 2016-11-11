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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * 07/11/2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author Manuel R.
 *
 */

@SuppressWarnings("serial")
@PluginDescription(name = "UAV Parameter Configuration", icon = "images/settings2.png")
@Popup(name = "UAV Parameter Configuration Panel", pos = POSITION.CENTER, height = 500, width = 400, accelerator = '0')
public class ParameterManager extends ConsolePanel implements MainVehicleChangeListener {
    private JTextField findTxtField;
    private JTable table;


    public ParameterManager(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout(0, 0));

        JPanel mainPanel = new JPanel();
        add(mainPanel);
        mainPanel.setLayout(new BorderLayout(0, 0));

        JPanel tablePanel = new JPanel();
        mainPanel.add(tablePanel, BorderLayout.EAST);
        tablePanel.setLayout(new MigLayout("", "[grow]", "[][][][][][][][]"));

        JButton btnGetParams = new JButton("Load Parameters");
        tablePanel.add(btnGetParams, "cell 0 0,growx");

        JButton btnWriteParams = new JButton("Write Parameters");
        tablePanel.add(btnWriteParams, "cell 0 1,growx");

        JButton btnSaveToFile = new JButton("Save to File");
        tablePanel.add(btnSaveToFile, "cell 0 2,growx");

        JButton btnLoadFromFile = new JButton("Load from File");
        tablePanel.add(btnLoadFromFile, "cell 0 3,growx");

                                JButton btnFind = new JButton("Find");
                                tablePanel.add(btnFind, "cell 0 6,growx");

                findTxtField = new JTextField();
                tablePanel.add(findTxtField, "cell 0 7,growx");
                findTxtField.setColumns(10);

        JScrollPane scrollPane = new JScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        table = new JTable();
        table.setModel(new DefaultTableModel(
            new Object[][] {
            },
            new String[] {
                "Command", "Value"
            }
        ));
        scrollPane.setViewportView(table);
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
    @Override
    public void initSubPanel() {
        setResizable(false);
        // TODO Auto-generated method stub

    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange e) {
        System.out.println("Vehicle changed "+ e.getCurrent() );
    }

}