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
 * May 27, 2013
 */
package pt.lsts.neptus.vtk.surface;

import pt.lsts.neptus.NeptusLog;
import vtk.vtkButterflySubdivisionFilter;
import vtk.vtkLinearSubdivisionFilter;
import vtk.vtkLoopSubdivisionFilter;
import vtk.vtkPolyData;
import vtk.vtkPolyDataAlgorithm;

/**
 * @author hfq
 *
 */
public class MeshSubdivision {

    public enum MeshSubdivisionFilterType {
        LINEAR, LOOP, BUTTERFLY
    }
    
    private MeshSubdivisionFilterType filterType = MeshSubdivisionFilterType.LINEAR;
    
    private vtkPolyData polyDataSubdivided;
    
    public MeshSubdivision() {
        polyDataSubdivided = new vtkPolyData();
    }
    
    public void performProcessing(vtkPolyData polyData) {
        
        vtkPolyDataAlgorithm subdivisionFilter = null;
        switch (filterType) {
            case LINEAR:
                subdivisionFilter = new vtkLinearSubdivisionFilter();
                break;
            case LOOP:
                subdivisionFilter = new vtkLoopSubdivisionFilter();
                break;
            case BUTTERFLY:
                subdivisionFilter = new vtkButterflySubdivisionFilter();
                break;
            default:
                NeptusLog.pub().error("MeshSubdivion: Invalid filter selection");
                break;
        }
        
        subdivisionFilter.SetInput(polyData);
        subdivisionFilter.Update();
        
        polyDataSubdivided = subdivisionFilter.GetOutput();
    }
    

    /**
     * Get the mesh subdivision filter type
     * @return the filterType
     */
    public MeshSubdivisionFilterType getFilterType() {
        return filterType;
    }

    /**
     * Set the mehs subdivion filter type
     * @param filterType the filterType to set
     */
    public void setFilterType(MeshSubdivisionFilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * @return the polyDataSubdivided
     */
    public vtkPolyData getPolyDataSubdivided() {
        return polyDataSubdivided;
    }

    /**
     * @param polyDataSubdivided the polyDataSubdivided to set
     */
    protected void setPolyDataSubdivided(vtkPolyData polyDataSubdivided) {
        this.polyDataSubdivided = polyDataSubdivided;
    }
    
    
}
