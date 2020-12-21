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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.jdesktop.swingx.JXBusyLabel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MarksKMLHandler;
import pt.lsts.neptus.util.csv.MarksCSVHandler;

/**
 * @author tsm
 */
@SuppressWarnings("serial")
public class MarksImporterPanel extends JPanel {
    public static String csvDelimiter = ",";

    private final int MAIN_WIDTH = 300;
    private final int MAIN_HEIGHT = 60;

    private final JRadioButton fromCsv = new JRadioButton(I18n.text("From CSV"));
    private final JRadioButton fromKml = new JRadioButton(I18n.text("From KML"));
    private final ButtonGroup group = new ButtonGroup();

    private final JButton kmlFromUrlBtn = new JButton(I18n.text("From URL"));
    private final JButton kmlFromFileBtn = new JButton(I18n.text("From file"));

    private final Color fgColor;
    
    private final JLabel sourceLabel = new JLabel("");
    private final JFileChooser fileChooser = new JFileChooser();

    private final JButton csvFromFileBtn = new JButton(I18n.text("Choose file"));

    private final JPanel importerSourcePanel = new JPanel();
    private final JPanel importerPanel = new JPanel();
    private final JPanel csvImporterPanel = new JPanel();
    private final JPanel kmlImporterPanel = new JPanel();
    private final JXBusyLabel busyLabel = InfiniteProgressPanel.createBusyAnimationInfiniteBeans(40);

    private final FileFilter csvFilter = GuiUtils.getCustomFileFilter(I18n.text("Comma Separated Values"), "csv");
    private final FileFilter kmlFilter = GuiUtils.getCustomFileFilter(I18n.text("KML"), "kml", "kmz");

    private Component parent = null;
    private List<MarkElement> importedMarks = null;

    public MarksImporterPanel() {
        this(null);
    }

    public MarksImporterPanel(Component parent) {
        this.parent = parent;
        
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        this.setBounds(0, 0, MAIN_WIDTH, MAIN_HEIGHT);
        this.setLayout(new MigLayout(""));

        initSourcesPanel();
        initImporterPanel();
        
        setWorking(false);
        
        add(importerSourcePanel, "w 20%, h 90%");
        add(importerPanel, "w 60%, h 90%");
        add(busyLabel, "w 40px, h 100%, wrap");
        add(sourceLabel, "w 20%, h 20%, spanx");
        
        fgColor = sourceLabel.getForeground();
    }

    private void setWorking(boolean working) {
        busyLabel.setBusy(working);
        busyLabel.setVisible(working);
    }
    
    /**
     * Null if something went wrong
     */
    public List<MarkElement> getImportedMarks() {
        return importedMarks;
    }

    private void initImporterPanel() {
        importerPanel.setBounds(0, 0, MAIN_WIDTH - 100, MAIN_HEIGHT);
        importerPanel.setLayout(new CardLayout());
        csvImporterPanel.setBounds(0, 0, MAIN_WIDTH - 100, MAIN_HEIGHT);
        kmlImporterPanel.setBounds(0, 0, MAIN_WIDTH - 100, MAIN_HEIGHT);

        //csv pane
        csvImporterPanel.setLayout(new MigLayout());
        csvFromFileBtn.addActionListener(e -> {
            sourceLabel.setText("");
            setWorking(true);
            try {
                fileChooser.resetChoosableFileFilters();
                fileChooser.setFileFilter(csvFilter);
                int res = fileChooser.showDialog(parent, I18n.text("CSV Source"));

                if(res == JFileChooser.APPROVE_OPTION) {
                    String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                    importedMarks = MarksCSVHandler.importCsv(filePath, csvDelimiter);
                    if(importedMarks == null) {
                        sourceLabel.setForeground(Color.RED);
                        sourceLabel.setText(I18n.text("Error while importing marks"));
                        return;
                    }

                    sourceLabel.setForeground(fgColor);
                    sourceLabel.setText(I18n.textf("Imported %numberOfMarks marks from '%path'", importedMarks.size(),
                            fileChooser.getSelectedFile().getName()));
                }
            }
            catch (Exception e1) {
                e1.printStackTrace();
                sourceLabel.setForeground(Color.RED);
                sourceLabel.setText(e1.toString());
            }
            finally {
                setWorking(false);
            }
        });

        kmlImporterPanel.setLayout(new MigLayout());
        kmlFromFileBtn.addActionListener(e -> {
            sourceLabel.setText("");
            setWorking(true);
            fileChooser.resetChoosableFileFilters();
            fileChooser.setFileFilter(kmlFilter);
            int res = fileChooser.showDialog(parent, I18n.text("KML Source"));
            if(res == JFileChooser.APPROVE_OPTION) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        doKmlImport(fileChooser.getSelectedFile().getAbsolutePath());
                        return null;
                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            sourceLabel.setForeground(Color.RED);
                            sourceLabel.setText(e.toString());
                        }
                        setWorking(false);
                    }
                };
                sw.execute();
            }
            else {
                setWorking(false);
            }
        });

        kmlFromUrlBtn.addActionListener(e -> {
            sourceLabel.setText("");
            setWorking(true);
            String url = JOptionPane.showInputDialog(MarksImporterPanel.this, "URL");
            if(url != null) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        doKmlImport(url);
                        return null;
                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            sourceLabel.setForeground(Color.RED);
                            sourceLabel.setText(e.toString());
                        }
                        setWorking(false);
                    }
                };
                sw.execute();
            }
            else {
                setWorking(false);
            }
        });

        csvImporterPanel.add(csvFromFileBtn);
        kmlImporterPanel.add(kmlFromFileBtn);
        kmlImporterPanel.add(kmlFromUrlBtn);

        importerPanel.add(csvImporterPanel, "csv");
        importerPanel.add(kmlImporterPanel, "kml");
        // csv by default
        ((CardLayout) importerPanel.getLayout()).show(importerPanel, "csv");
    }

    private void doKmlImport(String urlStr) {
        URL url;
        try {
            url = Paths.get(urlStr).toUri().toURL();
        }
        catch (MalformedURLException e1) {
            e1.printStackTrace();
            String msgStr = I18n.textf("KML %url is not a valid URL", urlStr);
            GuiUtils.showErrorPopup("KML", msgStr);
            sourceLabel.setForeground(Color.RED);
            sourceLabel.setText(msgStr);
            return;
        }
        catch (Exception e) {
            try {
                url = new URL(urlStr);
            }
            catch (MalformedURLException e1) {
                e1.printStackTrace();
                String msgStr = I18n.textf("KML %url is not a valid URL", urlStr);
                GuiUtils.showErrorPopup("KML", msgStr);
                sourceLabel.setForeground(Color.RED);
                sourceLabel.setText(msgStr);
                return;
            }
        }

        importedMarks = MarksKMLHandler.importKML(url);
        if (importedMarks == null) {
            sourceLabel.setForeground(Color.RED);
            sourceLabel.setText(I18n.text("Error while importing marks"));
            return;
        }

        sourceLabel.setForeground(fgColor);
        sourceLabel.setText(I18n.textf("Imported %numberOfMarks marks from '%path'", importedMarks.size(), urlStr));
    }

    private void initSourcesPanel() {
        importerSourcePanel.setBounds(0, 0, MAIN_WIDTH / 5, MAIN_HEIGHT);
        group.add(fromCsv);
        group.add(fromKml);

        fromCsv.setSelected(true);

        ActionListener radioBtnListener = e -> {
            if(fromCsv.isSelected()) {
                ((CardLayout) importerPanel.getLayout()).show(importerPanel, "csv");

                fileChooser.resetChoosableFileFilters();;
                fileChooser.setFileFilter(csvFilter);
            }
            else if(fromKml.isSelected()){
                ((CardLayout) importerPanel.getLayout()).show(importerPanel, "kml");

                fileChooser.resetChoosableFileFilters();;
                fileChooser.setFileFilter(kmlFilter);
            }
        };

        fromCsv.addActionListener(radioBtnListener);
        fromKml.addActionListener(radioBtnListener);

        importerSourcePanel.setLayout(new MigLayout("wrap 1"));
        importerSourcePanel.add(fromCsv);
        importerSourcePanel.add(fromKml);
    }

    public static List<MarkElement> showPanel(Component parent) {
        MarksImporterPanel panel = new MarksImporterPanel(parent);
        JOptionPane.showOptionDialog(parent, panel, I18n.text("Marks Importer"),
                JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{I18n.text("Close")}, null);

        return panel.getImportedMarks();
    }
}
