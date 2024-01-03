/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 12/06/2015
 */
package pt.lsts.neptus.plugins.urready4os.vtk;

import java.util.Arrays;

import pt.lsts.neptus.vtk.pointcloud.IPointCloudHandler;
import vtk.vtkDoubleArray;
import vtk.vtkLookupTable;
import vtk.vtkUnsignedCharArray;

/**
 * @author pdias
 *
 */
public class PointCloudHandlerRhodamineDye implements IPointCloudHandler {

    private PointCloudRhodamine pointCloud;
    
    private final vtkDoubleArray rhodamineDyeArray;
    
    private double[] useRange = null;

    private vtkUnsignedCharArray colorsRhodamineDye;

    private vtkLookupTable lutRhodamineDye;

    /**
     * @param pointCloud
     */
    public PointCloudHandlerRhodamineDye(PointCloudRhodamine pointCloud) {
        this.pointCloud = pointCloud;
        this.rhodamineDyeArray = pointCloud.getRhodamineList();

        setColorsRhodamineDye(new vtkUnsignedCharArray());
        setLutRhodamineDye(new vtkLookupTable());
    }

    /**
     * @return the useRange
     */
    public double[] getUseRange() {
        if (useRange == null)
            useRange = rhodamineDyeArray.GetRange();
        return useRange;
    }

    public double[] updateUseRange() {
        useRange = rhodamineDyeArray.GetRange();
        return useRange;
    }

    /**
     * @param useRange the useRange to set
     */
    public void setUseRange(double[] useRange) {
        this.useRange = useRange;
    }
 
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.IPointCloudHandler#generatePointCloudColors(boolean)
     */
    @Override
    public void generatePointCloudColors(boolean isColorInverted) {
        if(!isColorInverted) {
            getLutRhodamineDye().SetRange(useRange == null ? rhodamineDyeArray.GetRange() : useRange);
            getLutRhodamineDye().SetScaleToLinear();
            getLutRhodamineDye().Build();
        }
        else {
            invertLUTTableColors();
        }

        double[] rr = rhodamineDyeArray.GetRange();
        System.out.println("Range for 3D: " + Arrays.toString(rr));

        getColorsRhodamineDye().SetNumberOfComponents(3);
        getColorsRhodamineDye().SetName("colorsRhodamineDye");

        for (int i = 0; i < pointCloud.getNumberOfPoints(); ++i) {
            double rhodamineDye = rhodamineDyeArray.GetValue(i);

            double[] rhodamineDyeColor = new double[3];

            lutRhodamineDye.GetColor(rhodamineDye, rhodamineDyeColor);

            char[] colorRhodamineDye = new char[3];

            for (int j = 0; j < 3; ++j) {
                colorRhodamineDye[j] = (char) (255.0 * rhodamineDyeColor[j]);
            }

            getColorsRhodamineDye().InsertNextTuple3(colorRhodamineDye[0], colorRhodamineDye[1], colorRhodamineDye[2]);
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointcloud.IPointCloudHandler#invertLUTTableColors()
     */
    @Override
    public void invertLUTTableColors() {
        vtkLookupTable look1 = new vtkLookupTable();
        look1.SetRange(useRange == null ? rhodamineDyeArray.GetRange() : useRange);
        look1.SetScaleToLinear();
        look1.Build();

        lutRhodamineDye.SetRange(useRange == null ? rhodamineDyeArray.GetRange() : useRange);
        for (int i = 1; i <= 256 ; ++i) {
            lutRhodamineDye.SetTableValue(256 - i, look1.GetTableValue(i));
        }
        lutRhodamineDye.Build();
    }
    
    /**
     * @return the lutTemperature
     */
    public vtkLookupTable getLutRhodamineDye() {
        return lutRhodamineDye;
    }

    public void setLutRhodamineDye(vtkLookupTable lutRhodamineDye) {
        this.lutRhodamineDye = lutRhodamineDye;
    }

    public vtkUnsignedCharArray getColorsRhodamineDye() {
        return colorsRhodamineDye;
    }

    private void setColorsRhodamineDye(vtkUnsignedCharArray colorsRhodamineDye) {
        this.colorsRhodamineDye = colorsRhodamineDye;
    }
}