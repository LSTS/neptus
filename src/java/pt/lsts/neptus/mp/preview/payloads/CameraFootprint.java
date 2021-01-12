/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Aug 7, 2013
 */
package pt.lsts.neptus.mp.preview.payloads;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class CameraFootprint extends PayloadFingerprint {

    private double crossFov, alongFov;
    private Color color;
    private double maxDistance = Double.MAX_VALUE;

    private static LinkedHashMap<String, LinkedHashMap<String, String>> uavCamProperties = new LinkedHashMap<>();

    CameraFOV fov;

    /**
     * Create a camera footprint
     * 
     * @param alongTrackFov along track field of view, in radians
     * @param crossTrackFov cross track field of view, in radians
     * @param maxDistance distance after which the images are discarded (unfocused?)
     * @param color Color to use
     */
    public CameraFootprint(double alongTrackFov, double crossTrackFov, double maxDistance, Color color) {
        super("Camera", new Color(192, 192, 0, 128));
        this.alongFov = alongTrackFov;
        this.crossFov = crossTrackFov;
        this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
        this.maxDistance = maxDistance;
        fov = new CameraFOV(crossFov, alongFov);
    }

    public CameraFootprint(String model, Color color) {
        super(model, color);
        String v = "x8-05";
        loadProperties(v);
        this.crossFov = getHorizontalAOV(v, model);
        this.alongFov = getVerticalAOV(v, model);
        this.color = color;
        this.fov = new CameraFOV(crossFov, alongFov);
        this.fov.setTilt((Math.PI/2)+getTiltAngle(v, model));
    }

    /**
     * Create a camera footprint with given color and FOVs
     * 
     * @param alongTrackFov along track field of view, in radians
     * @param crossTrackFov cross track field of view, in radians
     * @param color Color to use
     */
    public CameraFootprint(double alongTrackFov, double crossTrackFov, Color color) {
        this(alongTrackFov, crossTrackFov, Double.MAX_VALUE, color);
    }

    /**
     * Create a camera footprint with given color, along track FOV and 16/9 horizontal/vertical ratio
     * 
     * @param fov along track field of view, in radians
     * @param color Color to use
     */
    public CameraFootprint(double fov, Color color) {
        this(fov, fov * (16. / 9.), Double.MAX_VALUE, color);
    }

    @Override
    public Color getColor() {
        return color;
    }

    private static Double getHorizontalAOV(String vehicle, String Model) {
        if (!uavCamProperties.containsKey(vehicle))
            loadProperties(vehicle);

        String paramName = "(" + Model + ") Horizontal AOV";
        
        if (uavCamProperties.get(vehicle).containsKey(paramName))
            return Math.toRadians(Double.parseDouble(uavCamProperties.get(vehicle).get(paramName)));

        return new Double(0);
    }

    private static Double getVerticalAOV(String vehicle, String Model) {
        if (!uavCamProperties.containsKey(vehicle))
            loadProperties(vehicle);
        String paramName = "(" + Model + ") Vertical AOV";
        if (uavCamProperties.get(vehicle).containsKey(paramName))
            return Math.toRadians(Double.parseDouble(uavCamProperties.get(vehicle).get(paramName)));

        return new Double(0);
    }

    private static Double getTiltAngle(String vehicle, String Model) {
        if (!uavCamProperties.containsKey(vehicle))
            loadProperties(vehicle);
        String paramName = "(" + Model + ") Tilt Angle";
        if (uavCamProperties.get(vehicle).containsKey(paramName))
            return Math.toRadians(Double.parseDouble(uavCamProperties.get(vehicle).get(paramName)));

        return new Double(0);
    }

    private static void loadProperties(String vehicle) {
        if (!uavCamProperties.containsKey(vehicle)) {
            ArrayList<SystemProperty> props = ConfigurationManager.getInstance().getPropertiesByEntity(vehicle,
                    "UAVCamera", Visibility.DEVELOPER, Scope.GLOBAL);

            uavCamProperties.put(vehicle, new LinkedHashMap<>());
            for (SystemProperty p : props)
                uavCamProperties.get(vehicle).put(p.getName(), "" + p.getValue());
        }
    }    

    public Area getFingerprint(SystemPositionAndAttitude pose) {
        if (pose.getAltitude() > maxDistance)
            return new Area(new Rectangle2D.Double(0, 0, 0, 0));
        fov.setState(pose);
        fov.setYaw(0);
        GeneralPath path = new GeneralPath();
        Point2D prev = null;
        for (LocationType loc : fov.getFootprintQuad()) {
            double nedOffsets[] = loc.getOffsetFrom(pose.getPosition());
            Point2D cur = new Point2D.Double(nedOffsets[1], -nedOffsets[0]);
            if (prev == null)
                path.moveTo(cur.getX(), cur.getY());
            else
                path.lineTo(cur.getX(), cur.getY());
            prev = cur;
        }
        path.closePath();
        return new Area(path);
    };
}
