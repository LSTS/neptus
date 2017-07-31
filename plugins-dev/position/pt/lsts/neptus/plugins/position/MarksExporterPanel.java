/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 23/05/2017
 */
package pt.lsts.neptus.plugins.position;


import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.CheckboxList;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.MarksKMLHandler;
import pt.lsts.neptus.util.csv.MarksCSVHandler;

/**
 * @author tsm
 */
public class MarksExporterPanel extends JPanel {
    public static String csvDelimiter = ",";

    private final int MAIN_WIDTH = 300;
    private final int MAIN_HEIGHT = 300;

    private final JButton fromCsv = new JButton(I18n.text("As CSV"));
    private final JButton fromKml = new JButton(I18n.text("As KML"));

    private final JLabel sourceLabel = new JLabel(I18n.text(""));
    private final JFileChooser fileChooser = new JFileChooser();

    private final CheckboxList marksList = new CheckboxList();
    private JScrollPane listScroller = new JScrollPane(marksList);

    private final JPanel exporterSourcePanel = new JPanel();

    private HashMap<String, MarkElement> marksToExport = null;

    private static boolean validOperation = false;

    public MarksExporterPanel(List<MarkElement> marks) {
        loadAvailableMarks(marks);

        this.setBounds(0, 0, MAIN_WIDTH, MAIN_HEIGHT);
        this.setLayout(new MigLayout(""));


        exporterSourcePanel.setBounds(0, 0, MAIN_WIDTH / 5, MAIN_HEIGHT);
        marksList.setBounds(0, 0, MAIN_WIDTH, MAIN_HEIGHT);
        listScroller.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT));

        fromCsv.setSelected(true);

        exporterSourcePanel.setLayout(new MigLayout("wrap 2"));
        exporterSourcePanel.add(fromCsv);
        exporterSourcePanel.add(fromKml);

        fromCsv.addActionListener(e ->{
            if(fileChooser.showDialog(null, I18n.text("To CSV")) != JFileChooser.APPROVE_OPTION)
                return;

            String exportPath = fileChooser.getSelectedFile().getAbsolutePath();
            validOperation = MarksCSVHandler.exportCsv(exportPath, fetchSelectedMarks(), csvDelimiter);
        });

        fromKml.addActionListener(e ->{
            if(fileChooser.showDialog(null, I18n.text("To KML")) != JFileChooser.APPROVE_OPTION)
                return;

            String exportPath = fileChooser.getSelectedFile().getAbsolutePath();
            validOperation = MarksKMLHandler.exportKML(exportPath, fetchSelectedMarks());
        });

        add(sourceLabel, "w 20%, h 10%, wrap");
        add(exporterSourcePanel, "w 100%, h 20%, wrap");
        add(listScroller, "w 100%, h 70%");
    }

    private List<MarkElement> fetchSelectedMarks() {
        ArrayList<MarkElement> marks = new ArrayList<>();
        Arrays.stream(marksList.getSelectedStrings())
                .forEach(id -> marks.add(marksToExport.get(id)));

        return marks;
    }

    private void loadAvailableMarks(List<MarkElement> marks) {
        marksToExport = new HashMap<>();
        JCheckBox[] checks = new JCheckBox[marks.size()];
        for (int i = 0; i < marks.size(); i++) {
            MarkElement m = marks.get(i);
            String id = m.getId();

            checks[i] = new JCheckBox(id);
            marksToExport.put(id, m);
        }

        marksList.setListData(checks);
    }

    public static boolean showPanel(Component parent, List<MarkElement> marks) {
        MarksExporterPanel panel = new MarksExporterPanel(marks);
        JOptionPane.showOptionDialog(parent, panel, I18n.text("Marks Exporter"),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{"OK", I18n.text("Cancel")}, null);

        return validOperation;
    }
}
