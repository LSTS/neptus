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
 * Author: hfq
 * Jun 3, 2014
 */
package pt.lsts.neptus.vtk.pointcloud;

import pt.lsts.neptus.vtk.pointtypes.PointXYZCTD;
import vtk.vtkDoubleArray;
import vtk.vtkPolyDataMapper;
import vtk.vtkVertexGlyphFilter;

/**
 * @author hfq
 *
 */
public class PointCloudCTD extends APointCloud<PointXYZCTD> {
    private vtkDoubleArray temperatures;
    private vtkDoubleArray salinities;
    private vtkDoubleArray pressures;

    /**
     * 
     */
    public PointCloudCTD() {
        super();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#getPointAtIndex(int)
     */
    @Override
    public PointXYZCTD getPointAtIndex(int index) {
        if (getXYZPoints().GetNumberOfPoints() > index) {
            double[] p = getXYZPoints().GetPoint(index);
            double t = getTemperatures().GetValue(index);
            double s = getSalinities().GetValue(index);
            double pr = getPressures().GetValue(index);
            return new PointXYZCTD(p[0], p[1], p[2], s, t, pr);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#addPoint(java.lang.Object)
     */
    @Override
    public void addPoint(PointXYZCTD p) {
        getXYZPoints().InsertNextPoint(p.getX(), p.getY(), p.getZ());
        int tempSize = getTemperatures().GetSize();
        getTemperatures().InsertValue(tempSize, p.getTemperature());
        int salSize = getSalinities().GetSize();
        getSalinities().InsertValue(salSize, p.getSalinity());
        int preSize = getPressures().GetSize();
        getPressures().InsertValue(preSize, p.getPressure());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#addPointAtIndex(java.lang.Object, int)
     */
    @Override
    public void addPointAtIndex(PointXYZCTD p, int index) {
        getXYZPoints().InsertPoint(index, p.getX(), p.getY(), p.getZ());
        getTemperatures().InsertValue(index, p.getTemperature());
        getSalinities().InsertValue(index, p.getSalinity());
        getPressures().InsertValue(index, p.getPressure());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#createActorFromPoints()
     */
    @Override
    public void createActorFromPoints() {
        try{
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
            getCloudLODActor().GetProperty().SetPointSize(10.0);
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
        setColorHandler(new PointCloudHandlerCTD(this));
        getColorHandler().generatePointCloudColors(true);
    }

    //    /**
    //     * @param pointCloudHandlerCTD
    //     */
    //    private void setColorHandler(PointCloudHandlerCTD pointCloudHandlerCTD) {
    //        getColorHandler() = pointCloudHandlerCTD;
    //    }

    /**
     * @return the temperatures
     */
    public vtkDoubleArray getTemperatures() {
        return temperatures;
    }

    /**
     * @param temperatures the temperatures to set
     */
    public void setTemperatures(vtkDoubleArray temperatures) {
        this.temperatures = temperatures;
    }

    /**
     * @return the salinities
     */
    public vtkDoubleArray getSalinities() {
        return salinities;
    }

    /**
     * @param salinities the salinities to set
     */
    public void setSalinities(vtkDoubleArray salinities) {
        this.salinities = salinities;
    }

    /**
     * @return the pressures
     */
    public vtkDoubleArray getPressures() {
        return pressures;
    }

    /**
     * @param pressures the pressures to set
     */
    public void setPressures(vtkDoubleArray pressures) {
        this.pressures = pressures;
    }
}
