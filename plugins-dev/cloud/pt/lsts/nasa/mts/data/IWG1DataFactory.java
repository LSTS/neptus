/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 02/06/2015
 */
package pt.lsts.nasa.mts.data;

import java.util.ArrayList;

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
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author pdias
 *
 */
public class IWG1DataFactory {

    private IWG1DataFactory() {
    }
    
    public static IWG1Data create(EstimatedState state) {
        IWG1Data o = new IWG1Data();
        o.setTimeStampMillis(state.getTimestampMillis());
 
        o.setSourceId(state.getSrc());
        
        LocationType loc = IMCUtils.parseLocation(state);
        loc = loc.getNewAbsoluteLatLonDepth();
        
        o.setLatitudeDegs(loc.getLatitudeDegs());
        o.setLongitudeDegs(loc.getLongitudeDegs());
        o.setGpsAltitude(loc.getHeight());
        
        o.setGroundSpeed(Math.sqrt(state.getVx() * state.getVx() + state.getVy() * state.getVy()
                + state.getVz() * state.getVz()));

        o.setVerticalSpeed(state.getVz());
        
        o.setPitchAngleDegs(Math.toDegrees(state.getTheta()));
        o.setRollAngleDegs(Math.toDegrees(state.getPhi()));
        o.setTrueHeadingDegs(Math.toDegrees(state.getPsi()));

        if (state.getDepth() != -1 && state.getAlt() != -1) {
            double tide = TidePredictionFactory.getTideLevel(state.getTimestampMillis());
            double bathym = state.getDepth() + state.getAlt() - tide;
            o.setBathymetry(bathym);
        }
        
        return o;        
    }
    
    public static IWG1Data create(long sourceId, SystemPositionAndAttitude state) {
        IWG1Data o = new IWG1Data();
        o.setTimeStampMillis(state.getTime());
        
        o.setSourceId(sourceId);
        
        LocationType loc = state.getPosition();
        loc = loc.getNewAbsoluteLatLonDepth();
        
        o.setLatitudeDegs(loc.getLatitudeDegs());
        o.setLongitudeDegs(loc.getLongitudeDegs());
        o.setGpsAltitude(loc.getHeight());
        
        o.setGroundSpeed(Math.sqrt(state.getVx() * state.getVx() + state.getVy() * state.getVy()
                + state.getVz() * state.getVz()));

        o.setVerticalSpeed(state.getVz());
        
        o.setPitchAngleDegs(Math.toDegrees(state.getPitch()));
        o.setRollAngleDegs(Math.toDegrees(state.getRoll()));
        o.setTrueHeadingDegs(Math.toDegrees(state.getYaw()));

        if (loc.getDepth() != -1 && state.getAltitude() != -1) {
            double tide = TidePredictionFactory.getTideLevel(state.getTime());
            double bathym = loc.getDepth() + state.getAltitude() - tide;
            o.setBathymetry(bathym);
        }
        
        return o;        
    }

    public static ArrayList<IWG1Data> create(LsfIndex index, long separationMillis) throws Exception {
        ArrayList<IWG1Data> observations = new ArrayList<>();
        
        boolean hasCTD = false;
        try {
            hasCTD = index.getNextMessageOfEntity("CTD", 0) != -1;
        }
        catch (Exception e) {
        }
        
        IndexScanner scanner = new IndexScanner(index);
        for (EstimatedState state : index.getIterator(EstimatedState.class, separationMillis)) {
            
            IWG1Data o = IWG1DataFactory.create(state);
            if (hasCTD) {
                scanner.setTime(state.getTimestamp());
                Temperature t = scanner.next(Temperature.class, "CTD");
                if (t != null) {
                    o.setTemperature(t.getValue());
                }
                
                Salinity s = scanner.next(Salinity.class, "CTD");
                if (s != null && s.getValue() > 0) {
                    o.setSalinity(s.getValue());
                } 
                
                Conductivity c = scanner.next(Conductivity.class, "CTD");
                if (c != null && c.getValue() > 0) {
                    o.setCondutivity(c.getValue());
                } 
            }
            observations.add(o);
        }
        
        return observations;              
    }
    
    public static void main(String[] args) throws Exception {
        LsfLogSource log = new LsfLogSource("D:\\LSTS-Logs\\2015-05-28-netmar\\lauv-np3-20150528-073052_bath_np3\\Data.lsf", null);
        ArrayList<IWG1Data> dataLst = create(log.getLsfIndex(), 1000);
        String testStr = "";
        for (IWG1Data iwg1Data : dataLst) {
            testStr = iwg1Data.toIWG1();
            System.out.print(testStr);
        }
        
        IWG1Data o = IWG1Data.parseIWG1(testStr);
        System.out.println("--------------");
        System.out.println(o.toIWG1());
    }
}
