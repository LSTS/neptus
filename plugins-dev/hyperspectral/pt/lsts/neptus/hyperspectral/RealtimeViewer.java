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
 * 18 May 2015
 */

package pt.lsts.neptus.hyperspectral;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import pt.lsts.imc.HyperSpecData;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

import com.google.common.eventbus.Subscribe;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class RealtimeViewer extends JPanel {
    private static int MIN_FREQ = 0;
    private static int MAX_FREQ = 740;
    
    private ConsoleLayout console;
    
    /* data visualization/display panel */
    private JSplitPane dataSplitPane;
    private JPanel fullSpectrumPanel; /* contains real-time images with all the frequencies requested by the user*/
    private JPanel selectedWavelengthPanel; /* contains real-time (stitched) images of a specific wavelength */
    private JLabel fullSpectrumDisplayer;
    private JLabel wavelengthDisplayer;
    
    /* control panel */
    private JSplitPane controlSplitPanel;
    private JPanel metadataPanel; /* metadata, etc*/
    private JPanel wavelengthSelectionPanel;
    private JTextField wavelengthField;
    private JLabel wavelengthUnits;
    private JButton sendRequest;
    
    /* testing */
    Queue<ImageIcon> frames;
    private double selectedWavelength;
    
    public RealtimeViewer(ConsoleLayout console) {
        super();
        this.console = console;
        setLayout(new BorderLayout());
        
        selectedWavelength = 0;
        
        setupControlPanels();
        setupDataDisplayPanels();
        
        /**** Just for testing ****/
        frames = loadFrames();        
        PeriodicUpdatesService.registerPojo(this);
    }
    
    
    /* where the actual data are displayed */
    private void setupDataDisplayPanels() {
        dataSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        dataSplitPane.setResizeWeight(0.5); /* split panels evenly */
        
        fullSpectrumPanel = new JPanel();
        selectedWavelengthPanel = new JPanel();
    
        add(dataSplitPane, BorderLayout.EAST);
        dataSplitPane.add(fullSpectrumPanel);
        dataSplitPane.add(selectedWavelengthPanel);
        
        fullSpectrumDisplayer = new JLabel();
        wavelengthDisplayer = new JLabel();
        
        fullSpectrumPanel.add(fullSpectrumDisplayer, BorderLayout.CENTER);
        selectedWavelengthPanel.add(wavelengthDisplayer, BorderLayout.CENTER);
    }
    
    private void setupControlPanels() {
        controlSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        controlSplitPanel.setResizeWeight(0.97);
        add(controlSplitPanel);
        
        int paneWidth = (int)(controlSplitPanel.getParent().getWidth() * 0.2);
        int paneHeight = (int)(controlSplitPanel.getParent().getHeight());
        controlSplitPanel.setPreferredSize(new Dimension(paneWidth, paneHeight));
        
        setupMetadataPanel();
        setupWavelengthSelectionPanel();
        
        controlSplitPanel.setTopComponent(metadataPanel);
        controlSplitPanel.setBottomComponent(wavelengthSelectionPanel);
    }
    
    private void setupMetadataPanel() {       
        metadataPanel = new JPanel();
   }
    
    private void setupWavelengthSelectionPanel() {
        wavelengthSelectionPanel = new JPanel();
        wavelengthSelectionPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        wavelengthField = new JTextField();
        wavelengthField.setColumns(20);
        wavelengthUnits = new JLabel("nm");
        sendRequest = new JButton("Send request");
        
        
        JComponent components[] = {wavelengthField, wavelengthUnits, sendRequest};
        
        for(int i = 0; i < components.length; i++) {
            c.weightx = 0.5;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = i;
            c.gridy = 0;
            
            wavelengthSelectionPanel.add(components[i], c);
        }
    }
        
    @Subscribe
    private void on(HyperSpecData msg) {
        /* in this case is it necessary? */
        String msgSrc = msg.getSourceName();       
        if(!msgSrc.equals(console.getMainSystem()))
                return;
        
        double minFreq = msg.getStartFreq();
        double maxFreq = msg.getEndFreq();
        byte[] frameBytes = msg.getData();
        
        ImageIcon frame = new ImageIcon(frameBytes);
        
        if(minFreq == MIN_FREQ && maxFreq == MAX_FREQ)
            fullSpectrumDisplayer.setIcon(frame);
        
        else if((minFreq == selectedWavelength) || (maxFreq == selectedWavelength))
            wavelengthDisplayer.setIcon(frame);
    }
    
    /***** Just for testing *****/
    
    private Queue<ImageIcon> loadFrames() {
        File dir = new File(System.getProperty("user.dir") + "/plugins-dev/hyperspectral/hypercap-sampledata/");
        File[] frames = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }            
        });
        
        LinkedList<ImageIcon> framesList = new LinkedList<>();
        for(int i = 0; i < frames.length; i++) {
            ImageIcon origFrame = new ImageIcon(frames[i].getAbsolutePath());
            
            int scaledWidth = (int)(0.80 * origFrame.getIconWidth());
            int scaledHeight = (int)(0.80 * origFrame.getIconHeight());
            
            ImageIcon scaledFrame = new ImageIcon(origFrame.getImage()
                    .getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_DEFAULT));
            
            framesList.add(scaledFrame);
        }
        
        fullSpectrumDisplayer.setIcon(framesList.get(30)); /* fixed image for top display */
        
        return framesList;
    }
    
    
    
    /* 2fps */
    @Periodic(millisBetweenUpdates = 500)
    public void validateCheck() {
        ImageIcon currentFrame = frames.poll();   
        
        wavelengthDisplayer.setIcon(currentFrame); /* display frame */
        
        frames.offer(currentFrame); /* add current frame to the end of the queue */
    }
}
