/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 5, 2012
 * $Id:: RevisionSidePanel.java 9955 2013-02-19 19:37:04Z jqcorreia             $:
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

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

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndexListener;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.replay.BathymetryReplay;
import pt.up.fe.dceg.neptus.mra.replay.EstimatedStateReplay;
import pt.up.fe.dceg.neptus.mra.replay.GPSFixReplay;
import pt.up.fe.dceg.neptus.mra.replay.LBLRangesReplay;
import pt.up.fe.dceg.neptus.mra.replay.LogReplayLayer;
import pt.up.fe.dceg.neptus.mra.replay.LogMarkersReplay;
import pt.up.fe.dceg.neptus.plugins.sss.SidescanOverlay;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.llf.LsfLogSource;

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
            fileLabel = new JLabel("(no log loaded)");
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
            LogReplayLayer[] layers = new LogReplayLayer[] {
                    new EstimatedStateReplay(),
                    new GPSFixReplay(),
                    new LBLRangesReplay(),
                    new LogMarkersReplay(),
                    new BathymetryReplay(),
                    new SidescanOverlay()
            };
           
            for (LogReplayLayer layer : layers) {                         
                if (layer.canBeApplied(source)) {
                    layer.parse(source);
                    addOverlay(layer.getName(), layer);
                }
            }
        }

        public void selectFile() {
            final JFileChooser chooser = new JFileChooser(overlays.defaultDirectory);
            chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("LSF log files"), new String[] {"lsf", "lsf.gz"}));
            chooser.setApproveButtonText("Open Log");
            
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
