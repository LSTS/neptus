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
 * 30 Jun 2015
 */
package pt.lsts.neptus.hyperspectral;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.HyperSpecData;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author tsmarques
 *
 */
@LayerPriority(priority=-10)
@PluginDescription(icon="pt/lsts/neptus/mra/replay/globe.png")
public class HyperspectralReplay extends JFrame implements LogReplayLayer {
    /* frames sent from a higher or lower altitude will be drawn on the map scaled up or down, respectively */
    private static final double DEFAULT_ALTITUDE = 100; /* in meters */
    
    /* wavelength selection panel */
    private JPanel mainPanel;
    private JComboBox<Double> wavelengths;
    private JButton selectButton;
    
    private boolean firstPaint = true;
    private boolean dataParsed = false;
    public double selectedWavelength = -1;
    
    private final OnPathLayer dataLayer = new OnPathLayer();
    private boolean layerGenerated = false;
    /* used to compute dataLayer's size */
    private double zoom;

    public HyperspectralReplay() {
        super();
        initWavelenSelectionPanel();
    }
    
    private void initWavelenSelectionPanel() {
        this.setSize(new Dimension(300, 70));
        this.setTitle("Select wavelength");
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setPreferredSize(this.getPreferredSize());
        
        wavelengths = new JComboBox<Double>();
        wavelengths.setSize(new Dimension(100, 40));
        ((JLabel)wavelengths.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        
        selectButton = new JButton("Show data");
        selectButton.setSize(new Dimension(100, 40));
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               if(!dataParsed)
                   return;
               
               Object selection = (double) wavelengths.getSelectedItem();
               
               if(selection != null) {
                   layerGenerated = false;
                   selectedWavelength = (double) selection;
//                   synchronized(dataset) {
//                       currentData = dataset.get(selectedWavelength);
//                   }
               }
            }
        });
        
        this.add(mainPanel);
        mainPanel.add(wavelengths, BorderLayout.NORTH);
        mainPanel.add(selectButton, BorderLayout.SOUTH);
        this.setLocationRelativeTo(null);
    }

    @Override
    public String getName() {
        return I18n.text("Hyperspectral Replay");
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(firstPaint) {
            this.setVisible(true);
            firstPaint = false;
            zoom = renderer.getZoom();
           
            return;
        }
       
        if(dataParsed) {
            if(selectedWavelength == -1)
                return;
            
            if(layerGenerated == false) {
                System.out.println("GENERATED LAYER");              
                
                dataLayer.generateLayer(selectedWavelength, renderer);               
                layerGenerated = true;
            }
            
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            double currZoom = renderer.getZoom();
            BufferedImage layerToDisplay;
            
            if(zoom != currZoom) {
                double scale = currZoom / zoom;
                layerToDisplay = HyperspecUtils.getScaledImage(dataLayer.getLayer(), scale, scale);
            }
            else
                layerToDisplay = dataLayer.getLayer();


            Point2D center = renderer.getScreenPosition(dataLayer.getCenter());
                        
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.translate(center.getX(), center.getY());
            g.drawImage(layerToDisplay, (int)(-layerToDisplay.getWidth()/2), (int)(-layerToDisplay.getHeight()/2), null, renderer);
            g.translate(-center.getX(), -center.getY());
            g.dispose();

            renderer.repaint();
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("HyperSpecData") != null;
    }


    @Override
    public void parse(IMraLogGroup source) {
        Thread t = new Thread(HyperspectralReplay.class.getSimpleName() + " " + source.getDir().getParent()) {
            
            @Override
            public void run() {
                IMraLog hyperspecLog = source.getLog("HyperSpecData");
                IMraLog esLog = source.getLog("EstimatedState");

                HyperSpecData msg = (HyperSpecData) hyperspecLog.firstLogEntry();
                EstimatedState previousState = null;
                
                LocationType loc = new LocationType();
                LocationType tempLoc;
                                
                int count = 0;
                while(count < 2000)  {
                    EstimatedState closestState = (EstimatedState)esLog.getEntryAtOrAfter(msg.getTimestampMillis());                   
                    double dataWavelen = msg.getWavelen();                    
                    
                    boolean overlapped = isDataOverlapped(previousState, closestState);   
                    dataLayer.addData(dataWavelen, new HyperspectralData(msg, closestState, overlapped));
                    
                    /* check if combobox already contains this wavelength, if not, add it */
                    if(((DefaultComboBoxModel<Double>)wavelengths.getModel()).getIndexOf(dataWavelen) == -1 )
                        wavelengths.addItem(dataWavelen);
                   
                    /* compute OnPathLayer area */
                    loc.setLatitudeDegs(Math.toDegrees(closestState.getDouble("lat")));
                    loc.setLongitudeDegs(Math.toDegrees(closestState.getDouble("lon")));
                    loc.setOffsetNorth(closestState.getDouble("x"));
                    loc.setOffsetEast(closestState.getDouble("y"));
                    tempLoc = loc.getNewAbsoluteLatLonDepth();
                    
                    dataLayer.updateMinMaxLocations(dataWavelen, tempLoc);

                    
                    msg = (HyperSpecData) hyperspecLog.nextLogEntry();
                    previousState = closestState;
                    
                    count++;
                }
            }
        };
        t.setDaemon(true);
        t.start();
        
        if(t.isAlive())
            dataParsed = true;
    }
    
    private boolean isDataOverlapped(EstimatedState previousState, EstimatedState currState) {
        /* means that currState is the first state to be received, so no overlap is possible at this time */
        if(previousState == null)
            return false;
        
        return previousState.getTimestamp() == currState.getTimestamp();
    }


    @Override
    public String[] getObservedMessages() {
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {

    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void cleanup() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
