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
 * May 9, 2013
 */
package pt.lsts.neptus.vtk.surface;

import pt.lsts.neptus.vtk.filters.Contours;
import vtk.vtkLODActor;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 * @author hfq
 * 
 */
public class PointCloudMesh {

    private vtkPolyData polyData;
    private vtkLODActor meshCloudLODActor;

    private Contours contours;

    public PointCloudMesh() {
        setPolyData(new vtkPolyData());
        setMeshCloudLODActor(new vtkLODActor());
        setContours(new Contours(this));
    }

    /**
     * 
     * @param polyData
     */
    public void generateLODActorFromPolyData(vtkPolyData polyData) {
        setPolyData(new vtkPolyData());
        setPolyData(polyData);
        getPolyData().Update();

        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputConnection(getPolyData().GetProducerPort());
        mapper.Update();

        setMeshCloudLODActor(new vtkLODActor());
        getMeshCloudLODActor().SetMapper(mapper);
        getMeshCloudLODActor().Modified();
    }

    /**
     * @return the meshCloudLODActor
     */
    public vtkLODActor getMeshCloudLODActor() {
        return meshCloudLODActor;
    }

    /**
     * @param meshCloudLODActor the meshCloudLODActor to set
     */
    public void setMeshCloudLODActor(vtkLODActor meshCloudLODActor) {
        this.meshCloudLODActor = meshCloudLODActor;
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
    public void setPolyData(vtkPolyData polyData) {
        this.polyData = polyData;
    }

    /**
     * @return the contours
     */
    public Contours getContours() {
        return contours;
    }

    /**
     * @param contours the contours to set
     */
    public void setContours(Contours contours) {
        this.contours = contours;
    }
}
