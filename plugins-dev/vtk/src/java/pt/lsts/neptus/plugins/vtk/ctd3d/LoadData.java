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
 * Author: hfq
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.ctd3d;

import java.util.Arrays;
import java.util.List;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.IndexScanner;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.vtk.pointcloud.PointCloudCTD;
import vtk.vtkDoubleArray;
import vtk.vtkPoints;

/**
 * @author hfq
 * 
 */
public class LoadData {

    private final IMraLogGroup source;

    private final PointCloudCTD pointcloud;
    private final vtkPoints points;

    private final vtkDoubleArray tempArray;
    private final vtkDoubleArray salinityArray;
    private final vtkDoubleArray pressureArray;

    List<String> validEntities = Arrays.asList("CTD", "Water Quality Sensor");

    
    /**
     * @param source
     */
    public LoadData(IMraLogGroup source) {
        this.source = source;
        this.pointcloud = new PointCloudCTD();
        this.points = pointcloud.getXYZPoints();
        this.tempArray = new vtkDoubleArray();
        this.salinityArray = new vtkDoubleArray();
        this.pressureArray = new vtkDoubleArray();
    }

    /**
     * 
     */
    public void loadCTDData() {
        LsfIndex lsfIndex = source.getLsfIndex();
        IndexScanner indexScanner = new IndexScanner(lsfIndex);
        LocationType initLoc = null;
        
        

        int count = 0;
        while (true) {
            Temperature temp = indexScanner.next(Temperature.class);
            while (temp != null && !validEntities.contains(temp.getEntityName()))
                temp = indexScanner.next(Temperature.class);
            
            Salinity salinity = indexScanner.next(Salinity.class);
            while (salinity != null && !validEntities.contains(temp.getEntityName()))
                salinity = indexScanner.next(Salinity.class);
            
            if (temp == null && salinity == null) {
                break;
            }

            EstimatedState state = indexScanner.next(EstimatedState.class);
            while (state != null && state.getSrc() != temp.getSrc())
                state = indexScanner.next(EstimatedState.class);
            
            if (state == null)
                break;
            
            SystemPositionAndAttitude pose = IMCUtils.parseState(state);
            LocationType currentLoc = pose.getPosition();
            if (initLoc == null)
                initLoc = new LocationType(currentLoc);
            double pointPose[] = currentLoc.getOffsetFrom(initLoc);

            if (temp != null)
                tempArray.InsertValue(count, temp.getValue());

            if (salinity != null)
                salinityArray.InsertValue(count, salinity.getValue());

            points.InsertNextPoint(pointPose[0], pointPose[1], pose.getPosition().getDepth());

            pressureArray.InsertValue(count, pose.getPosition().getDepth());
            ++count;
        }

        pointcloud.setNumberOfPoints(points.GetNumberOfPoints());
        pointcloud.setXYZPoints(points);
        pointcloud.setTemperatures(tempArray);
        pointcloud.setSalinities(salinityArray);
        pointcloud.setPressures(pressureArray);
    }

    /**
     * @return the pointcloud
     */
    public PointCloudCTD getPointcloud() {
        return pointcloud;
    }
}
