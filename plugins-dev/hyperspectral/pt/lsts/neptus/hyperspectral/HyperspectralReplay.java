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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.vecmath.Point2d;

import opendap.servlet.GetHTMLInterfaceHandler;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.HyperSpecData;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.mra.replay.MultibeamReplay;
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
public class HyperspectralReplay implements LogReplayLayer {
    /* frames sent from a higher or lower altitude will be drawn on the map scaled up or down, respectively */
    private static final double DEFAULT_ALTITUDE = 100; /* in meters */
    
    public double selectedWavelength = 0;
    private final HashMap<Double, List<HyperspectralData>> dataset = new HashMap<>();

    public HyperspectralReplay() {

    }

    @Override
    public String getName() {
        return I18n.text("Hyperspectral Replay");
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(dataset.isEmpty())
            return;
        
        if(dataset.containsKey(selectedWavelength)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for(int i = 0; i < dataset.size(); i++) {
                HyperspectralData frame = dataset.get(selectedWavelength).get(i);
                Point2D dataPosition = renderer.getScreenPosition(frame.dataLocation);

                BufferedImage scaledData = frame.getScaledData(1, renderer.getZoom());

                /* draw data with its center in the EstimatedState position */
                int dataX = (int) dataPosition.getX()- (scaledData.getWidth() / 2);
                int dataY = (int) dataPosition.getY() - (scaledData.getHeight() / 2);

                AffineTransform backup = g.getTransform();
                AffineTransform tx = new AffineTransform();
                tx.rotate(frame.rotationAngle, dataPosition.getX(), dataPosition.getY());

                g.setTransform(tx);
                g.drawImage(scaledData, dataX, dataY, null, renderer);
                g.setTransform(backup);
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
                while(msg != null)  {
                    EstimatedState closestState = (EstimatedState)esLog.getEntryAtOrAfter(msg.getTimestampMillis());
                    double dataWavelen = msg.getWavelen();
                    
                    List<HyperspectralData> dataList;
                    if(dataset.containsKey(dataWavelen))
                        dataList = dataset.get(dataWavelen);
                    else {
                        dataList = new LinkedList<>();
                        dataset.put(dataWavelen, dataList);
                    }
                    
                    dataList.add(new HyperspectralData(msg.getData(), closestState));
                    msg = (HyperSpecData) hyperspecLog.nextLogEntry();
                }
            }
        };
        t.setDaemon(true);
        t.start();
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

    }

    private class HyperspectralData {
        private double rotationAngle; 
        public BufferedImage data;
        public LocationType dataLocation;

        private AffineTransform tx;
        private AffineTransformOp op;

        public HyperspectralData(byte[] dataBytes, EstimatedState state) {
            data = HyperspecUtils.rawToBuffImage(dataBytes);
            dataLocation = IMCUtils.parseLocation(state);
            data = getScaledData(1, 0.25);
            

            rotationAngle = setRotationAngle(state.getPsi());
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
