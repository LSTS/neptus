/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * May 9, 2013
 */
package pt.lsts.neptus.plugins.vtk.geo;

import vtk.vtkGeoSource;
import vtk.vtkGeoTerrain;
import vtk.vtkGeoView;

/**
 * @author hfq
 * Not working on vtk 5.8
 */
public class GeoView {

    public GeoView() {
        vtkGeoView view = new vtkGeoView();
        view.DisplayHoverTextOff();
        view.GetRenderWindow().SetMultiSamples(0);
        view.GetRenderWindow().SetSize(400, 400);
        
        vtkGeoTerrain terrain = new vtkGeoTerrain();
        
        vtkGeoSource terrainSource = new vtkGeoSource();
        //vtkGeoFileTerrainSource terrainSource = new vtkGeoFileTerrainSource();
        // vtkGeoGlobeSource geoGlobe = new vtkGeoGlobeSource();
        
        //terrainSource.Register(geoGlobe);
        terrainSource.SetReferenceCount(1);
        terrainSource.GetReferenceCount();
        terrainSource.Modified();

        //terrainSource.VTKGetClassNameFromReference(geoGlobe.GetVTKId());
        terrainSource.Initialize(0);
        
        terrain.SetSource(terrainSource);
        view.SetTerrain(terrain);
        
        
        view.Render();
        view.GetInteractor().Initialize();
        view.GetInteractor().Start();
    }
}
