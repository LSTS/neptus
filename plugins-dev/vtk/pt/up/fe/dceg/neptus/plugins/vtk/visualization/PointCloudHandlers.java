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
 * Apr 19, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkDataArray;
import vtk.vtkLookupTable;
import vtk.vtkPolyData;
import vtk.vtkScalarsToColors;
import vtk.vtkUnsignedCharArray;

/**
 * @author hfq
 * Handles Pointcloud colors
 */
public class PointCloudHandlers<T extends PointXYZ> {
    
    private int numberOfPoints;
    private vtkPolyData polyData;
    private vtkLookupTable colorLookupTable;
    
    private vtkUnsignedCharArray colorsX;
    private vtkUnsignedCharArray colorsY;
    private vtkUnsignedCharArray colorsZ;
    
    private vtkLookupTable lutX;
    private vtkLookupTable lutY;
    private vtkLookupTable lutZ;
    
    public PointCloudHandlers() {
        setColorsX(new vtkUnsignedCharArray());
        setColorsY(new vtkUnsignedCharArray());
        setColorsZ(new vtkUnsignedCharArray());
        setLutX(new vtkLookupTable());
        setLutY(new vtkLookupTable());
        setLutZ(new vtkLookupTable());
    }
    
    /**
     * 
     * @param numberPoints
     * @param polyData
     * @param colorLookupTable
     * @param bounds
     */
    public void setPointCloudColorHandlers(int numberPoints, vtkPolyData polyData, vtkLookupTable colorLookupTable, double[] bounds) {
        this.numberOfPoints = numberPoints;
        this.setPolyData(polyData);
        
        getLutX().SetRange(bounds[0], bounds[1]);
        getLutX().SetScaleToLinear();
        getLutX().Build();
        getLutY().SetRange(bounds[2], bounds[3]);
        getLutY().SetScaleToLinear();
        getLutY().Build();
        getLutZ().SetRange(bounds[4], bounds[5]);
        getLutZ().SetScaleToLinear();
        getLutZ().Build();
        
        colorsX.SetNumberOfComponents(3);
        colorsY.SetNumberOfComponents(3);
        colorsZ.SetNumberOfComponents(3);      
        colorsX.SetName("colorsX");
        colorsY.SetName("colorsY");
        colorsZ.SetName("colorsZ");
        
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
            
            for (int j = 0; j < 3; ++j) {
                colorx[j] = (char) (255.0 * xDColor[j]);
                colory[j] = (char) (255.0 * yDColor[j]);
                colorz[j] = (char) (255.0 * zDColor[j]);
            }
                       
            colorsX.InsertNextTuple3(colorx[0], colorx[1], colorx[2]);
            colorsY.InsertNextTuple3(colory[0], colory[1], colory[2]);
            colorsZ.InsertNextTuple3(colorz[0], colorz[1], colorz[2]);
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
     * @return the colorLookupTable
     */
    public vtkLookupTable getColorLookupTable() {
        return colorLookupTable;
    }

    /**
     * @param colorLookupTable the colorLookupTable to set
     */
    public void setColorLookupTable(vtkLookupTable colorLookupTable) {
        this.colorLookupTable = colorLookupTable;
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
     * @return the lutZ
     */
    public vtkLookupTable getLutZ() {
        return lutZ;
    }

    /**
     * @param lutZ the lutZ to set
     */
    public void setLutZ(vtkLookupTable lutZ) {
        this.lutZ = lutZ;
    }

    public static double[] getRandomColor(PointCloud<PointXYZ> cloud) {
        cloud.getNumberOfPoints();
        
        for (int i = 0; i < cloud.getNumberOfPoints(); i++) {
            double[] point = new double[3];
            point = cloud.getPoints().GetPoint(i);
        }
        
        double[] rgbColor = new double[3];
        
        return rgbColor;
    }
    
    public static double[] getRandomColor() {
        double[] rgbCloud = new double[3];
        double sum;
        int step = 100;
        
        do {
            sum = 0;
            //rgbCloud[0] = (Math.random() % step) / (double)step;
            rgbCloud[0] = Math.random();
            //rgbCloud[1] = (Math.random() % step) / (double)step;
            rgbCloud[1] = Math.random();
            //rgbCloud[2] = (Math.random() % step) / (double)step;
            rgbCloud[2] = Math.random();
            sum = rgbCloud[0] + rgbCloud[1] + rgbCloud[2];
            //System.out.println("r = " + rgbCloud[0] + ", g = " + rgbCloud[1] + ", b = " + rgbCloud[2]);
        }while (sum <= 0.5 || sum >= 2.8);

        //rgbCloud[0] = min + Math.random() * ((min - max) + min); // [5,10];
        
        return rgbCloud;
    }
    
    public vtkScalarsToColors getRandomColor2() {
        vtkScalarsToColors scalars = new vtkScalarsToColors();
        return scalars;
    }
    
    public vtkDataArray getColor() {
        vtkDataArray scalars = new vtkUnsignedCharArray();
        vtkUnsignedCharArray scalars2 = new vtkUnsignedCharArray();
        vtkDataArray scalars3 = new vtkDataArray();
        
        
        scalars.SetNumberOfComponents(3);
        scalars.SetNumberOfTuples(numberOfPoints);
        
        double[] rgbColor = new double[3];
        rgbColor = getRandomColor();
        
        return scalars;
    }
}
