/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * May 28, 2014
 */
package pt.lsts.neptus.vtk.pointcloud;

import pt.lsts.neptus.vtk.pointtypes.APoint;
import vtk.vtkLODActor;
import vtk.vtkPoints;
import vtk.vtkPolyData;

/**
 * @author hfq
 */
public abstract class APointCloud<T extends APoint> {

    private String cloudName;
    private vtkPoints xyzPoints;
    private vtkPolyData polyData;
    private vtkLODActor cloudLODActor;
    private int numberOfPoints;
    private int memorySize;
    private double[] bounds;
    private IPointCloudHandler colorHandler;

    /**
     * 
     */
    public APointCloud(String name) {
        setXYZPoints(new vtkPoints());
        setPolyData(new vtkPolyData());
        setCloudLODActor(new vtkLODActor());
        setBounds(new double[6]);
    }

    public APointCloud() {
        this("pointcloud");
    }

    /**
     * Get point from index
     * @param index
     * @return
     */
    public abstract T getPointAtIndex(int index);

    /**
     * Adds a point to the cloud
     * @param p
     */
    public abstract void addPoint(T p);

    /**
     * Adds a point to a specific cloud index
     * @param p
     * @param index
     */
    public abstract void addPointAtIndex(T p, int index);

    /**
     * Create actor for a Pointcloud
     */
    public abstract void createActorFromPoints();

    /**
     * Generate the necessary Pointcloud handlers
     */
    public abstract void generateHandler();

    @Override
    public String toString() {
        String info = this.getClass().getSimpleName() + "\n";
        info += getXYZPoints().Print();
        info += getXYZPoints().Print();
        return info;
    }

    /**
     * @return the cloudName
     */
    public String getCloudName() {
        return cloudName;
    }

    /**
     * @param cloudName the cloudName to set
     */
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    /**
     * @return the points
     */
    public vtkPoints getXYZPoints() {
        return xyzPoints;
    }

    /**
     * @param points the points to set
     */
    public void setXYZPoints(vtkPoints xyzPoints) {
        this.xyzPoints = xyzPoints;
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
     * @return the cloudLODActorbounds
     */
    public vtkLODActor getCloudLODActor() {
        return cloudLODActor;
    }

    /**
     * @param cloudLODActor the cloudLODActor to set
     */
    public void setCloudLODActor(vtkLODActor cloudLODActor) {
        this.cloudLODActor = cloudLODActor;
    }

    /**
     * @return the numberOfPoints
     */
    public int getNumberOfPoints() {
        return numberOfPoints;
    }

    /**
     * @param numberOfPoints the numberOfPoints to set
     */
    public void setNumberOfPoints(int numberOfPoints) {
        this.numberOfPoints = numberOfPoints;
    }

    /**
     * @return the bounds
     */
    public double[] getBounds() {
        return bounds;
    }

    /**
     * @param bounds the bounds to set
     */
    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }

    /**
     * @return the colorHandler
     */
    public IPointCloudHandler getColorHandler() {
        return colorHandler;
    }

    /**
     * @param colorHandler the colorHandler to set
     */
    public void setColorHandler(IPointCloudHandler colorHandler) {
        this.colorHandler = colorHandler;
    }

    /**
     * @return the memorySize
     */
    public int getMemorySize() {
        return memorySize;
    }

    /**
     * @param memorySize the memorySize to set
     */
    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

}
