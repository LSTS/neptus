/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jcordeiro
 * November 20, 2025
 */
package pt.lsts.neptus.mra.plots;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;
import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JButton;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jcordeiro
 *
 */
@PluginDescription(author = "jcordeiro", name = "Vehicle Configuration")
public class ConfigIniPlot extends SimpleMRAVisualization {

    private File iniFile;
    private final Map<String, List<Property>> sectionMap = new LinkedHashMap<>();
    PropertySheetPanel sheet = new PropertySheetPanel();


    public ConfigIniPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        File logDir = source.getFile("");
        if (logDir == null || !logDir.isDirectory()) {
            return false;
        }

        // check for a .ini file in the directory
        File[] iniFiles = logDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ini")
        );

        if (iniFiles != null) {
            iniFile = iniFiles[0];
            return true;
        }
        return false;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 0"));

        JLabel titleLabel = new JLabel("<html><h2>Vehicle Configuration</h2></html>");
        mainPanel.add(titleLabel, "w 100%, wrap");

        // expand/collapse buttons
        JButton collapseButton = new JButton("Collapse All");
        collapseButton.addActionListener(e -> collapseAllCategories());
        mainPanel.add(collapseButton, "split 2, gapright 5");

        JButton expandButton = new JButton("Expand All");
        expandButton.addActionListener(e -> expandAllCategories());
        mainPanel.add(expandButton, "wrap");

        sheet.removeAll();
        sheet.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        sheet.setRestoreToggleStates(false);
        sheet.setSortingCategories(false);
        sheet.setSortingProperties(false);
        sheet.setDescriptionVisible(false);
        sheet.setToolBarVisible(false);

        if (iniFile != null && iniFile.exists()) {
            try {
                Map<String, List<Property>> sections = parseIniFile(iniFile);

                for (Map.Entry<String, List<Property>> entry : sections.entrySet()) {


                    for (Property p : entry.getValue()) {
                        sheet.addProperty(p);
                    }
                }

            } catch (IOException e) {
                mainPanel.add(new JLabel("Error reading INI file: " + e.getMessage()));
            }
        }

        JScrollPane scroll = new JScrollPane(sheet);
        scroll.setBorder(null);
        mainPanel.add(scroll, "grow, push");

        return mainPanel;
    }

    private Map<String, List<Property>> parseIniFile(File iniFile) throws IOException {

        String currentSectionName = null;
        List<Property> currentSection = null;

        try (Stream<String> lines = Files.lines(iniFile.toPath(), StandardCharsets.UTF_8)) {

            for (String rawLine : lines.collect(Collectors.toList())) {
                String line = rawLine.trim();

                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#"))
                    continue;

                // new section
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSectionName = line.substring(1, line.length() - 1).trim();
                    currentSection = new ArrayList<>();
                    sectionMap.put(currentSectionName, currentSection);
                    continue;
                }

                // key=value entry
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();

                    // removes possible comments
                    int commentIdx = value.indexOf(';');
                    if (commentIdx >= 0)
                        value = value.substring(0, commentIdx).trim();

                    currentSection.add(createProperty(key, value, currentSectionName));
                }
            }
        }

        // sort map alphabetically, first "General" then rest
        Map<String, List<Property>> sorted = new LinkedHashMap<>();

        if (sectionMap.containsKey("General"))
            sorted.put("General", sectionMap.get("General"));

        sectionMap.entrySet().stream()
                .filter(e -> !e.getKey().equals("General"))
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEachOrdered(e -> sorted.put(e.getKey(), e.getValue()));

        return sorted;
    }

    private Property createProperty(String key, String value, String category) {
        DefaultProperty prop = new DefaultProperty();
        prop.setName(key);
        prop.setDisplayName(key);
        prop.setShortDescription(""); // TODO get description from XML
        prop.setType(String.class);
        prop.setEditable(false);
        prop.setValue(value);

        prop.setCategory(category);

        return prop;
    }

    private void collapseAllCategories() {
        for (int i = 0; i < sheet.getTable().getSheetModel().getRowCount(); i++) {
            Item o = (Item) sheet.getTable().getSheetModel().getObject(i);
            if (o.isVisible() && !o.hasToggle()) {
                o.getParent().toggle();
            }
        }
    }

    private void expandAllCategories() {
        for (int i = 0; i < sheet.getTable().getSheetModel().getRowCount(); i++) {
            Item o = (Item) sheet.getTable().getSheetModel().getObject(i);
            if (!o.isVisible()) {
                if (o.hasToggle() && !o.isVisible()) {
                    o.toggle();
                } else if (!o.hasToggle() && o.getParent() != null && !o.getParent().isVisible()) {
                    o.getParent().toggle();
                }
            }
        }
    }
}

