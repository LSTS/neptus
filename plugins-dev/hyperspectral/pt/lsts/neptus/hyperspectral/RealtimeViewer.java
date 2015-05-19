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
import java.awt.Image;
import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

import com.google.common.eventbus.Subscribe;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class RealtimeViewer extends JPanel {
    private JSplitPane dataSplitPane;
    private JPanel fullSpectrumPanel; /* contains real-time images with all the frequencies requested by the user*/
    private JPanel selectedWavelengthPanel; /* contains real-time (stitched) images of a specific wavelength */
    private JLabel fullSpectrumDisplayer;
    private JLabel wavelengthDisplayer;
    
    private JPanel metadataPanel; /* metadata, etc*/
    
    /* testing */
    Queue<ImageIcon> frames;
    
    public RealtimeViewer() {
        super();
        setLayout(new BorderLayout());
        
        setupMetadataPanel();
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
    
    /* metadata, etc */
    private void setupMetadataPanel() {       
        metadataPanel = new JPanel();
        add(metadataPanel);
        
        int paneWidth = (int)(metadataPanel.getParent().getWidth() * 0.2);
        int paneHeight = (int)(metadataPanel.getParent().getHeight());
        metadataPanel.setPreferredSize(new Dimension(paneWidth, paneHeight));
    }
        
//    @Subscribe
//    private void on(HyperSpecData msg) {
//        
//    }
    
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
