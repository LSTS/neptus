/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 4, 2014
 */
package pt.lsts.neptus.vtk.pointcloud;

import vtk.vtkLookupTable;
import vtk.vtkPolyData;
import vtk.vtkUnsignedCharArray;

/**
 * @author hfq
 *
 */
public class PointCloudHandlerXYZI extends PointCloudHandlerXYZ implements IPointCloudHandler {
    private vtkUnsignedCharArray colorsI;
    private vtkLookupTable lutI;
    private final PointCloudXYZI pointCloud;
    private final vtkPolyData polyData;
    private final double[] bounds;

    /**
     * 
     */
    public PointCloudHandlerXYZI(PointCloudXYZI pointCloud) {
        super();
        setLutI(new vtkLookupTable());
        this.pointCloud = pointCloud;
        this.polyData = pointCloud.getPolyData();
        this.bounds = pointCloud.getBounds();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.IPointCloudHandler#generatePointCloudColors(boolean)
     */
    @Override
    public void generatePointCloudColors(boolean isColorInverted) {
        if(!isColorInverted) {
            getLutX().SetRange(bounds[0], bounds[1]);
            getLutX().SetScaleToLinear();
            getLutX().Build();
            getLutY().SetRange(bounds[2], bounds[3]);
            getLutY().SetScaleToLinear();
            getLutY().Build();
            getLutZ().SetRange(bounds[4], bounds[5]);
            getLutZ().SetScaleToLinear();
            getLutZ().Build();
            getLutI().SetRange(pointCloud.getIntensities().GetRange());
            getLutI().SetScaleToLinear();
            getLutI().Build();
        }
        else {
            invertLUTTableColors();
        }

        getColorsX().SetNumberOfComponents(3);
        getColorsY().SetNumberOfComponents(3);
        getColorsZ().SetNumberOfComponents(3);
        getColorsI().SetNumberOfComponents(3);
        getColorsX().SetName("colorsX");
        getColorsY().SetName("colorsY");
        getColorsZ().SetName("colorsZ");
        getColorsI().SetName("colorsI");


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

            getColorsX().InsertNextTuple3(colorx[0], colorx[1], colorx[2]);
            getColorsY().InsertNextTuple3(colory[0], colory[1], colory[2]);
            getColorsZ().InsertNextTuple3(colorz[0], colorz[1], colorz[2]);
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.IPointCloudHandler#invertLUTTableColors()
     */
    @Override
    public void invertLUTTableColors() {
        vtkLookupTable look1 = new vtkLookupTable();
        look1.SetRange(bounds[0], bounds[1]);
        look1.SetScaleToLinear();
        look1.Build();

        getLutX().SetRange(bounds[0], bounds[1]);
        for (int i = 1; i <= 256; ++i) {
            getLutX().SetTableValue(256 - i, look1.GetTableValue(i));
        }
        getLutX().Build();

        vtkLookupTable look2 = new vtkLookupTable();
        look2.SetRange(bounds[2], bounds[3]);
        look2.SetScaleToLinear();
        look2.Build();

        getLutY().SetRange(bounds[2], bounds[3]);
        for (int i = 1; i <= 256; ++i) {
            getLutY().SetTableValue(256 - i, look2.GetTableValue(i));
        }
        getLutY().Build();

        vtkLookupTable look3 = new vtkLookupTable();
        look3.SetRange(bounds[4], bounds[5]);
        for (int i = 1; i <= 256; ++i) {
            getLutZ().SetTableValue(256 - i, look3.GetTableValue(i));
        }
        getLutZ().Build();

        vtkLookupTable look4 = new vtkLookupTable();
        look4.SetRange(pointCloud.getIntensities().GetRange());
        for (int i = 1; i <= 256; ++i) {
            getLutI().SetTableValue(256-i, look4.GetTableValue(i));
        }
        getLutI().Build();
    }

    /**
     * @return the colorsI
     */
    public vtkUnsignedCharArray getColorsI() {
        return colorsI;
    }

    /**
     * @param colorsI the colorsI to set
     */
    protected void setColorsI(vtkUnsignedCharArray colorsI) {
        this.colorsI = colorsI;
    }

    /**
     * @return the lutI
     */
    public vtkLookupTable getLutI() {
        return lutI;
    }

    /**
     * @param lutI the lutI to set
     */
    protected void setLutI(vtkLookupTable lutI) {
        this.lutI = lutI;
    }

}
