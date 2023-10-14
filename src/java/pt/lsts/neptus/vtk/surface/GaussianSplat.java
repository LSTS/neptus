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
 * Author: hfq
 * May 11, 2013
 */
package pt.lsts.neptus.vtk.surface;

import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import vtk.vtkActor;
import vtk.vtkContourFilter;
import vtk.vtkGaussianSplatter;
import vtk.vtkPolyDataMapper;

/**
 * @author hfq
 * splat point into a volume with an elliptical, gaussian distribution
 * 
 * vtkGaussianSplatter is a filter that injects input points into a structured points (volume) datase. As each point is injected,
 * it "splats or distributes values to neaby voxels. Data is distributed using an elliptica, Gassian distribution function.
 * The distribution function is modified usainf scalar values (expands distribution) or normals (creates ellipsiodal distribution rather than spherical).
 * 
 */
public class GaussianSplat {

    private final APointCloud<?> pointCloud;
    private vtkActor actorGaussianSplat;

    public GaussianSplat(APointCloud<?> pointCloud) {
        this.pointCloud = pointCloud;
        setActorGaussianSplat(new vtkActor());
    }

    public void performGaussianSplat(int dim0, int dim1, int dim2, double radius) {

        vtkGaussianSplatter splatter  = new vtkGaussianSplatter();
        splatter.SetInput(pointCloud.getPolyData());
        // Set / get the dimensions of the sampling structured point set. Higher values produce better results but are much slower.
        splatter.SetSampleDimensions(dim0, dim1, dim2);
        splatter.SetRadius(radius);
        splatter.ScalarWarpingOff();

        vtkContourFilter surface = new vtkContourFilter();
        surface.SetInputConnection(splatter.GetOutputPort());
        surface.SetValue(0, 0.2);

        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputConnection(surface.GetOutputPort());

        actorGaussianSplat.SetMapper(mapper);
    }

    /**
     * @return the actorGaussianSplat
     */
    public vtkActor getActorGaussianSplat() {
        return actorGaussianSplat;
    }

    /**
     * @param actorGaussianSplat the actorGaussianSplat to set
     */
    private void setActorGaussianSplat(vtkActor actorGaussianSplat) {
        this.actorGaussianSplat = actorGaussianSplat;
    }
}
