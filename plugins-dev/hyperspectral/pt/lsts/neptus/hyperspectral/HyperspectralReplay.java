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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.vecmath.Point2d;

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

/**
 * @author tsmarques
 *
 */
@LayerPriority(priority=-10)
@PluginDescription(icon="pt/lsts/neptus/mra/replay/globe.png")
public class HyperspectralReplay implements LogReplayLayer {
    /* frames sent from a higher or lower altitude will be drawn on the map scaled up or down, respectively */
    private static final int DEFAULT_ALTITUDE = 100; /* in meters */
    
    private final List<HyperspectralData> dataset = new ArrayList<HyperspectralData>();

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

        for(int i = 0; i < dataset.size(); i++) {
            HyperspectralData frame = dataset.get(i);
            Point2D dataPosition = renderer.getScreenPosition(frame.dataLocation);

            /* draw data with its center in the EstimatedState position */
            int dataX = (int) dataPosition.getX()- (frame.data.getWidth() / 2);
            int dataY = (int) dataPosition.getY() - (frame.data.getHeight() / 2);

            AffineTransform backup = g.getTransform();
            AffineTransform tx = new AffineTransform();
            tx.rotate(frame.rotationAngle, dataPosition.getX(), dataPosition.getY());

            g.setTransform(tx);
            g.drawImage(frame.data, dataX, dataY, null, renderer);
            g.setTransform(backup);
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        //return source.getLog("HyperSpecData") != null;
        return true;
    }


    @Override
    public void parse(IMraLogGroup source) {
        //IMraLog hyperspecLog = source.getLog("hyperspecData");
        Queue<byte[]> frames = HyperspecUtils.loadFrames("320/");
        IMraLog esLog = source.getLog("EstimatedState");
        EstimatedState state = (EstimatedState) esLog.firstLogEntry();

        while(state != null && !frames.isEmpty()) {
            HyperspectralData newData = new HyperspectralData(frames.poll(), state);
            dataset.add(newData);

            state = (EstimatedState) esLog.nextLogEntry();
        }
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
            try {
                data = ImageIO.read(new ByteArrayInputStream(dataBytes));
                dataLocation = IMCUtils.parseLocation(state);
         
                /* scale image down */
                tx = new AffineTransform();
                tx.scale(0.5, 0.5);
                op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
                data = op.filter(data, null);
                
                rotationAngle = setRotationAngle(state.getPsi());
            }
            catch (IOException e) { e.printStackTrace(); }
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
