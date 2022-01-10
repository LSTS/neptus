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
 * Author: pdias
 * 12/06/2015
 */
package pt.lsts.neptus.plugins.urready4os.vtk;

import java.util.List;

import pt.lsts.neptus.plugins.urready4os.rhodamine.BaseData;
import pt.lsts.neptus.types.coord.LocationType;
import vtk.vtkDoubleArray;
import vtk.vtkPoints;

/**
 * @author pdias
 *
 */
public class RhodaminePointCloudLoader {

    private RhodaminePointCloudLoader() {
    }

    public static PointCloudRhodamine[] loadRhodamineData(List<BaseData> dataLst, List<BaseData> prevLst,
            double predictionScaleFactor) {
        PointCloudRhodamine pointcloud = new PointCloudRhodamine();
        vtkPoints points = pointcloud.getXYZPoints();
        vtkDoubleArray rhodArray = new vtkDoubleArray();

        PointCloudRhodamine pointcloudPrev = new PointCloudRhodamine();
        vtkPoints pointsPrev = pointcloudPrev.getXYZPoints();
        vtkDoubleArray prevArray = new vtkDoubleArray();

        LocationType firstPtLoc = null;
        
        int count = 0;
        for (BaseData pt : dataLst) {
            if (pt == null || Double.isNaN(pt.getRhodamineDyePPB()))
                continue;
            
            double offsetN = 0;
            double offsetE = 0;
            if (firstPtLoc == null) {
                firstPtLoc = new LocationType();
                firstPtLoc.setLatitudeDegs(pt.getLat());
                firstPtLoc.setLongitudeDegs(pt.getLon());
            }
            else {
                LocationType ptLoc = new LocationType();
                ptLoc.setLatitudeDegs(pt.getLat());
                ptLoc.setLongitudeDegs(pt.getLon());
                double[] offs = ptLoc.getOffsetFrom(firstPtLoc);
                offsetN = offs[0];
                offsetE = offs[1];
            }
            points.InsertNextPoint(offsetN, offsetE, pt.getDepth());
            rhodArray.InsertValue(count, pt.getRhodamineDyePPB());
            
            count++;
        }
        
        pointcloud.setNumberOfPoints(count);
        pointcloud.setXYZPoints(points);
        pointcloud.setRhodamineDyeList(rhodArray);

        count = 0;
        for (BaseData pt : prevLst) {
            if (pt == null || Double.isNaN(pt.getRhodamineDyePPB()))
                continue;
            
            double offsetN = 0;
            double offsetE = 0;
            if (firstPtLoc == null) {
                firstPtLoc = new LocationType();
                firstPtLoc.setLatitudeDegs(pt.getLat());
                firstPtLoc.setLongitudeDegs(pt.getLon());
            }
            else {
                LocationType ptLoc = new LocationType();
                ptLoc.setLatitudeDegs(pt.getLat());
                ptLoc.setLongitudeDegs(pt.getLon());
                double[] offs = ptLoc.getOffsetFrom(firstPtLoc);
                offsetN = offs[0];
                offsetE = offs[1];
            }
            pointsPrev.InsertNextPoint(offsetN, offsetE, pt.getDepth());
            prevArray.InsertValue(count, pt.getRhodamineDyePPB() * predictionScaleFactor);
            
            count++;
        }
        
        pointcloudPrev.setNumberOfPoints(count);
        pointcloudPrev.setXYZPoints(pointsPrev);
        pointcloudPrev.setRhodamineDyeList(prevArray);

        return new PointCloudRhodamine[] { pointcloud, pointcloudPrev };
    }
}
