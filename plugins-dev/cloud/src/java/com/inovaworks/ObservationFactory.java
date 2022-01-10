/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 31, 2015
 */
package com.inovaworks;

import java.util.ArrayList;
import java.util.Locale;

import com.inovaworks.Observation.Procedure.ObservationTypeEnum;

import pt.lsts.imc.Conductivity;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.IndexScanner;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;

/**
 * @author zp
 *
 */
public class ObservationFactory {

    public static Observation create(EstimatedState state) {
        Observation o = new Observation();
        o.observationTime = state.getTimestampMillis();
        o.phenomenonTime = state.getTimestampMillis();
        
        o.procedure.sensor = "EstimatedState";
        o.procedure.sensorHumanName = "DUNE Estimated State";
        o.procedure.type = ObservationTypeEnum.reading.toString();
        
        o.featureOfInterest.id = state.getSourceName();
        
        o.featureOfInterest.type = IMCUtils.getSystemType(state.getSrc());

        LocationType loc = IMCUtils.parseLocation(state);
        
        o.addProperty(ObservedProperty.position(loc.getLatitudeDegs(), loc.getLongitudeDegs(), state.getHeight()-state.getZ()));
        o.addProperty(ObservedProperty.speed(Math.sqrt(state.getVx() * state.getVx() + state.getVy() * state.getVy()
                + state.getVz() * state.getVz())));
        o.addProperty(ObservedProperty.verticalSpeed(state.getVz()));
        o.addProperty(ObservedProperty.heading(Math.toDegrees(state.getPsi())));
        
        if (state.getDepth() != -1 && state.getAlt() != -1) {
            double tide = TidePredictionFactory.getTideLevel(state.getTimestampMillis());
            double bathym = state.getDepth() + state.getAlt() - tide;
            o.addProperty(new ObservedProperty("bathymetry", String.format(Locale.US, "%.3f", bathym), "meters"));
        }
        
        return o;        
    }
    
    public static Observation create(String sourceName, String sourceType, SystemPositionAndAttitude state) {
        Observation o = new Observation();
        o.observationTime = state.getTime();
        o.phenomenonTime = state.getTime();
        
        o.procedure.sensor = "EstimatedState";
        o.procedure.sensorHumanName = "LSTS Estimated State";
        o.procedure.type = ObservationTypeEnum.reading.toString();
        
        o.featureOfInterest.id = sourceName;
        
        o.featureOfInterest.type = sourceType;

        LocationType loc = state.getPosition();
        
        o.addProperty(ObservedProperty.position(loc.getLatitudeDegs(), loc.getLongitudeDegs(), loc.getHeight()));
        o.addProperty(ObservedProperty.speed(state.getU()));
        o.addProperty(ObservedProperty.verticalSpeed(state.getV()));
        o.addProperty(ObservedProperty.heading(Math.toDegrees(state.getYaw())));
        
        return o;        
    }

    public static ArrayList<Observation> create(LsfIndex index, long separationMillis) throws Exception {
        ArrayList<Observation> observations = new ArrayList<Observation>();
        
        boolean hasCTD = false;
        try {
            hasCTD = index.getNextMessageOfEntity("CTD", 0) != -1;
        }
        catch (Exception e) {
            
        }
        
        IndexScanner scanner = new IndexScanner(index);
        for (EstimatedState state : index.getIterator(EstimatedState.class, separationMillis)) {
            
            Observation o = ObservationFactory.create(state);
            if (hasCTD) {
                
                scanner.setTime(state.getTimestamp());
                Temperature t = scanner.next(Temperature.class, "CTD");
                if (t != null) {
                    o.addProperty(new ObservedProperty("temperature", String.format(Locale.US, "%.4f", t.getValue()), "Cel"));
                }
                
                Salinity s = scanner.next(Salinity.class, "CTD");
                if (s != null && s.getValue() > 0) {
                    o.addProperty(new ObservedProperty("salinity", String.format(Locale.US, "%.4f", s.getValue()), "psu"));
                } 
                
                Conductivity c = scanner.next(Conductivity.class, "CTD");
                if (c != null && c.getValue() > 0) {
                    o.addProperty(new ObservedProperty("conductivity", String.format(Locale.US, "%.4f", s.getValue()), "s/m"));
                } 
            }
            observations.add(o);
        }
        
        return observations;              
    }
}
