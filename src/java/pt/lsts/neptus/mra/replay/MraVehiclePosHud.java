/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Dec 4, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author zp
 */
public class MraVehiclePosHud {

    protected LsfIndex index;
    protected Vector<SystemPositionAndAttitude> states = new Vector<>();
    protected double startTime, endTime;
    protected double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE,
            maxY = -Double.MAX_VALUE;
    protected LocationType ref = null;

    protected BufferedImage map = null;
    protected BufferedImage img = null;
    protected int currentPosition = -1, width, height;

    protected Color pathColor = Color.black;
    protected IMraLogGroup source;
    
    public MraVehiclePosHud(IMraLogGroup source, int width, int height) {
        this.source = source;
        this.index = source.getLsfIndex();
        this.width = width;
        this.height = height;
        loadIndex();
    }
    
    public void correctPositions() {
        CorrectedPosition cp = CorrectedPosition.getInstance(source);
        for (SystemPositionAndAttitude state : states) {
            double timestamp = state.getTime() / 1000.0;
            SystemPositionAndAttitude corPos = cp.getPosition(timestamp);
            state.setPosition(new LocationType(corPos.getPosition()));
        }
    }

    protected void loadIndex() {
        startTime = Math.ceil(index.getStartTime());
        endTime = Math.floor(index.getEndTime());
        int msgType = index.getDefinitions().getMessageId("EstimatedState");
        int lastIndex = 0;

        int indexFirtsMsg = index.getFirstMessageOfType(msgType);
        if (indexFirtsMsg < 0) {
            return;
        }

        for (double time = startTime; time < endTime; time++) {
            int i = index.getMessageAtOrAfer(msgType, 0xFF, lastIndex, time);
            if (i != -1) {
                IMCMessage estimatedState = index.getMessage(i);
                LocationType loc = new LocationType(Math.toDegrees(estimatedState.getDouble("lat")),
                        Math.toDegrees(estimatedState.getDouble("lon")));
                loc.setDepth(estimatedState.getDouble("depth"));
                loc.translatePosition(estimatedState.getDouble("x"), estimatedState.getDouble("y"),
                        estimatedState.getDouble("z"));

                if (ref == null) {
                    ref = new LocationType(loc);
                    loc.convertToAbsoluteLatLonDepth();
                }

                SystemPositionAndAttitude state = new SystemPositionAndAttitude(loc,
                        estimatedState.getDouble("phi"), estimatedState.getDouble("theta"),
                        estimatedState.getDouble("psi"));
                state.setTime(estimatedState.getTimestampMillis());
                states.add(state);
                lastIndex = i;
            }
            else
                states.add(null);
        }
    }
    
    public BufferedImage getImage(double startTimestamp, double endTimestamp, double timestep) {
        double desiredWidth = maxX - minX;
        double desiredHeight = maxY - minY;
        double zoom = (width-20) / desiredWidth;
        zoom = Math.min(zoom, (height-20) / desiredHeight);
        if (map == null) {
            map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            createMap();
        }
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2 = img.createGraphics();
        g2.drawImage(map, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (double timestamp = startTimestamp; timestamp < endTimestamp; timestamp += timestep) {
            Graphics2D g = (Graphics2D)g2.create();
            setTimestamp(timestamp);
            SystemPositionAndAttitude state = states.get(currentPosition);
        
            
            g.scale(zoom, zoom);
            g.translate(-minX+10, -minY+10);
            double offsets[] = state.getPosition().getOffsetFrom(ref);
            g.setColor(Color.red);
            Ellipse2D.Double tmp = new Ellipse2D.Double(offsets[1]-3/zoom, -offsets[0]-3/zoom, 6/zoom, 6/zoom); 
            g.fill(tmp);            
        }
        return img;

    }

    public BufferedImage getImage(double timestamp) {
        double desiredWidth = maxX - minX;
        double desiredHeight = maxY - minY;
        double zoom = (width-20) / desiredWidth;
        zoom = Math.min(zoom, (height-20) / desiredHeight);
        setTimestamp(timestamp);
        SystemPositionAndAttitude state = states.get(currentPosition);
        
        if (map == null) {
            map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            createMap();
        }
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = img.createGraphics();
        g.drawImage(map, 0, 0, null);

       
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(zoom, zoom);
        g.translate(-minX+10, -minY+10);
        double offsets[] = state.getPosition().getOffsetFrom(ref);
        g.setColor(Color.red);
        Ellipse2D.Double tmp = new Ellipse2D.Double(offsets[1]-3/zoom, -offsets[0]-3/zoom, 6/zoom, 6/zoom); 
        g.fill(tmp);
        g.setColor(Color.black);
        g.draw(tmp);
        
        return img;
    }

    protected void createMap() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);

        for (int i = 0; i < states.size(); i++) {
            SystemPositionAndAttitude state = states.get(i);
            if (state == null)
                continue;

            LocationType loc = state.getPosition();

            double[] offsets = loc.getOffsetFrom(ref);
            double x = offsets[1];
            double y = -offsets[0];
            maxX = Math.max(x, maxX);
            maxY = Math.max(y, maxY);
            minX = Math.min(x, minX);
            minY = Math.min(y, minY);
            path.lineTo(x, y);
        }

        double desiredWidth = maxX - minX;
        double desiredHeight = maxY - minY;

        double zoom = (width-20) / desiredWidth;
        zoom = Math.min(zoom, (height-20) / desiredHeight);

        Graphics2D g = map.createGraphics();//offscreen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.scale(zoom, zoom);
        g.translate(-minX+10, -minY+10);
        
        g.setColor(new Color(pathColor.getRed(),pathColor.getGreen(),pathColor.getBlue(),64));
        g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(pathColor.getRed(),pathColor.getGreen(),pathColor.getBlue(),200));
        g.draw(path);        
    }

    public void setTimestamp(double timeSecs) {
        if (timeSecs <= startTime)
            currentPosition = 0;
        else if (timeSecs >= endTime)
            currentPosition = states.size() - 1;
        else {
            currentPosition = (int) (timeSecs - startTime);
        }
    }

    public double getTimestamp() {
        return currentPosition + startTime;
    }

    /**
     * @return the startTime
     */
    public final double getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public final void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the endTime
     */
    public final double getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public final void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    /**
     * @param pathColor the pathColor to set
     */
    public final void setPathColor(Color pathColor) {
        if(this.pathColor.getRGB() != pathColor.getRGB()) {
            this.pathColor = pathColor;
            map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            createMap();
        }
    }

    public static void main(String[] args) throws Exception {

        //LsfIndex index = new LsfIndex(new File("/home/zp/Desktop/logs/shore_line/Data.lsf"));
        IMraLogGroup source = new LsfLogSource(new File("/home/zp/Desktop/REP14/REP_20140708/lauv-noptilus-2/20140708/100908_long_pier/Data.lsf"), null);
        MraVehiclePosHud hud = new MraVehiclePosHud(source, 250, 200);
        hud.correctPositions();
        BufferedImage img = hud.getImage(hud.getStartTime()+300);

        GuiUtils.testFrame(new JLabel(new ImageIcon(img)));
    }
}
