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
 * May 7, 2013
 */
package pt.lsts.neptus.vtk.filters;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.pointcloud.PointCloudXYZ;
import vtk.vtkCleanPolyData;

/**
 * @author hfq FIXME
 * this method isn't the best one to downsample... it simply cleans data within a tolerance
 */
public class DownsamplePointCloud {
    protected int numberOfPoints = 0;
    public APointCloud<?> pointCloud;
    private PointCloudXYZ outputDownsampledCloud;
    private final double tolerance;
    public Boolean isDownsampleDone = false;

    public DownsamplePointCloud(APointCloud<?> pointCloud, double tolerance) {
        this.pointCloud = pointCloud;
        this.numberOfPoints = pointCloud.getNumberOfPoints();
        this.tolerance = tolerance;
        setOutputDownsampledCloud(new PointCloudXYZ());
        downsample();
    }

    private void downsample() {
        try {
            vtkCleanPolyData cleanPolyData = new vtkCleanPolyData();
            cleanPolyData.SetInputConnection(pointCloud.getPolyData().GetProducerPort());
            cleanPolyData.SetTolerance(tolerance);
            cleanPolyData.Update();

            outputDownsampledCloud.setCloudName("downsampledCloud");
            outputDownsampledCloud.setXYZPoints(cleanPolyData.GetOutput().GetPoints());
            outputDownsampledCloud.setNumberOfPoints(outputDownsampledCloud.getXYZPoints().GetNumberOfPoints());

            isDownsampleDone = true;
        }
        catch (Exception e) {
            NeptusLog.pub().info("Exception on cloud downsampling");
            e.printStackTrace();
        }
    }

    /**
     * @return the outputDownsampledCloud
     */
    public PointCloudXYZ getOutputDownsampledCloud() {
        return outputDownsampledCloud;
    }

    /**
     * @param outputDownsampledCloud the outputDownsampledCloud to set
     */
    private void setOutputDownsampledCloud(PointCloudXYZ outputDownsampledCloud) {
        this.outputDownsampledCloud = outputDownsampledCloud;
    }
}
