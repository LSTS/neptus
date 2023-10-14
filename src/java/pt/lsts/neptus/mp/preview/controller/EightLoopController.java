/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 12/10/2017
 */
package pt.lsts.neptus.mp.preview.controller;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.UnicycleModel;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;

public class EightLoopController {
    
    private enum State {
        LEFT_ARC,
        TRAVEL_TO_CENTER_FOR_RIGHT,
        TRAVEL_TO_RIGHT,
        RIGHT_ARC,
        TRAVEL_TO_CENTER_FOR_LEFT,
        TRAVEL_TO_LEFT
    }
    
    private EightLoopController.State stageState = State.TRAVEL_TO_LEFT;
    private boolean inPattern = false;
    
    private LocationType destination = null;
    private LocationType target = null;
    
    private final double centerLatRad;
    private final double centerLonRad;

    private final double leftLatRad;
    private final double leftLonRad;
    private final double rightLatRad;
    private final double rightLonRad;
    
    private final double leftExitLatRad;
    private final double leftExitLonRad;
    private final double leftEntryLatRad;
    private final double leftEntryLonRad;
    
    private final double rightExitLatRad;
    private final double rightExitLonRad;
    private final double rightEntryLatRad;
    private final double rightEntryLonRad;
    
    private double speedMps = 2;
    
    private double radius = 10;
    
    private boolean clockwise = true;
    private double bearingRads = 0;
    private double rotRads = 0;
    
    private double arcStepRads = 0;
    
    public EightLoopController(double centerLatRad, double centerLonRad, double bearingRads, double length, double radius,
            double depth, boolean clockwise, double speedMps) {
        this.centerLatRad = centerLatRad;
        this.centerLonRad = centerLonRad;
        
        this.bearingRads = bearingRads;
        
        this.speedMps = speedMps;

        if (length < radius * 2.0)
            length = radius * 2.0;

        this.radius = radius;
        
        this.clockwise = clockwise;
        
        rotRads = this.bearingRads - Math.PI / 2.0;
        double[] pos = AngleUtils.rotate(rotRads, length / 2.0, 0, !clockwise);
        double[] dipLLD = CoordinateUtil.WGS84displace(Math.toDegrees(centerLatRad), Math.toDegrees(centerLonRad), 0, pos[0], pos[1], 0);
        leftLatRad = Math.toRadians(dipLLD[0]);
        leftLonRad = Math.toRadians(dipLLD[1]);
        
        pos = AngleUtils.rotate(rotRads + Math.PI, length / 2.0, 0, !clockwise);
        dipLLD = CoordinateUtil.WGS84displace(Math.toDegrees(centerLatRad), Math.toDegrees(centerLonRad), 0, pos[0], pos[1], 0);
        rightLatRad = Math.toRadians(dipLLD[0]);
        rightLonRad = Math.toRadians(dipLLD[1]);

        pos = AngleUtils.rotate(rotRads + Math.PI / 2.0, radius, 0, !clockwise);
        dipLLD = CoordinateUtil.WGS84displace(Math.toDegrees(leftLatRad), Math.toDegrees(leftLonRad), 0, pos[0], pos[1], 0);
        leftExitLatRad = Math.toRadians(dipLLD[0]);
        leftExitLonRad = Math.toRadians(dipLLD[1]);

        pos = AngleUtils.rotate(rotRads - Math.PI / 2.0, radius, 0, !clockwise);
        dipLLD = CoordinateUtil.WGS84displace(Math.toDegrees(leftLatRad), Math.toDegrees(leftLonRad), 0, pos[0], pos[1], 0);
        leftEntryLatRad = Math.toRadians(dipLLD[0]);
        leftEntryLonRad = Math.toRadians(dipLLD[1]);

        pos = AngleUtils.rotate(rotRads + Math.PI - Math.PI / 2.0, radius, 0, !clockwise);
        dipLLD = CoordinateUtil.WGS84displace(Math.toDegrees(rightLatRad), Math.toDegrees(rightLonRad), 0, pos[0], pos[1], 0);
        rightExitLatRad = Math.toRadians(dipLLD[0]);
        rightExitLonRad = Math.toRadians(dipLLD[1]);

        pos = AngleUtils.rotate(rotRads + Math.PI + Math.PI / 2.0, radius, 0, !clockwise);
        dipLLD = CoordinateUtil.WGS84displace(Math.toDegrees(rightLatRad), Math.toDegrees(rightLonRad), 0, pos[0], pos[1], 0);
        rightEntryLatRad = Math.toRadians(dipLLD[0]);
        rightEntryLonRad = Math.toRadians(dipLLD[1]);

        destination = new LocationType();
        destination.setLatitudeRads(leftEntryLatRad);
        destination.setLongitudeRads(leftEntryLonRad);
        destination.setDepth(depth);

        target = new LocationType(destination);
    }

    public LocationType step(UnicycleModel model, SystemPositionAndAttitude state, double timestep, double ellapsedTime) {
        double arcStepIncRads = Math.asin(speedMps * timestep / radius);

//        System.out.println(stageState + "  ::  deltaXY state  vs target =" + state.getPosition().getHorizontalDistanceInMeters(target) + "   speed=" + speedMps);
        if (state.getPosition().getHorizontalDistanceInMeters(target) < speedMps) {
            switch (stageState) {
                case TRAVEL_TO_LEFT:
                    changeTo(State.LEFT_ARC);
                    break;
                case LEFT_ARC:
                    changeTo(State.TRAVEL_TO_CENTER_FOR_RIGHT);
                    break;
                case TRAVEL_TO_CENTER_FOR_RIGHT:
                    changeTo(State.TRAVEL_TO_RIGHT);
                    break;
                case TRAVEL_TO_RIGHT:
                    changeTo(State.RIGHT_ARC);
                    break;
                case RIGHT_ARC:
                    changeTo(State.TRAVEL_TO_CENTER_FOR_LEFT);
                    break;
                case TRAVEL_TO_CENTER_FOR_LEFT:
                    changeTo(State.TRAVEL_TO_LEFT);
                    break;
            }
        }

        switch (stageState) {
            case LEFT_ARC:
//                System.out.println(stageState + "  ::  deltaXY state  vs dest =" + state.getPosition().getHorizontalDistanceInMeters(destination) + "   speed=" + speedMps);
                if (state.getPosition().getHorizontalDistanceInMeters(destination) < speedMps) {
                    arcStepRads += arcStepIncRads;
                    double[] pos = AngleUtils.rotate(rotRads - Math.PI / 2.0 + arcStepRads, radius, 0, !clockwise);
                    destination.setLocation(target);
                    destination.setLatitudeRads(leftLatRad);
                    destination.setLongitudeRads(leftLonRad);
                    destination.translatePosition(pos[0], pos[1], 0);
                }
                break;
            case RIGHT_ARC:
//                System.out.println(stageState + "  ::  deltaXY state  vs dest =" + state.getPosition().getHorizontalDistanceInMeters(destination) + "   speed=" + speedMps);
                if (state.getPosition().getHorizontalDistanceInMeters(destination) < speedMps) {
                    arcStepRads += arcStepIncRads;
                    double[] pos = AngleUtils.rotate(rotRads - Math.PI / 2.0 - arcStepRads, radius, 0, !clockwise);
                    destination.setLocation(target);
                    destination.setLatitudeRads(rightLatRad);
                    destination.setLongitudeRads(rightLonRad);
                    destination.translatePosition(pos[0], pos[1], 0);
                }
                break;
            case TRAVEL_TO_LEFT:
            case TRAVEL_TO_RIGHT:
            case TRAVEL_TO_CENTER_FOR_RIGHT:
            case TRAVEL_TO_CENTER_FOR_LEFT:
                break;
        }
        
        return new LocationType(destination);
    }
    
    private void changeTo(EightLoopController.State state) {
        inPattern = true;
        stageState = state;
        
        switch (state) {
            case TRAVEL_TO_LEFT:
                target.setLatitudeRads(leftEntryLatRad);
                target.setLongitudeRads(leftEntryLonRad);
                destination.setLocation(target);
                break;
            case LEFT_ARC:
                target.setLatitudeRads(leftExitLatRad);
                target.setLongitudeRads(leftExitLonRad);
                destination.setLatitudeRads(leftEntryLatRad);
                destination.setLongitudeRads(leftEntryLonRad);
                arcStepRads = 0;
                break;
            case TRAVEL_TO_RIGHT:
                target.setLatitudeRads(rightEntryLatRad);
                target.setLongitudeRads(rightEntryLonRad);
                destination.setLocation(target);
                break;
            case RIGHT_ARC:
                target.setLatitudeRads(rightExitLatRad);
                target.setLongitudeRads(rightExitLonRad);
                destination.setLatitudeRads(rightEntryLatRad);
                destination.setLongitudeRads(rightEntryLonRad);
                arcStepRads = 0;
                break;
            case TRAVEL_TO_CENTER_FOR_RIGHT:
            case TRAVEL_TO_CENTER_FOR_LEFT:
                target.setLatitudeRads(centerLatRad);
                target.setLongitudeRads(centerLonRad);
                destination.setLocation(target);
                break;
        }
    }
    
    /**
     * @return the inPattern
     */
    public boolean isInPattern() {
        return inPattern;
    }
}