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
 * May 10, 2013
 */
package pt.lsts.neptus.plugins.vtk.geo;

import vtk.vtkEarthSource;
import vtk.vtkLODActor;
import vtk.vtkPolyDataMapper;

/**
 * @author hfq
 *
 */
public class EarthSource {

    private vtkLODActor earthActor;
    
    public EarthSource() {
        setEarthActor(new vtkLODActor());
    }
    
    public void produceEarth() {        
        //vtkGlobeSource globe = new vtkGlobeSource();
        //globe.QuadrilateralTessellationOn();
        //globe.Update();
             
        vtkEarthSource earth = new vtkEarthSource();
        earth.OutlineOn();
        //earth.OutlineOff();
        earth.UpdateWholeExtent();
        earth.SetOnRatio(20);
        //earth.Modified();
        earth.Update();
        
        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputConnection(earth.GetOutputPort());
        
        // vtkGeoSource source = new vtkGeoSource();
       
        
        setEarthActor(new vtkLODActor());
        getEarthActor().SetMapper(mapper);
        getEarthActor().GetProperty().SetRepresentationToSurface();
    }

    /**
     * @return the earthActor
     */
    public vtkLODActor getEarthActor() {
        return earthActor;
    }

    /**
     * @param earthActor the earthActor to set
     */
    private void setEarthActor(vtkLODActor earthActor) {
        this.earthActor = earthActor;
    }
}
