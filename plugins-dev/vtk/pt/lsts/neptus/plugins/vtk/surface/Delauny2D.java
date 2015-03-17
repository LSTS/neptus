/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * May 23, 2013
 */
package pt.lsts.neptus.plugins.vtk.surface;

import pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud;
import vtk.vtkCleanPolyData;
import vtk.vtkDelaunay2D;
import vtk.vtkPolyData;

/**
 * @author hfq
 *
 */
public class Delauny2D {
    private vtkPolyData polyData;

    public Delauny2D() {

    }

    public void performDelauny(APointCloud<?> inputCloud) {

        // Clean point cloud
        vtkCleanPolyData cleanPolyData = new vtkCleanPolyData();
        cleanPolyData.SetInputConnection(inputCloud.getPolyData().GetProducerPort());
        cleanPolyData.Update();

        // Generate mesh
        vtkDelaunay2D delauny = new vtkDelaunay2D();
        delauny.SetInputConnection(cleanPolyData.GetOutputPort());
        //delauny.BoundingTriangulationOn();
        delauny.Update();

        setPolyData(delauny.GetOutput());
    }

    /**
     * @return the polyData
     */
    public vtkPolyData getPolyData() {
        return polyData;
    }

    /**
     * @param polyData the polyData to set
     */
    private void setPolyData(vtkPolyData polyData) {
        this.polyData = polyData;
    }
}
