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
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.vtk.io;

import java.io.File;
import java.io.IOException;

import vtk.vtkDataSetWriter;
import vtk.vtkOBJExporter;
import vtk.vtkPLYWriter;
import vtk.vtkPolyData;
import vtk.vtkRenderWindow;
import vtk.vtkSTLWriter;
import vtk.vtkVRMLExporter;
import vtk.vtkX3DExporter;

/**
 * @author hfq
 * Export vtk polydata to some of the most known 3D file types
 * 3D file types supported:
 * *.vtk - Native VTK file type, can also be loaded onto ParaView
 * *.obj
 * *.ply
 * *.stl
 * *.wrl
 * *.x3d
 * 
 * .3ds ?!?! - doesn't have an exporter on vtk lib (it can be imported)
 */
public class Writer3D {

    private vtkDataSetWriter exporterToVtk;
    private vtkOBJExporter exporterToOBJ;
    private vtkPLYWriter exporterToPLY;
    private vtkSTLWriter exporterToSTL;
    private vtkVRMLExporter exporterToVRML;
    private vtkX3DExporter exporterToX3D;

    /**
     * Constructor
     */
    public Writer3D() {

    }

    /**
     * 
     * @param file
     * @param poly
     * @throws IOException
     */
    public void exportToVTKFileFormat(File file, vtkPolyData poly) throws IOException {
        exporterToVtk = new vtkDataSetWriter();
        exporterToVtk.SetFileName(file.getAbsolutePath());
        exporterToVtk.SetInput(poly);
        exporterToVtk.Update();
        exporterToVtk.Write();
    }

    /**
     *
     * @param path
     * @param poly
     * @throws IOException
     */
    public void exportToVTKFileFormat(String path, vtkPolyData poly) throws IOException {
        exportToVTKFileFormat(new File(path), poly);
    }

    /**
     * not supported?!?
     * @param file
     * @param poly
     * @param renWin
     * @throws IOException
     */
    public void exportToOBJFileFormat(File file, vtkPolyData poly, vtkRenderWindow renWin) throws IOException {
        exporterToOBJ = new vtkOBJExporter();
        exporterToOBJ.SetFilePrefix(file.getParent() + "/cells");
        exporterToOBJ.SetInput(renWin);
        exporterToOBJ.Update();
        exporterToOBJ.Write();
    }

    /**
     * @param path
     * @param poly
     * @param renWin
     * @throws IOException
     */
    public void exportToOBJFileFormat(String path, vtkPolyData poly, vtkRenderWindow renWin) throws IOException {
        exportToOBJFileFormat(new File(path), poly, renWin);
    }

    /**
     * 
     * @param file
     * @param poly
     * @throws IOException
     */
    public void exportToPLYFileFormat(File file, vtkPolyData poly) throws IOException {
        exporterToPLY = new vtkPLYWriter();
        exporterToPLY.SetFileName(file.getAbsolutePath());
        exporterToPLY.SetInput(poly);
        exporterToPLY.SetFileTypeToBinary();
        //exporterToPLY.SetLookupTable()
        exporterToPLY.Update();
        exporterToPLY.Write();
    }

    public void exportToPLYFileFormat(String path, vtkPolyData poly) throws IOException {
        exportToPLYFileFormat(new File(path), poly);
    }

    /**
     * 
     * @param file
     * @param poly
     * @throws IOException
     */
    public void exportToSTLFileFormat(File file, vtkPolyData poly) throws IOException {
        exporterToSTL = new vtkSTLWriter();
        exporterToSTL.SetFileName(file.getAbsolutePath());
        exporterToSTL.SetInput(poly);
        exporterToSTL.Update();
        exporterToSTL.Write();
    }

    /**
     * 
     * @param path
     * @param poly
     * @throws IOException
     */
    public void exportToSTLFileFormat(String path, vtkPolyData poly) throws IOException {
        exportToSTLFileFormat(new File(path), poly);
    }

    /**
     * 
     * @param file
     * @param poly
     * @param renWin
     * @throws IOException
     */
    public void exportToVRMLFileFormat(File file, vtkPolyData poly, vtkRenderWindow renWin) throws IOException {
        exporterToVRML = new vtkVRMLExporter();
        exporterToVRML.SetFileName(file.getAbsolutePath());
        exporterToVRML.SetInput(renWin);
        exporterToVRML.SetSpeed(5.5);
        exporterToVRML.Update();
        exporterToVRML.Write();
    }

    /**
     * 
     * @param path
     * @param poly
     * @param renWind
     * @throws IOException
     */
    public void exportToVRMLFileFormat(String path, vtkPolyData poly, vtkRenderWindow renWin) throws IOException {
        exportToVRMLFileFormat(new File(path), poly, renWin);
    }

    /**
     * 
     * @param file
     * @param poly
     * @param renWin
     * @throws IOException
     */
    public void exportToX3DFileFormat(File file, vtkPolyData poly, vtkRenderWindow renWin) throws IOException {
        exporterToX3D = new vtkX3DExporter();
        exporterToX3D.SetFileName(file.getAbsolutePath());
        exporterToX3D.SetSpeed(5.5);
        exporterToX3D.SetInput(renWin);
        exporterToX3D.Update();
        exporterToX3D.Write();
    }

    /**
     * 
     * @param path
     * @param poly
     * @param renWin
     * @throws IOException
     */
    public void exportToX3DFileFormat(String path, vtkPolyData poly, vtkRenderWindow renWin) throws IOException {
        exportToX3DFileFormat(new File(path), poly, renWin);
    }
}
