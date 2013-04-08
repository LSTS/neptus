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
 * Apr 8, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.io;

import java.nio.file.Path;

import vtk.vtk3DSImporter;
import vtk.vtkDataSetReader;
import vtk.vtkOBJReader;
import vtk.vtkPLYReader;
import vtk.vtkSTLReader;
import vtk.vtkSimplePointsReader;

/**
 * @author hfq
 *
 */
public class VtkImporter {
    private static Path path = null;
    
    private static final String FILE_VTK_EXT = ".vtk";
    private static final String FILE_OBJ_EXT = ".obj";
    private static final String FILE_PLY_EXT = ".ply";
    private static final String FILE_STL_EXT = ".stl";
    private static final String FILE_XYZ_EXT = ".xyz";
    
    private static vtkDataSetReader readVTK;
    private static vtkOBJReader readOBJ;
    private static vtkPLYReader readPLY;
    private static vtkSTLReader readSTL;
    private static vtkSimplePointsReader importXYZ;
    
    public enum ImporterOps {
        VTK, OBJ, PLY, STL, XYZ
    }
    
    public VtkImporter(ImporterOps impOps) {
        switch (impOps)
        {
            case VTK:
                System.out.println("vtk data Set Reader chosen!");
                break;
            
            case OBJ:
                System.out.println("obj reader chosen");
                break;
                
            case PLY:
                System.out.println("ply reader chosen");
                break;
                
            case STL:
                System.out.println("stl reader chosen");
                break;
                
            case XYZ:
                System.out.println("xyz, simple points reader");
                break;
            
            default:
                System.out.println("error, nor supposed to be here");
        }
    }
    
    public static vtkOBJReader readOBJFile() {
        readOBJ = new vtkOBJReader();
        
        
        
        return readOBJ;
    }
}
