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
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.cdt3d;

import vtk.vtkDoubleArray;
import vtk.vtkLODActor;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkVertexGlyphFilter;

/**
 * @author hfq
 *
 */
public class PointCloudCTD {

    private String cloudName;
    private vtkPoints points;
    private vtkPolyData polyData;
    private vtkLODActor cloudLODActor;
    private int numberOfPoints;
    private double[] bounds;
    private int memorySize;

    private vtkDoubleArray temperatureArray;
    private vtkDoubleArray salinityArray;
    private vtkDoubleArray pressureArray;

    private vtkDoubleArray timestampArray;

    /**
     * @param cloudName
     */
    public PointCloudCTD(String cloudName) {
        this.cloudName = cloudName;
        this.points = new vtkPoints();
        this.polyData = new vtkPolyData();
        this.cloudLODActor = new vtkLODActor();

        this.temperatureArray = new vtkDoubleArray();
        this.salinityArray = new vtkDoubleArray();
        this.pressureArray = new vtkDoubleArray();
    }

    /**
     * 
     */
    public PointCloudCTD() {
        this("ctd");
    }

    /**
     * 
     */
    public void createPointCloudActor() {
        try {
            points.Squeeze();
            points.Modified();

            polyData.Allocate(numberOfPoints, numberOfPoints);
            polyData.SetPoints(points);

            vtkVertexGlyphFilter vertex = new vtkVertexGlyphFilter();
            vertex.AddInput(polyData);
            vertex.Update();

            polyData.ShallowCopy(vertex.GetOutput());
            polyData.Squeeze();
            polyData.Update();

            setBounds(polyData.GetBounds());

            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(polyData.GetProducerPort());

            cloudLODActor.SetMapper(mapper);
            cloudLODActor.GetProperty().SetPointSize(5.0);
            cloudLODActor.GetProperty().SetRepresentationToPoints();

            setMemorySize(mapper.GetInput().GetActualMemorySize());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        String info = this.getClass().getSimpleName() + "\n";
        info += "CloudName " + cloudName + "\n";
        info += getPoints().Print();
        info += getPolyData().Print();
        return info;
    }

    /**
     * @return the points
     */
    public vtkPoints getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(vtkPoints points) {
        this.points = points;
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
     * @return the cloudLODActor
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

    /**
     * @return the temperatureArray
     */
    public vtkDoubleArray getTemperatureArray() {
        return temperatureArray;
    }

    /**
     * @param temperatureArray the temperatureArray to set
     */
    public void setTemperatureArray(vtkDoubleArray temperatureArray) {
        this.temperatureArray = temperatureArray;
    }

    /**
     * @return the salinityArray
     */
    public vtkDoubleArray getSalinityArray() {
        return salinityArray;
    }

    /**
     * @param salinityArray the salinityArray to set
     */
    public void setSalinityArray(vtkDoubleArray salinityArray) {
        this.salinityArray = salinityArray;
    }

    /**
     * @return the pressureArray
     */
    public vtkDoubleArray getPressureArray() {
        return pressureArray;
    }

    /**
     * @param pressureArray the pressureArray to set
     */
    public void setPressureArray(vtkDoubleArray pressureArray) {
        this.pressureArray = pressureArray;
    }

    /**
     * @return the timestampArray
     */
    public vtkDoubleArray getTimestampArray() {
        return timestampArray;
    }

    /**
     * @param timestampArray the timestampArray to set
     */
    public void setTimestampArray(vtkDoubleArray timestampArray) {
        this.timestampArray = timestampArray;
    }


}
