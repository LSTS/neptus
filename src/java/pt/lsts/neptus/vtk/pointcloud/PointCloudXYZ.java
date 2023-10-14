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
 * Jun 3, 2014
 */
package pt.lsts.neptus.vtk.pointcloud;

import pt.lsts.neptus.vtk.pointtypes.PointXYZ;
import vtk.vtkPolyDataMapper;
import vtk.vtkVertexGlyphFilter;

/**
 * @author hfq
 *
 */
public class PointCloudXYZ extends APointCloud<PointXYZ>{

    /**
     * 
     */
    public PointCloudXYZ() {
        super();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#getPointAtIndex(int)
     */
    @Override
    public PointXYZ getPointAtIndex(int index) {
        if (getXYZPoints().GetNumberOfPoints() > index) {
            double[] p = getXYZPoints().GetPoint(index);
            return new PointXYZ(p[0], p[1], p[2]);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#addPoint(java.lang.Object)
     */
    @Override
    public void addPoint(PointXYZ p) {
        getXYZPoints().InsertNextPoint(p.getX(), p.getY(), p.getZ());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#addPointAtIndex(java.lang.Object, int)
     */
    @Override
    public void addPointAtIndex(PointXYZ p, int index) {
        getXYZPoints().InsertPoint(index, p.getX(), p.getY(), p.getZ());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#createActorFromPoints()
     */
    @Override
    public void createActorFromPoints() {
        try {
            getXYZPoints().Squeeze();
            getXYZPoints().Modified();

            getPolyData().Allocate(getNumberOfPoints(), getNumberOfPoints());
            getPolyData().SetPoints(getXYZPoints());

            vtkVertexGlyphFilter vertex = new vtkVertexGlyphFilter();
            vertex.AddInput(getPolyData());
            vertex.Update();

            getPolyData().ShallowCopy(vertex.GetOutput());
            getPolyData().Squeeze();
            getPolyData().Update();

            setBounds(getPolyData().GetBounds());

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(getPolyData().GetProducerPort());

            getCloudLODActor().SetMapper(mapper);
            getCloudLODActor().GetProperty().SetPointSize(1.0);
            getCloudLODActor().GetProperty().SetRepresentationToPoints();

            setMemorySize(mapper.GetInput().GetActualMemorySize());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#generateHandler()
     */
    @Override
    public void generateHandler() {
        setColorHandler(new PointCloudHandlerXYZ(this));
        getColorHandler().generatePointCloudColors(false);
    }
}
