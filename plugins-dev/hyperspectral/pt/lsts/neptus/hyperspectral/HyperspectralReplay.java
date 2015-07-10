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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.HyperSpecData;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.IMCUtils;
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
    public double selectedWavelength = 0;
    private final HashMap<Double, List<HyperspectralData>> dataset = new HashMap<>();
    List<HyperspectralData> dataList;

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
                   selectedWavelength = (double) selection;
                   dataList = dataset.get(selectedWavelength);
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
        }
        
        if(dataset.isEmpty())
            return;

        if(dataParsed && (dataList != null)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            /* draw data along the vehicle's path */
            for(int i = 0; i < 5000; i++) {
                HyperspectralData frame = dataList.get(i);
                Point2D dataPosition = renderer.getScreenPosition(frame.dataLocation);

                BufferedImage scaledData = frame.getScaledData(1, renderer.getZoom());

                /* draw data with its center in the EstimatedState position */
                int dataX = (int) dataPosition.getX()- (scaledData.getWidth() / 2);
                int dataY = (int) dataPosition.getY() - (scaledData.getHeight() / 2);


//                AffineTransform backup = g.getTransform();
//                AffineTransform tx = new AffineTransform();
//                tx.rotate(frame.rotationAngle, dataPosition.getX(), dataPosition.getY());
//
//                g.setTransform(tx);
                g.drawImage(scaledData, dataX, dataY, null, renderer);
//                g.setTransform(backup);
            }
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
                while(msg != null)  {
                    EstimatedState closestState = (EstimatedState)esLog.getEntryAtOrAfter(msg.getTimestampMillis());
                    double dataWavelen = msg.getWavelen();
                    
                    List<HyperspectralData> dataList;
                    if(dataset.containsKey(dataWavelen))
                        dataList = dataset.get(dataWavelen);
                    else {
                        dataList = new LinkedList<>();
                        dataset.put(dataWavelen, dataList);
                        /* add to combo box */
                        wavelengths.addItem(dataWavelen);
                    }
                    
                    boolean overlapped = isDataOverlapped(previousState, closestState);
                    
                    dataList.add(new HyperspectralData(msg, closestState, overlapped));
                    msg = (HyperSpecData) hyperspecLog.nextLogEntry();
                    previousState = closestState;
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

    private class HyperspectralData {
        private double rotationAngle; 
        public BufferedImage data;
        public LocationType dataLocation;

        public HyperspectralData(HyperSpecData msg, EstimatedState state, boolean overlapped) {
            data = HyperspecUtils.rawToBuffImage(msg.getData());
            
            dataLocation = IMCUtils.parseLocation(state);
            
            if(overlapped)
                translateDataPosition(msg, state);
            
            data = getScaledData(1, 0.25);
            
            rotationAngle = setRotationAngle(state.getPsi());
            data = rotateData();
        }
        
        /* 
           If some data is overlapped with another over an
           Estimated State point, make an estimate of its position
           using the vehicle's speed and difference between timestamps 
           i.e., calculate how much the vehicle moved since the last 
           EstimatedState, and draw it there, instead of in the position
           given by getEntryAtOrAfter()
         */
        private void translateDataPosition(HyperSpecData data, EstimatedState state) {
            double deltaTime = data.getTimestamp() - state.getTimestamp();
            double speedX = state.getVx();
            double speedY = state.getVy();

            double deltaX = speedX * deltaTime;
            double deltaY = speedY * deltaTime;

            dataLocation.setOffsetNorth(deltaX);
            dataLocation.setOffsetEast(deltaY);
        }
        
        private BufferedImage rotateData() {
            double sin = Math.abs(Math.sin(rotationAngle));
            double cos = Math.abs(Math.cos(rotationAngle));
            int w = data.getWidth();
            double h = data.getHeight();
            
            int rw =(int) Math.floor(cos * w + sin * h);
            int rh = (int) Math.floor(cos * h + sin * w);
            
            BufferedImage rotatedImage = new BufferedImage((int)rw, (int)rh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) rotatedImage.getGraphics();
            
            g.translate((rw-w)/2, (rh-h)/2);
            g.rotate(rotationAngle, w/2, h/2);
            g.drawRenderedImage(data, null);
            g.dispose();
            
            return rotatedImage;
        }
        
        private BufferedImage getScaledData(double scalex, double scaley) {
            return (BufferedImage)ImageUtils.getFasterScaledInstance(data, 
                    (int)(data.getWidth() * scalex), (int)(scaley * data.getHeight()));
        }

        /* Get angle so that the frame is perpendicular to the vehicle's heading */
        private double setRotationAngle(double psi) {
            double angle;

            psi = (Math.toDegrees(psi)) - 90; /* -90 to make angle perpendicular */
            if(psi < 0)
                angle = 360 + psi;
            else
                angle = psi;

            return Math.toRadians(angle);
        }
    }
}
