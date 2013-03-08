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
 * Dec 4, 2012
 * $Id:: MraVehiclePosHud.java 9880 2013-02-07 15:23:52Z jqcorreia              $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

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

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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

    public MraVehiclePosHud(LsfIndex index, int width, int height) {
        this.index = index;
        this.width = width;
        this.height = height;
        loadIndex();
    }

    protected void loadIndex() {
        startTime = Math.ceil(index.getStartTime());
        endTime = Math.floor(index.getEndTime());
        int msgType = index.getDefinitions().getMessageId("EstimatedState");
        int lastIndex = 0;

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
                states.add(state);

            }
            else
                states.add(null);
        }
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
        
        g.setColor(new Color(0,0,0,64));
        g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0,0,0,200));
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

    public static void main(String[] args) throws Exception {

        //LsfIndex index = new LsfIndex(new File("/home/zp/Desktop/logs/shore_line/Data.lsf"));
        LsfIndex index = new LsfIndex(new File("/home/zp/Desktop/logs/073552_change_ref/Data.lsf"));
        MraVehiclePosHud hud = new MraVehiclePosHud(index, 250, 200);

        BufferedImage img = hud.getImage(hud.getStartTime()+20);

        GuiUtils.testFrame(new JLabel(new ImageIcon(img)));
    }
}
