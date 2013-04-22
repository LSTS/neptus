/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Apr 19, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.util.Random;

import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkLODActor;
import vtk.vtkPoints;
import vtk.vtkScalarsToColors;

/**
 * @author hfq
 *
 */
public class PointCloudHandlers<T extends PointXYZ> {
    
    public PointCloudHandlers() {
        
    }

    public static double[] getRandomColor(PointCloud<PointXYZ> cloud) {
        cloud.getNumberOfPoints();
        
        for (int i = 0; i < cloud.getNumberOfPoints(); i++) {
            double[] point = new double[3];
            point = cloud.points.GetPoint(i);
        }
        
        double[] rgbColor = new double[3];
        
        return rgbColor;
    }
    
    public static double[] getRandomColor() {
        double[] rgbCloud = new double[3];
        double sum;
        int step = 100;
        
        do {
            sum = 0;
            //rgbCloud[0] = (Math.random() % step) / (double)step;
            rgbCloud[0] = Math.random();
            //rgbCloud[1] = (Math.random() % step) / (double)step;
            rgbCloud[1] = Math.random();
            //rgbCloud[2] = (Math.random() % step) / (double)step;
            rgbCloud[2] = Math.random();
            sum = rgbCloud[0] + rgbCloud[1] + rgbCloud[2];
            //System.out.println("r = " + rgbCloud[0] + ", g = " + rgbCloud[1] + ", b = " + rgbCloud[2]);
        }while (sum <= 0.5 || sum >= 2.8);

        //rgbCloud[0] = min + Math.random() * ((min - max) + min); // [5,10];
        
        return rgbCloud;
    }
    
    public static vtkScalarsToColors getRandomColor2() {
        vtkScalarsToColors scalars = new vtkScalarsToColors();
        
      
        
        return scalars;
    }
}
