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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Dec 5, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import pt.lsts.imc.lsf.LsfIndexListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayComponent.Context;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.renderer2d.ImageLayer;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author zp
 *
 */
public class RevisionSidePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel fileLabel;
    private JButton fileSelection;
    private RevisionOverlays overlays;
    private JPanel filePanel = new JPanel(new BorderLayout());
    private JPanel overlaysPanel;
    private Vector<Renderer2DPainter> painters = new Vector<>();    
    private IMraLogGroup logSource = null;
    private File logFile = null;

    public void clearOverlays() {
        for (Renderer2DPainter painter : painters)
            overlays.renderer.removePostRenderPainter(painter);
        painters.clear();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                overlaysPanel.removeAll();
                overlaysPanel.revalidate();                
            }
        });
    }

    public void addOverlay(final String name, final Renderer2DPainter painter) {
        painters.add(painter);
        final JCheckBox check = new JCheckBox(name);
        check.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (check.isSelected())
                    overlays.renderer.addPreRenderPainter(painter);
                else
                    overlays.renderer.removePreRenderPainter(painter);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                overlaysPanel.add(check);
                overlaysPanel.revalidate();
            }});
        }

        public RevisionSidePanel(RevisionOverlays overlays) {
            this.overlays = overlays;
            initialize();
        }

        private void initialize() {
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            fileLabel = new JLabel(I18n.text("(no log loaded)"));
            fileLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            fileLabel.setBackground(Color.white);
            fileLabel.setOpaque(true);
            filePanel.add(fileLabel, BorderLayout.CENTER);
            fileSelection = new JButton("...");
            fileSelection.setMargin(new Insets(1, 1, 1, 1));
            fileSelection.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    selectFile();
                }
            } );
            filePanel.add(fileSelection, BorderLayout.EAST);
            filePanel.setMaximumSize(new Dimension(200, 25));
            add(filePanel);
            overlaysPanel = new JPanel();
            overlaysPanel.setLayout(new BoxLayout(overlaysPanel, BoxLayout.PAGE_AXIS));
            add(new JScrollPane(overlaysPanel));
        }

        private void loadOverlays(LsfLogSource source) {
            
            Vector<LogReplayLayer> layers = new Vector<>();
            
            for (String name : PluginsRepository.getReplayLayers().keySet()) {
                layers.add(PluginsRepository.getPlugin(name, LogReplayLayer.class));
            }
            
            for (LogReplayLayer layer : layers) {                         
                if (layer.canBeApplied(source, Context.Console)) {
                    layer.parse(source);
                    addOverlay(layer.getName(), layer);
                }
            }
            
            File dir = source.getFile("mra");
            File[] files = dir.listFiles();
            for (File f : files) {
                if (FileUtil.getFileExtension(f).equals("layer")) {
                    try {
                        addOverlay(f.getName(), ImageLayer.read(f));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void selectFile() {
            final JFileChooser chooser = GuiUtils.getFileChooser(overlays.defaultDirectory, I18n.text("LSF log files"), 
                    FileUtil.FILE_TYPE_LSF, FileUtil.FILE_TYPE_LSF_COMPRESSED, FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2);
            chooser.setApproveButtonText(I18n.text("Open Log"));
            
            int option = chooser.showOpenDialog(overlays.getConsole());
            if (option != JFileChooser.APPROVE_OPTION)
                return;
            else {
                Thread logLoading = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        final ProgressMonitor monitor = new ProgressMonitor(overlays.getConsole(), I18n.text("Opening LSF log"), I18n.text("Opening LSF log"), 0, 100);
                        try {
                            clearOverlays();
                            fileLabel.setText("loading...");
                            logFile = chooser.getSelectedFile();
                            LsfLogSource source = new LsfLogSource(chooser.getSelectedFile(), new LsfIndexListener() {                            
                                @Override
                                public void updateStatus(String messageToDisplay) {                                
                                    monitor.setNote(messageToDisplay);
                                }
                            });
                            logSource = source;
                            loadOverlays(source);                        
                        }
                        catch  (Exception e) {
                            GuiUtils.errorMessage(overlays.getConsole(), e);
                        }
                        SwingUtilities.invokeLater(new Runnable() {                        
                            @Override
                            public void run() {                            
                                fileLabel.setText(chooser.getSelectedFile().getParentFile().getName());                            
                            }                        
                        });
                        monitor.close();
                        overlays.defaultDirectory = chooser.getSelectedFile().getParent();   

                    }
                });
                logLoading.start();
            }
        }

        /**
         * @return the logFile
         */
        public File getLogFile() {
            return logFile;
        }

        /**
         * @return the logSource
         */
        public IMraLogGroup getLogSource() {
            return logSource;
        }
    }
