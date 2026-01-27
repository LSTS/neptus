/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * November 25, 2025
 */
package pt.lsts.neptus.mra.plots;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTable;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;
import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
@PluginDescription(author = "jcordeiro", name = "Vehicle Configuration", icon="images/settings2.png")
public class ConfigIniPlot extends SimpleMRAVisualization {

    private File iniFile;
    private final Map<String, List<Property>> sectionMap = new LinkedHashMap<>();
    PropertySheetPanel sheet = new PropertySheetPanel();
    private JPopupMenu currentlyOpenDropdown = null;

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

        SwingUtilities.invokeLater(() -> {
            collapseAllCategories();
            installDropdownOnTable(sheet);
        });

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

    private void installDropdownOnTable(PropertySheetPanel sheet) {
        PropertySheetTable table = (PropertySheetTable) sheet.getTable();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // close if already open
                if (currentlyOpenDropdown != null) {
                    currentlyOpenDropdown.setVisible(false);
                    currentlyOpenDropdown = null;
                    return;
                }

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row < 0 || col != 1) {
                    return;
                }

                Object o = table.getSheetModel().getObject(row);
                if (!(o instanceof Item)) {
                    return;
                }

                Item item = (Item) o;

                // ignore if category
                if (item.hasToggle()) {
                    return;
                }

                Property property = item.getProperty();
                if (property == null) {
                    return;
                }

                Object val = property.getValue();
                if (val == null) {
                    return;
                }

                String full = val.toString().trim();
                if (full.isEmpty()) {
                    return;
                }

                // show dropdown only if the content is long-ish
                FontMetrics fm = table.getFontMetrics(table.getFont());
                int colWidth = table.getColumnModel().getColumn(col).getWidth();
                int textWidth = SwingUtilities.computeStringWidth(fm, full);

                if (textWidth <= colWidth - 10) {
                    return;
                }

                // split by comma and trim, each value per line
                String[] values = full.split("\\s*,\\s*");

                // build multiline text
                StringBuilder multiline = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    multiline.append(values[i]);
                    if (i < values.length - 1)
                        multiline.append("\n");
                }
                String text = multiline.toString();

                // create popup
                JPopupMenu dropdown = new JPopupMenu();
                dropdown.setLayout(new BorderLayout());

                // selectable text area
                JTextArea textArea = new JTextArea(text);
                textArea.setEditable(false);
                textArea.setLineWrap(false);
                textArea.setWrapStyleWord(false);
                textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                textArea.setBackground(UIManager.getColor("Panel.background"));
                textArea.setFont(table.getFont());
                textArea.setCaretPosition(0);

                // select whole line with one click
                textArea.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent me) {
                        int pos = textArea.viewToModel2D(me.getPoint());
                        if (pos >= 0) {
                            try {
                                int line = textArea.getLineOfOffset(pos);
                                int start = textArea.getLineStartOffset(line);
                                int end = textArea.getLineEndOffset(line);
                                // trim trailing newline from selection
                                if (end > start && textArea.getText(end - 1, 1).equals("\n")) {
                                    end = end - 1;
                                }
                                textArea.requestFocusInWindow();
                                textArea.select(start, end);
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    }
                });

                JScrollPane scroll = new JScrollPane(textArea);
                scroll.setBorder(null);
                scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                int maxWidth = 200;
                for (String v : values) {
                    maxWidth = Math.max(maxWidth, SwingUtilities.computeStringWidth(fm, v) + 40);
                }
                maxWidth = Math.min(800, maxWidth);

                configureScrollSize(scroll, fm, values.length, maxWidth);

                dropdown.add(scroll, BorderLayout.CENTER);

                positionDropdown(dropdown, table, row, col);
                showDropdown(dropdown, table);

                currentlyOpenDropdown = dropdown;
            }
        });
    }

    private void configureScrollSize(JScrollPane scroll, FontMetrics fm, int lineCount, int maxWidth) {
        int lineHeight = fm.getHeight();
        int visibleLines = Math.min(lineCount, 6);
        int prefHeight = visibleLines * lineHeight + 10;
        scroll.setPreferredSize(new Dimension(maxWidth, prefHeight));
    }

    private void positionDropdown(JPopupMenu dropdown, JTable table, int row, int col) {
        Rectangle rect = table.getCellRect(row, col, true);
        Point p = new Point(rect.x, rect.y + rect.height);
        SwingUtilities.convertPointToScreen(p, table);

        Window parentWindow = SwingUtilities.getWindowAncestor(table);
        if (parentWindow != null) {
            Rectangle parentBounds = parentWindow.getBounds();
            Dimension dropdownSize = dropdown.getPreferredSize();

            if (p.y + dropdownSize.height > parentBounds.y + parentBounds.height) {
                p.y = parentBounds.y + parentBounds.height - dropdownSize.height;
            }

            if (p.x + dropdownSize.width > parentBounds.x + parentBounds.width) {
                p.x = parentBounds.x + parentBounds.width - dropdownSize.width;
            }

            p.y = Math.max(p.y, parentBounds.y);
            p.x = Math.max(p.x, parentBounds.x);
        }

        dropdown.setLocation(p);
    }

    private void showDropdown(JPopupMenu dropdown, JTable table) {
        dropdown.setInvoker(table);
        dropdown.setSize(dropdown.getPreferredSize());
        dropdown.setVisible(true);
    }
}

