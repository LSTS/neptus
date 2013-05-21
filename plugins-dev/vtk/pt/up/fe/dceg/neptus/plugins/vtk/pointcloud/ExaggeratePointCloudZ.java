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
 * May 14, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.pointcloud;

import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkCellArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;

/**
 * @author hfq
 * Handle request to exaggerate depth perception
 * FIXME - // will have to create this class for every pointCloud !?! (better performance - more memory needed)
 *  and not just create a new one on toogle selected
 */
public class ExaggeratePointCloudZ {

    private PointCloud<PointXYZ> pointCloud;
    private vtkPolyData polyData;
    private vtkPoints cloudPoints;
    private vtkPoints newCloudPoints;
    private vtkCellArray verts;
    private int zExaggeration;
    private Boolean isExaggerationPerformed;
    
    public ExaggeratePointCloudZ (PointCloud<PointXYZ> pointCloud, int zExaggeration) {
        this.pointCloud = pointCloud;
        this.polyData = pointCloud.getPoly();
        this.cloudPoints = pointCloud.getPoints();
        this.verts = new vtkCellArray();
        this.zExaggeration = zExaggeration;
        this.newCloudPoints = new vtkPoints();
        this.newCloudPoints.Allocate(cloudPoints.GetNumberOfPoints(), 0);
        this.isExaggerationPerformed = false;
    }
    
    /**
     * Depth is multipled by 10
     */
    public void performZExaggeration() {   
        try {
            if (!isExaggerationPerformed) {
                for (int i = 0; i < cloudPoints.GetNumberOfPoints(); ++i) {
                    double[] p = new double[3];
                    cloudPoints.GetPoint(i, p);
                    p[2] = p[2] * zExaggeration;
                    verts.InsertNextCell(1);
                    verts.InsertCellPoint(newCloudPoints.InsertNextPoint(p));
                }               
                pointCloud.setPoints(newCloudPoints);
                polyData.SetPoints(newCloudPoints);
                polyData.SetVerts(verts);
                polyData.Modified();
                polyData.Update();
                isExaggerationPerformed = true;
            }
//            else if (isExaggerationPerformed) {
//                NeptusLog.pub().info("Exaggeration already performed: " + isExaggerationPerformed);
//                pointCloud.setPoints(newCloudPoints);
//                polyData.SetPoints(newCloudPoints);
//                polyData.Modified();
//                polyData.Update();
//            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Pointcloud returns no normal
     */
    public void reverseZExaggeration() {
        //NeptusLog.pub().info("Reverse isExaggerationPerformed beggining: " + isExaggerationPerformed);
        try {
            pointCloud.setPoints(cloudPoints);
            polyData.SetPoints(cloudPoints);
            polyData.Modified();
            polyData.Update();
        }
        catch (Exception e) {
            
        }
    }
}
