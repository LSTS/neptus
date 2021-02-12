/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 8, 2013
 */
package pt.lsts.neptus.vtk.io;

import java.io.File;
import java.nio.file.Path;

import pt.lsts.neptus.NeptusLog;
import vtk.vtk3DSImporter;
import vtk.vtkDataSetReader;
import vtk.vtkLODActor;
import vtk.vtkOBJReader;
import vtk.vtkPLYReader;
import vtk.vtkPolyDataMapper;
import vtk.vtkSTLReader;
import vtk.vtkSimplePointsReader;
import vtk.vtkVRMLImporter;

/**
 * @author hfq
 *
 */
public class Reader3D {
    protected static Path path = null;

    // private static String filePath;
    // private static String fileName;
    private static String absolutePath;

    protected static final String FILE_VTK_EXT = ".vtk";
    protected static final String FILE_OBJ_EXT = ".obj";
    protected static final String FILE_PLY_EXT = ".ply";
    protected static final String FILE_STL_EXT = ".stl";
    protected static final String FILE_XYZ_EXT = ".xyz";
    protected static final String FILE_3DS_EXT = ".3ds";
    protected static final String FILE_VRML_EXT = ".wrl";

    protected static vtkDataSetReader readVTK;
    protected static vtkOBJReader readOBJ;
    protected static vtkPLYReader readPLY;
    protected static vtkSTLReader readSTL;
    protected static vtkSimplePointsReader readXYZ;
    protected static vtk3DSImporter import3ds;
    protected static vtkVRMLImporter importVRML;

    private vtkLODActor actor;

    public enum ImporterOps {
        VTK, OBJ, PLY, STL, XYZ, ThreeDS, WRL
    }

    private ImporterOps impOp;

    public Reader3D(File file) {

        checkFileExtention(file);        
        switch (impOp)
        {
            case VTK:
                NeptusLog.pub().info("vtk data Set Reader chosen!");
                readVTKFile();
                break;

            case OBJ:
                NeptusLog.pub().info("obj reader chosen");
                readOBJFile();
                break;

            case PLY:
                NeptusLog.pub().info("ply reader chosen");
                readPLYFile();
                break;

            case STL:
                NeptusLog.pub().info("stl reader chosen");
                readSTLFile();
                break;

            case XYZ:
                NeptusLog.pub().info("xyz, simple points reader");
                readXYZfile();
                break;

            default:
                NeptusLog.pub().info("error file extention not found, not supposed to be here");
        }
    }

    /**
     * Checks file extention of the intended loading file
     * @param file
     */
    private void checkFileExtention(File file) {

        absolutePath = file.getAbsolutePath();

        try {
            if (file.isDirectory()) {
                String stringFile = file.toString();

                if (stringFile.endsWith(FILE_OBJ_EXT)) {
                    impOp = ImporterOps.OBJ;
                }
                else if (stringFile.endsWith(FILE_PLY_EXT)) {
                    impOp = ImporterOps.PLY;
                }
                else if (stringFile.endsWith(FILE_STL_EXT)) {
                    impOp = ImporterOps.STL;
                }
                else if (stringFile.endsWith(FILE_VTK_EXT)) {
                    impOp = ImporterOps.VTK;
                }
                else if (stringFile.endsWith(FILE_XYZ_EXT)) {
                    impOp = ImporterOps.XYZ;
                }
                else if (stringFile.endsWith(FILE_3DS_EXT)) {
                    impOp = ImporterOps.ThreeDS;
                }
                else if (stringFile.endsWith(FILE_VRML_EXT)) {
                    impOp = ImporterOps.WRL;
                }
                else {
                    NeptusLog.pub().info("File extention not supported, or invalid file");
                }                    
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read *.obj files
     */
    private void readOBJFile() {
        readOBJ = new vtkOBJReader();
        try {
            readOBJ.SetFileName(absolutePath);
            readOBJ.Update();
            readOBJ.UpdateInformation();

            setActor(new vtkLODActor());

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(readOBJ.GetOutputPort());

            actor.SetMapper(mapper);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read *.ply files
     */
    private void readPLYFile() {
        readPLY = new vtkPLYReader();
        try {
            readPLY.SetFileName(absolutePath);
            readPLY.Update();
            readPLY.UpdateInformation();

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(readPLY.GetOutputPort());

            actor.SetMapper(mapper);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read *.stl files
     */
    private void readSTLFile() {
        readSTL = new vtkSTLReader();
        try {
            readSTL.SetFileName(absolutePath);
            readSTL.Update();
            readSTL.UpdateInformation();

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(readSTL.GetOutputPort());

            actor.SetMapper(mapper);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read *.vtk files
     */
    private void readVTKFile() {
        readVTK = new vtkDataSetReader();
        try {
            readVTK.SetFileName(absolutePath);
            readVTK.Update();
            readVTK.UpdateInformation();

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(readVTK.GetOutputPort());

            actor.SetMapper(mapper);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read simple *.xyz files
     */
    private void readXYZfile() {
        readXYZ = new vtkSimplePointsReader();
        try {
            readXYZ.SetFileName(absolutePath);
            readXYZ.Update();
            readXYZ.UpdateInformation();

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(readXYZ.GetOutputPort());

            actor.SetMapper(mapper);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the actor
     */
    public vtkLODActor getActor() {
        return actor;
    }

    /**
     * @param actor the actor to set
     */
    private void setActor(vtkLODActor actor) {
        this.actor = actor;
    }
}
