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
 * Author: pdias
 * 12/06/2015
 */
package pt.lsts.neptus.plugins.urready4os.vtk;

import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import vtk.vtkDoubleArray;
import vtk.vtkPolyDataMapper;
import vtk.vtkVertexGlyphFilter;

/**
 * @author pdias
 *
 */
public class PointCloudRhodamine extends APointCloud<PointRhodamine> {

    private vtkDoubleArray rhodamineDyeList;
    private double[] useRange = null;
    
    /**
     * @param name
     */
    public PointCloudRhodamine(String name) {
        super(name);
    }

    /**
     * 
     */
    public PointCloudRhodamine() {
    }

    /**
     * @return the useRange
     */
    public double[] getUseRange() {
        if (useRange == null)
            useRange = rhodamineDyeList.GetRange();
        return useRange;
    }

    public double[] updateUseRange() {
        useRange = rhodamineDyeList.GetRange();
        return useRange;
    }

    /**
     * @param useRange the useRange to set
     */
    public void setUseRange(double[] useRange) {
        this.useRange = useRange;
    }
    
    @Override
    public PointRhodamine getPointAtIndex(int index) {
        if (getXYZPoints().GetNumberOfPoints() > index) {
            double[] p = getXYZPoints().GetPoint(index);
            double r = getRhodamineList().GetValue(index);
            return new PointRhodamine(p[0], p[1], p[2], r);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#addPoint(java.lang.Object)
     */
    @Override
    public void addPoint(PointRhodamine p) {
        getXYZPoints().InsertNextPoint(p.getX(), p.getY(), p.getZ());
        int rhodamineSize = getRhodamineList().GetSize();
        getRhodamineList().InsertValue(rhodamineSize, p.getRhodamineDye());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.APointCloud#addPointAtIndex(java.lang.Object, int)
     */
    @Override
    public void addPointAtIndex(PointRhodamine p, int index) {
        getXYZPoints().InsertPoint(index, p.getX(), p.getY(), p.getZ());
        getRhodamineList().InsertValue(index, p.getRhodamineDye());
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
        setColorHandler(new PointCloudHandlerRhodamineDye(this));
        if (useRange != null)
            ((PointCloudHandlerRhodamineDye) getColorHandler()).setUseRange(useRange);
        getColorHandler().generatePointCloudColors(true);
    }

    /**
     * @return the rhodamineList
     */
    public vtkDoubleArray getRhodamineList() {
        return rhodamineDyeList;
    }
    
    /**
     * @param rhodamineDyeList the rhodamineDyeList to set
     */
    public void setRhodamineDyeList(vtkDoubleArray rhodamineDyeList) {
        this.rhodamineDyeList = rhodamineDyeList;
    }
}
