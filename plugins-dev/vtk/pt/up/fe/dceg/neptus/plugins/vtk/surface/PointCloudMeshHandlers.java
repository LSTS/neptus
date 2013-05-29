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
 * May 27, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.surface;

import vtk.vtkLookupTable;
import vtk.vtkPolyData;
import vtk.vtkUnsignedCharArray;

/**
 * @author hfq
 * Handles pointCloud generated mesh colors
 *
 */
public class PointCloudMeshHandlers {
    private vtkUnsignedCharArray colorsX;
    private vtkUnsignedCharArray colorsY;
    private vtkUnsignedCharArray colorsZ;
    
    private vtkLookupTable lutX;
    private vtkLookupTable lutY;
    private vtkLookupTable lutZ;
    
    public PointCloudMeshHandlers() {
        setColorsX(new vtkUnsignedCharArray());
        setColorsY(new vtkUnsignedCharArray());
        setColorsZ(new vtkUnsignedCharArray());
        setLutX(new vtkLookupTable());
        setLutY(new vtkLookupTable());
        setLutZ(new vtkLookupTable());
    }
    
    public void generateMeshColorHandlers(vtkPolyData polyData, double[] bounds) {
        getLutX().SetRange(bounds[0], bounds[1]);
        getLutX().SetScaleToLinear();
        getLutX().Build();
        
        getLutY().SetRange(bounds[2], bounds[3]);
        getLutY().SetScaleToLinear();
        getLutY().Build();
        
        getLutZ().SetRange(bounds[4], bounds[5]);
        getLutZ().SetScaleToLinear();
        getLutZ().Build();
        
        getColorsX().SetNumberOfComponents(3);
        getColorsX().SetName("colorsX");
        
        getColorsY().SetNumberOfComponents(3);
        getColorsX().SetName("colorsY");
        
        getColorsZ().SetNumberOfComponents(3);
        getColorsX().SetName("colorsZ");
        
        for (int i = 0; i < polyData.GetNumberOfPoints(); ++i) {
            double[] point = new double[3];
            polyData.GetPoint(i, point);
            
            double[] xDColor = new double[3];
            double[] yDColor = new double[3];
            double[] zDColor = new double[3];
            
            getLutX().GetColor(point[0], xDColor);
            getLutY().GetColor(point[1], yDColor);
            getLutZ().GetColor(point[2], zDColor);
            
            char[] colorx = new char[3];
            char[] colory = new char[3];
            char[] colorz = new char[3];
            
            for (int j = 0; j <3; ++j) {
                colorx[j] = (char) (255.0 * xDColor[j]);
                colory[j] = (char) (255.0 * yDColor[j]);
                colorz[j] = (char) (255.0 * zDColor[j]);
            }
            
            getColorsX().InsertNextTuple3(colorx[0], colorx[1], colorx[2]);
            getColorsY().InsertNextTuple3(colory[0], colory[1], colory[2]);
            getColorsZ().InsertNextTuple3(colorz[0], colorz[1], colorz[2]);
        }
    }
    
    /**
     * @return the colorsX
     */
    public vtkUnsignedCharArray getColorsX() {
        return colorsX;
    }

    /**
     * @param colorsX the colorsX to set
     */
    public void setColorsX(vtkUnsignedCharArray colorsX) {
        this.colorsX = colorsX;
    }

    /**
     * @return the colorsY
     */
    public vtkUnsignedCharArray getColorsY() {
        return colorsY;
    }

    /**
     * @param colorsY the colorsY to set
     */
    public void setColorsY(vtkUnsignedCharArray colorsY) {
        this.colorsY = colorsY;
    }

    /**
     * @return the colorsZ
     */
    public vtkUnsignedCharArray getColorsZ() {
        return colorsZ;
    }

    /**
     * @param colorsZ the colorsZ to set
     */
    public void setColorsZ(vtkUnsignedCharArray colorsZ) {
        this.colorsZ = colorsZ;
    }

    /**
     * @return the lutX
     */
    public vtkLookupTable getLutX() {
        return lutX;
    }

    /**
     * @param lutX the lutX to set
     */
    public void setLutX(vtkLookupTable lutX) {
        this.lutX = lutX;
    }

    /**
     * @return the lutY
     */
    public vtkLookupTable getLutY() {
        return lutY;
    }

    /**
     * @param lutY the lutY to set
     */
    public void setLutY(vtkLookupTable lutY) {
        this.lutY = lutY;
    }

    /**
     * @return the lytZ
     */
    public vtkLookupTable getLutZ() {
        return lutZ;
    }

    /**
     * @param lytZ the lytZ to set
     */
    public void setLutZ(vtkLookupTable lutZ) {
        this.lutZ = lutZ;
    }

    
}
